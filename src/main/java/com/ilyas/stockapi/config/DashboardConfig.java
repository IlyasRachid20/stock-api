package com.ilyas.stockapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Serves the React dashboard (built into classpath:/static/ by the Dockerfile) from the same address
 * as the API. React handles its own pages (/sales/new, /products...), so any unknown path that isn't
 * the API or a file gets index.html; a browser refresh on /sales/new then works.
 * In development the dashboard runs on Vite instead, and static/ is empty: nothing is served here.
 */
@Configuration
public class DashboardConfig implements WebMvcConfigurer {

    private static final List<String> NOT_DASHBOARD = List.of("api/", "actuator", "v3/", "swagger-ui", "error");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Built files have a content hash in their name (index-BYvVFO5W.js): safe to cache for a year
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource file = location.createRelative(path);
                        if (file.exists() && file.isReadable()) {
                            return file;
                        }
                        // A page of the dashboard, not the API or a missing file (e.g. /missing.js)
                        boolean isPage = NOT_DASHBOARD.stream().noneMatch(path::startsWith) && !path.contains(".");
                        Resource index = new ClassPathResource("/static/index.html");
                        return isPage && index.exists() ? index : null;
                    }
                });
    }
}
