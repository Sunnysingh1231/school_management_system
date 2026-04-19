package com.sms.serviceIMPL;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sms.model.ClassEntity;
import com.sms.model.School;
import com.sms.repository.RoleRepository;
import com.sms.serviceInterface.SuperAdminServiceInterface;

@Service
public class SuperAdminService implements SuperAdminServiceInterface{

	@Autowired
	private RoleRepository roleRepository;
	
	@Override
	public void initializeRoles() {

        List<String> roles = Arrays.asList(
        		"ROLE_SUPER_ADMIN",
        		"ROLE_SCHOOL_ADMIN",
        		"ROLE_TEACHER",
        		"ROLE_STUDENT",
        		"ROLE_PARENT",
        		"ROLE_RECEPTIONIST",
        		"ROLE_LIBRARIAN",
        		"ROLE_ACCOUNTANT"
        );

        for (String role : roles) {
        	if (!roleRepository.existsByName(role)) {
                roleRepository.save(role);
            }
        }
    }
}
