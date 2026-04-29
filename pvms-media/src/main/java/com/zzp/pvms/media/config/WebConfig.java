package com.zzp.pvms.media.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /thumbnails/** 映射到本地目录 D:/pvms/thumbnails/
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:D:/pvms/thumbnails/");
    }
}
