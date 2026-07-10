package com.sms.passwordReset;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    public void sendOtp(String phoneNumber, String otp) {

        Message.creator(
                new PhoneNumber("whatsapp:" + phoneNumber),
                new PhoneNumber(fromNumber),
                "Your password reset OTP is: " + otp + ". This OTP is valid for 5 minutes."
        ).create();
    }
}