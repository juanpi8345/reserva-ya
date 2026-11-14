package com.reservaya.notification_service.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.reservaya.notification_service.service.TemplateStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateStorageServiceImpl implements TemplateStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    @Cacheable(value = "emailTemplates", key = "#templateName")
    public String fetchTemplate(String templateName) {
        log.info("Descargando template '{}' desde S3 bucket '{}'", templateName, bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(templateName + ".html")
                .build();
        return s3Client.getObjectAsBytes(request).asUtf8String();
    }
}
