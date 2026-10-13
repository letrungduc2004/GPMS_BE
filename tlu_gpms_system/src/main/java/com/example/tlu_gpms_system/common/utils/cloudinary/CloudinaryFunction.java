package com.example.tlu_gpms_system.common.utils.cloudinary;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryFunction {
   String uploadImage(MultipartFile multipartFile);
   boolean deleteImage(String publicId);
}
