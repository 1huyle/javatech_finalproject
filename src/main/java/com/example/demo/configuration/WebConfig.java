package com.example.demo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir.base:file:uploads/}")
    private String uploadDirBaseUri;

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    private static final String AVATARS_PATH_PATTERN = "/uploads/avatars/**";
    private static final String AVATARS_RESOURCE_LOCATION_SUFFIX = "avatars/";

    private static final String PROPERTIES_PATH_PATTERN = "/uploads/real-estate/**";
    private static final String PROPERTIES_RESOURCE_LOCATION_SUFFIX = "real-estate/";

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Đảm bảo uploadDirBaseUri kết thúc bằng dấu "/"
        String baseLocation = uploadDirBaseUri;
        if (!baseLocation.endsWith("/")) {
            baseLocation += "/";
        }

        // Phục vụ ảnh avatars
        registry.addResourceHandler(AVATARS_PATH_PATTERN)
                .addResourceLocations(baseLocation + AVATARS_RESOURCE_LOCATION_SUFFIX);

        // Phục vụ ảnh bất động sản
        registry.addResourceHandler(PROPERTIES_PATH_PATTERN)
                .addResourceLocations(baseLocation + PROPERTIES_RESOURCE_LOCATION_SUFFIX);
        
        // Cấu hình cho các tài nguyên tĩnh khác
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
        
        // Cấu hình thêm từ WebConfig cũ
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
} 