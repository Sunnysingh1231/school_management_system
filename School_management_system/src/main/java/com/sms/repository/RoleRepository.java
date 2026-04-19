package com.sms.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Role;
import com.sms.model.User;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer>{

	public Role findByName(String name);

	boolean existsByName(String role);

	public void save(String role);
	
	
}
