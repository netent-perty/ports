package com.netent.news.logging.client;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.netent.news.logging.LoggingConstants.HEADERS;
import static com.netent.news.logging.LoggingConstants.REQUEST_BODY;
import static com.netent.news.logging.LoggingConstants.REQUEST_METHOD;
import static com.netent.news.logging.LoggingConstants.REQUEST_URI;
import static com.netent.news.logging.LoggingConstants.RESPONSE_BODY;
import static com.netent.news.logging.LoggingConstants.RESPONSE_STATUS;
import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Creating a WebClient with this class will guarantee you the following properties: -
 * Instrumentation for OpenTracing with Zipkin - Timeouts configured with http.read.timeout.ms and http.connection.timeout.ms - and standardized
 * ObjectMapper
 *
 * https://github.com/spring-cloud/spring-cloud-sleuth/blob/master/spring-cloud-sleuth-core/src/main/java/org/springframework/cloud/sleuth/instrument/web/client/TraceWebClientBeanPostProcessor.java
 */
@SuppressWarnings("PlaceholderCountMatchesArgumentCount")
public class WebClientFactory {

    private static final Logger LOGGER = getLogger(WebClientFactory.class);

    private final WebClient.Builder builder;

    public WebClientFactory(final WebClient.Builder builder) {
        this.builder = requireNonNull(builder);
    }

    public WebClient fromBaseUrl(final String baseUrl) {
        // See https://andrew-flower.com/blog/webclient-body-logging
        var loggingEncoder = new LoggingEncoder(bytes -> LOGGER.info("Client request body", kv(REQUEST_BODY, new String(bytes))));
        var loggingDecoder = new LoggingDecoder(bytes -> LOGGER.info("Client response body", kv(RESPONSE_BODY, new String(bytes))));

        return builder.clone()
                .filter(logRequest())
                .filter(logResponseStatus())
                .codecs(codecConfigurer -> {
                    codecConfigurer.defaultCodecs().jackson2JsonEncoder(loggingEncoder);
                    codecConfigurer.defaultCodecs().jackson2JsonDecoder(loggingDecoder);
                })
                .baseUrl(baseUrl)
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            LOGGER.info("Client request",
                    kv(REQUEST_METHOD, clientRequest.method()),
                    kv(HEADERS, getHeaders(clientRequest.headers())),
                    kv(REQUEST_URI, clientRequest.url())
            );
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponseStatus() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
             LOGGER.info("Client response",
                    kv(RESPONSE_STATUS, clientResponse.rawStatusCode()),
                    kv(HEADERS, clientResponse.headers().asHttpHeaders().toString()));
            return Mono.just(clientResponse);
        });
    }

    protected static String getHeaders(final HttpHeaders headers) {
        if (headers == null || headers.size() == 0) {
            return "";
        }

        return headers.entrySet().stream()
                .map(WebClientFactory::getHeaderString)
                .collect(Collectors.joining(", "));
    }

    private static String getHeaderString(Map.Entry<String, List<String>> entry) {
        String key = entry.getKey();
        List<String> value = entry.getValue();
        //mask authorization header
        if ("Authorization".equalsIgnoreCase(key)) {
            value = Collections.singletonList("***");
        }
        return key + ": '" + value + "'";
    }

    static class LoggingEncoder extends Jackson2JsonEncoder {
        private final Consumer<byte[]> consumer;

        public LoggingEncoder(final Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }

        @Override
        public DataBuffer encodeValue(final Object value,
                                      final DataBufferFactory bufferFactory,
                                      final ResolvableType valueType,
                                      @Nullable final MimeType mimeType,
                                      @Nullable final Map<String, Object> hints) {

            // Encode/Serialize data to JSON
            final DataBuffer data = super.encodeValue(value, bufferFactory, valueType, mimeType, hints);

            // Interception: Generate Signature and inject header into request
            consumer.accept(extractBytesAndReset(data));

            return data;
        }

    }

    static class LoggingDecoder extends Jackson2JsonDecoder {
        private final Consumer<byte[]> consumer;

        public LoggingDecoder(final Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }

        @Override
        public Mono<Object> decodeToMono(final Publisher<DataBuffer> input,
                                         final ResolvableType elementType,
                                         @Nullable final MimeType mimeType,
                                         @Nullable final Map<String, Object> hints) {
            // Buffer for bytes from each published DataBuffer
            final ByteArrayOutputStream payload = new ByteArrayOutputStream();

            // Augment the Flux, and intercept each group of bytes buffered
            final Flux<DataBuffer> interceptor = Flux.from(input)
                    .doOnNext(buffer -> bufferBytes(payload, buffer))
                    .doOnComplete(() -> consumer.accept(payload.toByteArray()));

            // Return the original method, giving our augmented Publisher
            return super.decodeToMono(interceptor, elementType, mimeType, hints);
        }

        private void bufferBytes(final ByteArrayOutputStream bao, final DataBuffer buffer) {
            try {
                bao.write(extractBytesAndReset(buffer));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static byte[] extractBytesAndReset(final DataBuffer data) {
        final byte[] bytes = new byte[data.readableByteCount()];
        data.read(bytes);
        data.readPosition(0);
        return bytes;
    }
}
