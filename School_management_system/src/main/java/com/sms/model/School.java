package com.sms.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class School {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	private String name;
	
	private String email;
	
	private String phone;
	
	private String address;
	
	private String areaPost;
	
	private String pincode;
	
	private String city;
	
	private String district;
	
	private String state;
	
	private LocalDateTime createdAt;
	
//	@JsonManagedReference
	@JsonIgnore
	@OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
	private Set<User> users;
	
	@JsonIgnore
	@OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
	private Set<Teacher> teachers;
	
}
