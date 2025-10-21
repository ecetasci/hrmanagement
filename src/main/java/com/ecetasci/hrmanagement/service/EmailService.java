package com.ecetasci.hrmanagement.service;

public interface EmailService {
    void send(String to, String subject, String message);

    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendAccountApprovalEmail(String to);
    void sendAccountRejectionEmail(String to);
    void sendEmployeeActivationEmail(String to);
    void sendLeaveRequestStatusEmail(String to, String status, String managerNote);
}
