package com.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Role;
import com.sms.model.Teacher;
import com.sms.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>{

	User findByName(String name);
	
	User findByEmail(String username);
	
	Optional<User> findByPhone(String phoneNumber);

}
