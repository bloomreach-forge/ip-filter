package org.onehippo.forge.ipfilter;


import com.drew.lang.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

import static org.onehippo.forge.ipfilter.IpFilterConstants.*;

public final class IpFilterUtils {
    private static final Logger log = LoggerFactory.getLogger(IpFilterUtils.class);
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    public static final ObjectMapper JSON = new ObjectMapper();

    static {
        JSON.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSON.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private IpFilterUtils() {
    }

    public static String getParameter(final FilterConfig filterConfig, final String paramName, final String defaultValue) {
        String value = filterConfig.getInitParameter(paramName);
        if (value == null) {
            value = filterConfig.getServletContext().getInitParameter(paramName);
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }


    public static <T> String toJson(final T object) {
        if (object == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON error", e);
        }
        return null;
    }


    @Nullable
    public static Map<String, AuthObject> fromJsonAsMap(final String message) {

        try {
            return JSON.readValue(message, new TypeReference<Map<String, AuthObject>>() {
            });
        } catch (Exception e) {

            log.error("JSON error (see message below)", e);
            log.error("{}", message);
        }
        return null;
    }

    @Nullable
    public static <T> T fromJson(final String message, Class<T> clazz) {

        try {
            return JSON.readValue(message, clazz);
        } catch (Exception e) {
            log.error("JSON error (see message below)", e);
            log.error("{}", message);
        }
        return null;
    }


    public static String getIp(HttpServletRequest request, final String name) {
        final String headerName = Strings.isNullOrEmpty(name) ? IpFilterConstants.HEADER_X_FORWARDED_FOR : name;
        final String header = request.getHeader(headerName);
        if (Strings.isNullOrEmpty(header)) {
            log.debug("Header: {} was empty", headerName);
            return request.getRemoteAddr();
        }
        final Iterable<String> ipAddresses = COMMA_SPLITTER.split(header);
        final Iterator<String> iterator = ipAddresses.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return request.getRemoteAddr();
    }

    public static String getHost(final HttpServletRequest request) {
        final String hostHeader = request.getHeader(HEADER_X_FORWARDED_HOST);
        if (Strings.isNullOrEmpty(hostHeader)) {
            final String remoteHost = request.getRemoteHost();
            log.debug("missing header {}, using: {}", HEADER_X_FORWARDED_HOST, remoteHost);
            return remoteHost;
        }
        return hostHeader;

    }
}
