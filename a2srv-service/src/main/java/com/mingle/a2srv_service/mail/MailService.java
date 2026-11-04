package com.mingle.a2srv_service.mail;

/** Outgoing mail. Kept behind an interface so the transport (SMTP today) can change without touching callers. */
public interface MailService {

    void sendVerification(String toEmail, String verificationLink);
}
