package com.cm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Value("${file.upload-path}")
    private String uploadPathConfig;

    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "File is empty");
            return result;
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 存储时使用 UUID 确保绝对不冲突
            String storeName = UUID.randomUUID().toString() + extension;
            
            Path uploadDir = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            Path destPath = uploadDir.resolve(storeName);
            Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
            
            result.put("code", 200);
            result.put("msg", "success");
            // 返回包含原始文件名的下载接口地址
            String downloadUrl = "/api/common/download/" + storeName + "?name=" + 
                                 URLEncoder.encode(originalFilename != null ? originalFilename : "file", "UTF-8");
            result.put("data", downloadUrl);
            
        } catch (IOException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "Upload failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * 专门的下载/预览接口，强制浏览器识别原始文件名
     */
    @GetMapping("/download/{storeName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String storeName, @RequestParam String name) {
        try {
            Path filePath = Paths.get(uploadPathConfig).toAbsolutePath().normalize().resolve(storeName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // 核心：通过 Header 告诉浏览器文件的“原名”
                String encodedName = URLEncoder.encode(name, "UTF-8").replaceAll("\\+", "%20");
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"; filename*=UTF-8''" + encodedName)
                        .body(resource);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.notFound().build();
    }
}
