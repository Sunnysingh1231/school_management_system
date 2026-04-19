package com.sms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sms.model.Attendence;
import com.sms.model.Student;
import com.sms.serviceInterface.StudentServiceInterface;

@Controller
@RequestMapping("student")
public class StudentController {
	
	@Autowired
	private StudentServiceInterface studentServiceInterface;

	@Autowired
	PasswordEncoder passwordEncoder;
	
	@GetMapping
	public String dashboard(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		List<Attendence> attendences = studentServiceInterface.findAttendenceByStudentId(s1.getId());
		
		int count = attendences.size() == 0 ? 1 : attendences.size();
		int abs = 0;
		
		for(Attendence a1 : attendences) {
			if(a1.getStatus().equals("ABSENT")) {
				abs++;
			}
		}
		
		
		model.addAttribute("activePage", "dashboard");
		model.addAttribute("attendence", ((count-abs)*100)/count+"%");
		model.addAttribute("student", s1);
		

		return "/student/dashboard";
	}
	
	@GetMapping("/profile")
	public String profile(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		model.addAttribute("activePage", "profile");
		
		return "/student/profile";
	}
	
	@GetMapping("/attendence")
	public String attendence(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		List<Attendence> attendences = studentServiceInterface.findAttendenceByStudentId(s1.getId());
		
		int count = attendences.size() == 0 ? 1 : attendences.size();
		int abs = 0;
		
		for(Attendence a1 : attendences) {
			if(a1.getStatus().equals("ABSENT")) {
				abs++;
			}
		}
		
		model.addAttribute("activePage", "attendance");
		model.addAttribute("totalDays", attendences.size());
		model.addAttribute("presentDays", attendences.size()-abs);
		model.addAttribute("absentDays", abs);
		model.addAttribute("attendancePercent", ((count-abs)*100)/count);
		
		model.addAttribute("attendence6list", studentServiceInterface.findTop6AttendenceOfStudentByStudentId(s1.getId()));
		
		for(Attendence a1: studentServiceInterface.findTop6AttendenceOfStudentByStudentId(s1.getId())) {
			System.out.println(a1.getStatus());
		}
		
		
		return "/student/attendence";
	}
	
	@GetMapping("/assignment")
	public String assignment(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "assignment");
		
		return "/student/assignment";
	}
	
	@GetMapping("/result-grade")
	public String resultGrade(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "result_grade");
		
		return "/student/result_grade";
	}
	
	@GetMapping("/fees")
	public String fees(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "fees");
		
		return "/student/fees";
	}
	
	@GetMapping("/time-table")
	public String timeTable(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "time_table");
		
		return "/student/time_table";
	}
	
	@GetMapping("/notice")
	public String notice(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "notice");
		
		return "/student/notice";
	}
	
	@GetMapping("/setting")
	public String setting(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "setting");
		
		return "/student/setting";
		
	}
	
	@GetMapping("/profile-edit")
	public String edit(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "profile");
		
		return "/student/profile_edit";
		
	}
	
	@GetMapping("/change-password")
	public String changePassword(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "profile");
		
		return "/student/password_edit";
		
	}
	
	@PostMapping("/update")
	public String updateProfile(@ModelAttribute Student student) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		s1.setName(student.getName());
		s1.setEmail(student.getEmail());
		s1.setPhone(student.getPhone());
		s1.setGender(student.getGender());
		s1.setDateOfBirth(student.getDateOfBirth());
		s1.setParentName(student.getParentName());
		s1.setParentPhone(student.getParentPhone());
		s1.setAddress(student.getAddress());
		
		studentServiceInterface.updateStudent(s1);
				
		return "redirect:/student/profile";
		
	}
	
	@PostMapping("/update-password")
	public String updatePassword(
	        @RequestParam String oldPassword,
	        @RequestParam String newPassword,
	        @RequestParam String confirmPassword,
	        RedirectAttributes redirectAttributes) {

	    Student s1 = studentServiceInterface.getCurrentStudent();

	    if (!passwordEncoder.matches(oldPassword, s1.getPassword())) {
	        redirectAttributes.addFlashAttribute("error", "Old password is incorrect");
	        return "redirect:/student/change-password";
	    } 
	    else if (!newPassword.equals(confirmPassword)) {
	        redirectAttributes.addFlashAttribute("error", "Passwords do not match");
	        return "redirect:/student/change-password";
	    } 
	    else {
	        s1.setPassword(passwordEncoder.encode(newPassword));
	        studentServiceInterface.updateStudent(s1);
	        redirectAttributes.addFlashAttribute("success", "Password updated successfully");
	    }

	    return "redirect:/student/profile";
	}
}
