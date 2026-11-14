package com.reservaya.notification_service.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.reservaya.notification_service.service.EmailService;
import com.reservaya.notification_service.service.TemplateStorageService;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateStorageService templateStorageService;
    private final SpringTemplateEngine stringTemplateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateStorageService templateStorageService) {
        this.mailSender = mailSender;
        this.templateStorageService = templateStorageService;
        this.stringTemplateEngine = new SpringTemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);
        this.stringTemplateEngine.setTemplateResolver(resolver);
    }

    @Override
    public void send(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            String rawHtml = templateStorageService.fetchTemplate(templateName);

            Context ctx = new Context();
            ctx.setVariables(variables);
            String renderedHtml = stringTemplateEngine.process(rawHtml, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(renderedHtml, true);

            mailSender.send(message);
            log.info("Email '{}' enviado a {}", templateName, to);

        } catch (Exception e) {
            log.error("Error al enviar email '{}' a {}: {}", templateName, to, e.getMessage(), e);
            throw new RuntimeException("Error al enviar email: " + templateName, e);
        }
    }
}
