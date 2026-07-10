package com.sms.passwordReset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

	
	
    Optional<Otp> findTopByEmailPhoneOrderByIdDesc(String emailOrPhone);
    
    
    
}
