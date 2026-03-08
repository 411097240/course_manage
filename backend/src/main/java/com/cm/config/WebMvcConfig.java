package com.cm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPathConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用配置文件中的路径，并确保它是绝对路径
        File file = new File(uploadPathConfig);
        String absolutePath = file.getAbsolutePath();
        
        // 确保以斜杠结尾
        String uploadPath = absolutePath.endsWith(File.separator) ? absolutePath : absolutePath + File.separator;
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
