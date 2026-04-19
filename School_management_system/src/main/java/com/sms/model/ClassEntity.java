package com.sms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "classes")
public class ClassEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "section", length = 10)
    private String section;

    // Many classes belong to one school
    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;

    // Many classes can have one class teacher
    @ManyToOne
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;

}
