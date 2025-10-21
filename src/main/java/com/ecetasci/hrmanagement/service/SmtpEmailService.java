package com.ecetasci.hrmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void send(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String link = "http://localhost:8080/api/auth/verify?token=" + token;
        send(to, "Email Verification", "Lütfen emailinizi doğrulamak için tıklayın: " + link);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String link = "http://localhost:8080/api/auth/reset-password?token=" + token;
        send(to, "Password Reset", "Şifrenizi yenilemek için: " + link);
    }

    @Override
    public void sendAccountApprovalEmail(String to) {
        send(to, "Account Approved", "Tebrikler! Hesabınız onaylandı");
    }

    @Override
    public void sendAccountRejectionEmail(String to) {
        send(to, "Account Rejected", "Üzgünüz, hesabınız reddedildi");
    }

    @Override
    public void sendEmployeeActivationEmail(String to) {
        send(to, "Employee Activated", "Hesabınız aktif hale getirildi");
    }

    @Override
    public void sendLeaveRequestStatusEmail(String to, String status, String managerNote) {
        send(to, "Leave Request " + status,
                "İzin talebiniz " + status + " olarak güncellendi.\nYönetici notu: " + managerNote);
    }
}

