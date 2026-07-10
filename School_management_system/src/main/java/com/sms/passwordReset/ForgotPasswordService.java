package com.sms.passwordReset;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final PasswordEncoder passwordEncoder;

    public String sendOtp(String emailOrPhone) {

        boolean isEmail = emailOrPhone.contains("@");
        boolean accountExists = false;

        if (isEmail) {
            if (userRepository.findByEmail(emailOrPhone) != null) {
                accountExists = true;
            } else if (teacherRepository.findByEmail(emailOrPhone) != null) {
                accountExists = true;
            } else if (studentRepository.findByEmail(emailOrPhone) != null) {
                accountExists = true;
            }
        } else {
            if (userRepository.findByPhone(emailOrPhone).isPresent()) {
                accountExists = true;
            } else if (teacherRepository.findByPhone(emailOrPhone).isPresent()) {
                accountExists = true;
            } else if (studentRepository.findByPhone(emailOrPhone).isPresent()) {
                accountExists = true;
            }
        }

        if (!accountExists) {
            return "Email or Phone Number Not Found";
        }

        String otp = generateOtp();

        Otp otpEntity = Otp.builder()
                .emailPhone(emailOrPhone)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .attempts(0)
                .build();

        otpRepository.save(otpEntity);

        if (isEmail) {
            emailService.sendOtp(emailOrPhone, otp);
        } else {
            whatsAppService.sendOtp(emailOrPhone, otp);
        }

        return "OTP Sent Successfully";
    }

    public String verifyOtp(String emailOrPhone, String otp) {

        Otp savedOtp = otpRepository
                .findTopByEmailPhoneOrderByIdDesc(emailOrPhone)
                .orElse(null);

        if (savedOtp == null) {
            return "OTP Not Found";
        }

        if (savedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            return "OTP Expired";
        }

        if (savedOtp.getAttempts() >= 5) {
            return "Too Many Wrong Attempts";
        }

        if (!savedOtp.getOtp().equals(otp)) {
            savedOtp.setAttempts(savedOtp.getAttempts() + 1);
            otpRepository.save(savedOtp);
            return "Invalid OTP";
        }

        savedOtp.setVerified(true);
        otpRepository.save(savedOtp);

        return "OTP Verified";
    }

    public String resetPassword(String emailOrPhone, String newPassword) {

        Otp savedOtp = otpRepository
                .findTopByEmailPhoneOrderByIdDesc(emailOrPhone)
                .orElse(null);

        if (savedOtp == null || !savedOtp.isVerified()) {
            return "Verify OTP First";
        }

        if (savedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            return "OTP Expired";
        }

        boolean isEmail = emailOrPhone.contains("@");
        String encodedPassword = passwordEncoder.encode(newPassword);

        if (isEmail) {

            User user = userRepository.findByEmail(emailOrPhone);
            if (user != null) {
                user.setPassword(encodedPassword);
                userRepository.save(user);
                otpRepository.delete(savedOtp);
                return "Password Reset Successful";
            }

            Teacher teacher = teacherRepository.findByEmail(emailOrPhone);
            if (teacher != null) {
                teacher.setPassword(encodedPassword);
                teacherRepository.save(teacher);
                otpRepository.delete(savedOtp);
                return "Password Reset Successful";
            }

            Student student = studentRepository.findByEmail(emailOrPhone);
            if (student != null) {
                student.setPassword(encodedPassword);
                studentRepository.save(student);
                otpRepository.delete(savedOtp);
                return "Password Reset Successful";
            }

        } else {

            User user = userRepository.findByPhone(emailOrPhone).orElse(null);
            if (user != null) {
                user.setPassword(encodedPassword);
                userRepository.save(user);
//                otpRepository.delete(savedOtp);
                return "Password Reset Successful";
            }

            Teacher teacher = teacherRepository.findByPhone(emailOrPhone).orElse(null);
            if (teacher != null) {
                teacher.setPassword(encodedPassword);
                teacherRepository.save(teacher);
//                otpRepository.delete(savedOtp);
                return "Password Reset Successful";
            }

            Student student = studentRepository.findByPhone(emailOrPhone).orElse(null);
            if (student != null) {
                student.setPassword(encodedPassword);
                studentRepository.save(student);
//                otpRepository.delete(savedOtp);
                return "Password Reset Successful";
            }
        }

        return "User Not Found";
    }

    private String generateOtp() {

        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);

        return String.valueOf(number);
    }
}