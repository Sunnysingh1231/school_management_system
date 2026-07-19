package com.sms.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentAssignmentDto {

	private int id;
	private String title;
	private String subject;
	private String description;
	private LocalDate assignDate;
	private LocalDate dueDate;
		
}
