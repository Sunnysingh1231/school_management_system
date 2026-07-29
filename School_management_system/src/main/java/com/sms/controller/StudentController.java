package com.sms.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.NotificationReceiver;
import com.sms.model.Student;
import com.sms.model.StudentAssignment;
import com.sms.model.StudentAssignmentDto;
import com.sms.model.StudentFee;
import com.sms.serviceInterface.StudentServiceInterface;
import com.sms.serviceInterface.TeacherServiceInterface;

import jakarta.servlet.http.HttpServletRequest;
@Controller
@RequestMapping("student")
public class StudentController {
	
	@Autowired
	private StudentServiceInterface studentServiceInterface;
	
	@Autowired
	private TeacherServiceInterface teacherServiceInterface;

	@Autowired
	PasswordEncoder passwordEncoder;
	
	@GetMapping("/test")
	@ResponseBody
	public String test() {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		String month = LocalDate.now()
		        .getMonth()
		        .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		return month;
	}
	
	@GetMapping
	public String dashboard(Model model, HttpServletRequest request) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		List<Attendence> attendences = studentServiceInterface.findAttendenceByStudentId(s1.getId());
		
		boolean currentFee = studentServiceInterface.findCurrentMongthStudentFeeStatus(s1.getId());
		
		int count = attendences.size() == 0 ? 1 : attendences.size();
		int abs = 0;
		
		for(Attendence a1 : attendences) {
			if(a1.getStatus().equals("ABSENT")) {
				abs++;
			}
		}
		
		
		model.addAttribute("activePage", "dashboard");
		model.addAttribute("attendence", ((count-abs)*100)/count);
		model.addAttribute("student", s1);
		model.addAttribute("currentFee", currentFee);
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		int tAssignments = studentServiceInterface.findAllStudentAssignment(s1).size();
		List<Assignment> assignments = studentServiceInterface.findTop3AssignByClsId(s1.getClassEntity().getId());
				
		List<StudentAssignment> sAssignments = studentServiceInterface.findStdAssignBtStdId(s1.getId());
		List<StudentAssignment> nAssignments = new ArrayList<>();
		Short pa = (short) (tAssignments-sAssignments.size());
//		sAssignments.
//		
		for(Assignment a : assignments) {
			
			StudentAssignment s = new StudentAssignment();
			
			s.setAssignment(a);
			s.setSchool(a.getSchool());
			s.setSession(a.getSession());
			s.setStudent(s1);
			
			for(StudentAssignment sa : sAssignments) {
				if(s.getAssignment().equals(sa.getAssignment())) {
					s.setIsComplete(false);
				}
			}
			nAssignments.add(s);
			
		}
		
		model.addAttribute("assignments", nAssignments);
		model.addAttribute("pendingAssignment", pa);
		model.addAttribute("totalAssignment", tAssignments);
		
		
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/dashboard :: dashboard"; // fragment
	    }
		return "/student/dashboard";

	}
	
	@GetMapping("/profile")
	public String profile(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		model.addAttribute("activePage", "profile");
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
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
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		return "/student/attendence";
	}
	
	@GetMapping("/assignment")
	public String assignment(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("assignments", studentServiceInterface.findAllStudentAssignment(s1));
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		return "/student/assignment";
	}
	
	@GetMapping("/complete-assignment")
	@ResponseBody
	public List<StudentAssignmentDto> completeAssignment() {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		return studentServiceInterface.studentAssignmentDtos(s1);
	}
	
	@GetMapping("/pending-assignment")
	@ResponseBody
	public List<StudentAssignmentDto> pendingAssignment() {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		return studentServiceInterface.studentPendingAssignment(s1);
	}
	
	@GetMapping("/result-grade")
	public String resultGrade(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "result_grade");
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		return "/student/result_grade";
	}
	
	@GetMapping("/fees")
	public String fees(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		List<StudentFee> studentFees = teacherServiceInterface.findStudentFeeDetail(s1);
		model.addAttribute("fees", studentFees);
		
		BigDecimal paid = new BigDecimal("0.0");
		BigDecimal pending = new BigDecimal("0.0");
		for(StudentFee sf : studentFees) {
			if(sf.getStatus().equals("PAID")) {
				paid = paid.add(sf.getAmount());
			}
			else {
				pending = pending.add(sf.getAmount());
			}
		}
		
		model.addAttribute("paid", paid);
		model.addAttribute("pending", pending);
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		return "/student/fees";
	}
	
	@GetMapping("/time-table")
	public String timeTable(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "time_table");
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		return "/student/time_table";
	}
	
	@GetMapping("/notice")
	public String notice(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
		List<NotificationReceiver> notificationList = studentServiceInterface.studentNotice(s1);
		Collections.reverse(notificationList);
		
		model.addAttribute("activePage", "notice");
		
		model.addAttribute("notificationList", notificationList);
		
		
		return "/student/notice";
	}
	
	@GetMapping("/setting")
	public String setting(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("student", s1);
		
		model.addAttribute("activePage", "setting");
		
		String sesson = studentServiceInterface.getAcademicSession();
		model.addAttribute("sess", sesson);
		
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
	
	@PostMapping("/submit-Assignment")
	public String assignmentComplete(@RequestParam int assignmentId, @RequestParam int studentId) {
				
		studentServiceInterface.markAssignmentComplete(assignmentId, studentId);
				
		return "redirect:/student/assignment";
	}
	
	@PostMapping("/read-notice")
	public String readNotice(@RequestParam int noticeId) {
				
		studentServiceInterface.readNotice(noticeId);
		
		System.out.println("All fine...");
		
				
		return "redirect:/student/notice";
	}
}
