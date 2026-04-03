package com.example.DoAn.service;

public interface EmailService {
    void sendAccountCreatedEmail(String toEmail, String fullName, String roleName, String password);
    void sendRoleUpdatedEmail(String toEmail, String fullName, String oldRoleName, String newRoleName);
    void sendAccountStatusEmail(String toEmail, String fullName, String newStatus);
}
