package com.example.tlu_gpms_system.common.utils.mail.impl;
import com.example.tlu_gpms_system.common.utils.mail.MailFuncation;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailService implements MailFuncation {

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender javaMailSender;

    @Override
    public SimpleMailMessage sendMessage(String title, String content, String sendToMail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(sendToMail);
            message.setSubject(title);
            message.setText(content);
            javaMailSender.send(message);
            return message;
        } catch (AppException e) {
            log.error("Mail Service không hoạt động");
            throw new AppException(ErrorCode.MAIL_SERVICE_FAIL);
        }
    }

}
