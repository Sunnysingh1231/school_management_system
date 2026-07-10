package com.sms.passwordReset;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Password Reset OTP - School Management System");

        message.setText(
                "Dear User,\n\n" +
                "We received a request to reset the password for your School Management System account.\n\n" +
                "Your One-Time Password (OTP) is: " + otp + "\n\n" +
                "This OTP is valid for 5 minutes and can be used only once.\n\n" +
                "If you did not request a password reset, please ignore this email. Your account will remain secure.\n\n" +
                "For security reasons, please do not share this OTP with anyone.\n\n" +
                "Regards,\n" +
                "School Management System Team"
        );

        mailSender.send(message);
    }
}