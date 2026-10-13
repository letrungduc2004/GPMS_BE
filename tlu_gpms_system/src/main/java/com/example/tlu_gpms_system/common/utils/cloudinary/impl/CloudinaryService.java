package com.example.tlu_gpms_system.common.utils.cloudinary.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.tlu_gpms_system.common.utils.cloudinary.CloudinaryFunction;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService implements CloudinaryFunction {
    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("File name không được để trống");
        }
        // Lấy tên file
        String publicValue = generatePublicValue(file.getOriginalFilename());
        try {
            Map uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), ObjectUtils.asMap("public_id", publicValue));
            return uploadResult.get("secure_url").toString();
        } catch(IOException e) {
            deleteImage(publicValue);
            throw new AppException(ErrorCode.CLOUDINARY_SERVICE_FAIL);
        }
    }

    @Override
    public boolean deleteImage(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            // Cloudinary trả về {"result":"ok"} nếu xóa thành công
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            throw new AppException(ErrorCode.CLOUDINARY_SERVICE_FAIL);
        }
    }

    public String generatePublicValue(String originalName) {
        String fileName = getFileName(originalName)[0];
        return StringUtils.join(UUID.randomUUID().toString(), "_", fileName);
    }

    public String[] getFileName(String originalName) {
        return originalName.split("\\.");
    }
}
