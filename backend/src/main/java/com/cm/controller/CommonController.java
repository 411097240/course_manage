package com.cm.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
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
            
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 使用绝对路径，确保目录创建可靠
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            Path destPath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
            
            result.put("code", 200);
            result.put("msg", "success");
            // The file is served by WebMvcConfig at /uploads/filename
            result.put("data", "/uploads/" + newFilename);
        } catch (IOException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "Upload failed: " + e.getMessage());
        }
        return result;
    }
}
