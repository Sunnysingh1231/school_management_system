package com.sms.model;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString
public class Period {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	
	private LocalTime startTime;
	
	private LocalTime endTime;
	
	@Column(length = 30)
	private String subject;
	
	@ManyToOne
	@JoinColumn(name = "timetable_id")
	@JsonBackReference
	private Timetable timetable;

}
