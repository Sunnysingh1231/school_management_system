package com.sms.passwordReset;

import lombok.RequiredArgsConstructor;

import java.io.Console;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public String sendOtp(@RequestParam String emailOrPhone) {
    	System.out.println(emailOrPhone);
    	
//    	return "all fine";
    	
        return forgotPasswordService.sendOtp(emailOrPhone);
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public String verifyOtp(@RequestParam String emailOrPhone,
                            @RequestParam String otp) {
        return forgotPasswordService.verifyOtp(emailOrPhone, otp);
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public String resetPassword(@RequestParam String emailOrPhone,
                                @RequestParam String newPassword) {
        return forgotPasswordService.resetPassword(emailOrPhone, newPassword);
    }
}
