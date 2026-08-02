package com.sms.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.sms.model.NotificationReceiver;
import com.sms.model.Student;
import com.sms.serviceInterface.StudentServiceInterface;


@ControllerAdvice
public class StudentGlobleController {
	
	@Autowired
	private StudentServiceInterface studentServiceInterface;
	
	@ModelAttribute
	public void addCommonData(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		List<NotificationReceiver> notificationList = studentServiceInterface.studentNotice(s1);
		Collections.reverse(notificationList);
		int n1 = 0;
		for(NotificationReceiver nr : notificationList) {
			if(nr.getStatus().equals("UNREAD")) {
				n1++;
			}
		}
		
		model.addAttribute("unreadNoticeC", n1);
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
	}

}
