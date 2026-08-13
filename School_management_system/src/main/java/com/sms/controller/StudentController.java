package com.sms.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.json.JSONObject;
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
import com.sms.model.Notification;
import com.sms.model.NotificationReceiver;
import com.sms.model.Student;
import com.sms.model.StudentAssignment;
import com.sms.model.StudentAssignmentDto;
import com.sms.model.StudentFee;
import com.sms.model.Timetable;
import com.sms.repository.TimeTableRepository;
import com.sms.serviceInterface.StudentServiceInterface;
import com.sms.serviceInterface.TeacherServiceInterface;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.var;
@Controller
@RequestMapping("student")
public class StudentController {
	
	private final RazorpayClient razorpayClient;

	@Autowired
	private StudentServiceInterface studentServiceInterface;
	
	@Autowired
	private TeacherServiceInterface teacherServiceInterface;

	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	private TimeTableRepository timeTableRepository;

	StudentController(RazorpayClient razorpayClient) {
		this.razorpayClient = razorpayClient;
	}
	
//	@GetMapping("/test")
//	@ResponseBody
//	public String test() {
//		
//		
//	}
	
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
		
		
		List<NotificationReceiver> n = studentServiceInterface.top3notification(s1);
		
		model.addAttribute("top3notification", n);
		
		List<Timetable> t1 = timeTableRepository.findAllByClassEntityId(s1.getClassEntity().getId());
		model.addAttribute("timetable", t1);
		
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/dashboard :: dashboard"; // fragment
	    }
		return "/student/dashboard";

	}
	
	@GetMapping("/profile")
	public String profile(Model model) {
		
		
		model.addAttribute("activePage", "profile");
		
		return "/student/profile";
	}
	
	@GetMapping("/attendence")
	public String attendence(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
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
		
		return "/student/attendence";
	}
	
	@GetMapping("/assignment")
	public String assignment(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		model.addAttribute("activePage", "assignment");
		model.addAttribute("assignments", studentServiceInterface.findAllStudentAssignment(s1));
		
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
		
		model.addAttribute("activePage", "result_grade");
		
		return "/student/result_grade";
	}
	
	@GetMapping("/fees")
	public String fees(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
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
		
		model.addAttribute("activePage", "fees");

		model.addAttribute("paid", paid);
		model.addAttribute("pending", pending);
		
		
		
		return "/student/fees";
	}
	
	@GetMapping("/time-table")
	public String timeTable(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("activePage", "time_table");
		
		List<Timetable> t1 = timeTableRepository.findAllByClassEntityId(s1.getClassEntity().getId());
		model.addAttribute("timetable", t1);
		
		return "/student/time_table";
	}
	
	@GetMapping("/k")
	@ResponseBody
	public List<Timetable> cls(){
		
		List<Timetable> t1 = timeTableRepository.findAllByClassEntityId(57);
		
		return t1;
	}
	
	@GetMapping("/notice")
	public String notice(Model model) {
		
		Student s1 = studentServiceInterface.getCurrentStudent();
		
		List<NotificationReceiver> notificationList = studentServiceInterface.studentNotice(s1);
		Collections.reverse(notificationList);
		
		model.addAttribute("activePage", "notice");
		
		model.addAttribute("notificationList", notificationList);
		
		
		return "/student/notice";
	}
	
	@GetMapping("/chat")
	public String chat(Model model) {
		Student s1 = studentServiceInterface.getCurrentStudent();
		model.addAttribute("user", s1);
		return "/student/chat";
		
	}
	
	@GetMapping("/setting")
	public String setting(Model model) {
		
		model.addAttribute("activePage", "setting");
		
		return "/student/setting";
		
	}
	
	@GetMapping("/profile-edit")
	public String edit(Model model) {
		
		model.addAttribute("activePage", "profile");
		
		return "/student/profile_edit";
		
	}
	
	@GetMapping("/change-password")
	public String changePassword(Model model) {
		
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
				
		return "redirect:/student/notice";
	}
	@PostMapping("/create_payment")
	@ResponseBody
	public String createPayment(@RequestParam String amount) throws RazorpayException {		
				
		int a = new BigDecimal(amount).intValueExact();
		
		RazorpayClient  clint = new RazorpayClient("rzp_test_TPErYV1hRv1w5l","hwhVzaph532E9cy2hZABWqKR");
		
		JSONObject request = new JSONObject();
        request.put("amount", a*100);
        request.put("currency", "INR");
        request.put("receipt", "SMS_put");

        Order razorpayOrder = clint.Orders.create(request);
		
        System.out.println(razorpayOrder);
		
		return razorpayOrder.toString();
		
	}
	
}
