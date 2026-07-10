package com.sms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	private String name;
	
	@Column(unique = true)
	private String email;
	
	@Column(unique = true)
	private String phone;
	
	private String password;
	
	private boolean status = true;
	
//	@ManyToMany(fetch = FetchType.EAGER)
//	@JoinTable(
//			name = "user_roles",
//					joinColumns  = @JoinColumn(name = "user_id"),
//					inverseJoinColumns = @JoinColumn(name = "role_id")
//			)
//	private Set<Role> roles;
	
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "role_id")
	private Role roles;
	
//	@JsonBackReference
//	@JsonIgnore
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "school_id")
	private School school;
	
	
	
}
