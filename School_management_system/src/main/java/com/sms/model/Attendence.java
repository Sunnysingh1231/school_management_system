package com.sms.model;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Attendence {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	private LocalDate date;
	
	private String status;
	
	@ManyToOne
	@JoinColumn(name = "school_id")
	private School school;
	
	@ManyToOne
	@JoinColumn(name = "class_id")
	private ClassEntity classEntity;
	
	@ManyToOne
	@JoinColumn(name = "student_id")
	private Student student;
	
}
