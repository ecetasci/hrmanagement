package com.ecetasci.hrmanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailService smtpEmailService;

    @BeforeEach
    void setup() {
        // no-op; @InjectMocks will construct with mocked mailSender
    }

    @Test
    void send_buildsAndSendsSimpleMailMessage() {
        smtpEmailService.send("to@example.com", "Hello", "World");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"to@example.com"}, msg.getTo());
        assertEquals("Hello", msg.getSubject());
        assertEquals("World", msg.getText());
    }

    @Test
    void sendVerificationEmail_containsVerificationLinkAndSubject() {
        String token = "abc123";
        smtpEmailService.sendVerificationEmail("user@example.com", token);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"user@example.com"}, msg.getTo());
        assertEquals("Email Verification", msg.getSubject());
        assertTrue(msg.getText().contains("http://localhost:8080/api/auth/verify?token=" + token));
    }

    @Test
    void sendPasswordResetEmail_containsResetLinkAndSubject() {
        String token = "reset-token";
        smtpEmailService.sendPasswordResetEmail("user2@example.com", token);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"user2@example.com"}, msg.getTo());
        assertEquals("Password Reset", msg.getSubject());
        assertTrue(msg.getText().contains("http://localhost:8080/api/auth/reset-password?token=" + token));
    }

    @Test
    void sendAccountApprovalEmail_usesExpectedTemplate() {
        smtpEmailService.sendAccountApprovalEmail("approved@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"approved@example.com"}, msg.getTo());
        assertEquals("Account Approved", msg.getSubject());
        assertEquals("Tebrikler! Hesabınız onaylandı", msg.getText());
    }

    @Test
    void sendAccountRejectionEmail_usesExpectedTemplate() {
        smtpEmailService.sendAccountRejectionEmail("rejected@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"rejected@example.com"}, msg.getTo());
        assertEquals("Account Rejected", msg.getSubject());
        assertEquals("Üzgünüz, hesabınız reddedildi", msg.getText());
    }

    @Test
    void sendEmployeeActivationEmail_usesExpectedTemplate() {
        smtpEmailService.sendEmployeeActivationEmail("active@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"active@example.com"}, msg.getTo());
        assertEquals("Employee Activated", msg.getSubject());
        assertEquals("Hesabınız aktif hale getirildi", msg.getText());
    }

    @Test
    void sendLeaveRequestStatusEmail_formatsSubjectAndBody() {
        smtpEmailService.sendLeaveRequestStatusEmail("emp@example.com", "APPROVED", "İyi tatiller");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertArrayEquals(new String[]{"emp@example.com"}, msg.getTo());
        assertEquals("Leave Request APPROVED", msg.getSubject());
        assertTrue(msg.getText().contains("İzin talebiniz APPROVED olarak güncellendi."));
        assertTrue(msg.getText().contains("Yönetici notu: İyi tatiller"));
    }
}

