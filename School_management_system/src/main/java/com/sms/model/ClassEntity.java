package com.sms.model;


import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    
    @OneToMany(mappedBy = "classEntity")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Timetable> timetables;

}
