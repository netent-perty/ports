package com.netent.news.logging.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContext;

@Configuration
public class FilterConfig {
    public static final String GAMEPROVIDERAPI = "/gameproviderapi";

    private static final Logger LOG = LoggerFactory.getLogger(FilterConfig.class);

    @Autowired
    public void logServer(ServletContext servletContext) {
        LOG.info("HTTP server: " + servletContext.getServerInfo());
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration() {
        var registration = new FilterRegistrationBean<LoggingFilter>();
        registration.setFilter(new LoggingFilter());
        registration.addUrlPatterns(GAMEPROVIDERAPI + "/*");
        return registration;
    }
}
