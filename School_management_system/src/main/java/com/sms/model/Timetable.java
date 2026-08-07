package com.sms.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class Timetable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	@Column(length = 10)
	private String day;
	
	@ManyToOne
	@JoinColumn(name = "class_id")
	@ToString.Exclude
	@JsonIgnore
	private ClassEntity classEntity;
	
	@OneToMany(mappedBy = "timetable",
	           cascade = CascadeType.ALL,
	           orphanRemoval = true)
	@JsonManagedReference
	private List<Period> periods = new ArrayList<>();
}
