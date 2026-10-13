package com.example.tlu_gpms_system.common.utils.mail;

import org.springframework.mail.SimpleMailMessage;

public interface MailFuncation {
    SimpleMailMessage sendMessage(String title, String content, String sendToMail);
}
