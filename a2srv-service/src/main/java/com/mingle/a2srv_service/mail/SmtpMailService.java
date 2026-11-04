package com.mingle.a2srv_service.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@mingle.local}")
    private String from;

    @Override
    public void sendVerification(String toEmail, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Confirm your Mingle email");
        message.setText("Welcome to Mingle!\n\nConfirm your email address to start matching:\n"
                + verificationLink + "\n\nThe link is valid for 24 hours. If you did not sign up, ignore this email.");
        mailSender.send(message);
    }
}
