package com.netent.news.config.logging.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.netent.news.config.logging.LoggingConstants.REQUEST_BODY;
import static com.netent.news.config.logging.LoggingConstants.REQUEST_METHOD;
import static com.netent.news.config.logging.LoggingConstants.REQUEST_QUERY;
import static com.netent.news.config.logging.LoggingConstants.REQUEST_URI;
import static com.netent.news.config.logging.LoggingConstants.RESPONSE_BODY;
import static com.netent.news.config.logging.LoggingConstants.RESPONSE_STATUS;
import static net.logstash.logback.argument.StructuredArguments.kv;

@SuppressWarnings("PlaceholderCountMatchesArgumentCount")
public final class LoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var requestWrapper = new ContentCachingRequestWrapper(request);
        var responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(requestWrapper, responseWrapper);
        logRequest(requestWrapper);
        logResponse(responseWrapper);
        responseWrapper.copyBodyToResponse();
    }

    private void logRequest(ContentCachingRequestWrapper wrapper) throws UnsupportedEncodingException {
        var body = new String(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
        LOG.info("Incoming request",
                kv(REQUEST_METHOD, wrapper.getMethod()),
                kv(REQUEST_URI, wrapper.getRequestURI()),
                kv(REQUEST_QUERY, wrapper.getQueryString()),
                kv(REQUEST_BODY, body)
        );
    }

    private void logResponse(ContentCachingResponseWrapper wrapper) throws UnsupportedEncodingException {
        var body = new String(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
        LOG.info("Outgoing response",
                kv(RESPONSE_STATUS, wrapper.getStatus()),
                kv(RESPONSE_BODY, body)
        );
    }
}
