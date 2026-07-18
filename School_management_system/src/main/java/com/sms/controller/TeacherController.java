package com.sms.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.http.HttpRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.ClassEntity;
import com.sms.model.Student;
import com.sms.model.StudentFee;
import com.sms.model.Teacher;
import com.sms.repository.StudentRepository;
import com.sms.serviceIMPL.TeacherService;
import com.sms.serviceInterface.StudentServiceInterface;
import com.sms.serviceInterface.TeacherServiceInterface;
import com.sms.serviceInterface.UserServiceInterface;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("teacher")
public class TeacherController {
		
	@Autowired
	private TeacherServiceInterface teacherServiceInterface;
	
	@Autowired
	private UserServiceInterface userServiceInterface;
	
	@Autowired
	private StudentServiceInterface studentServiceInterface;
	

//	GET MAPPING------------------------------------------------------------------------
	
	@GetMapping
	public String dashboard(Model model,Integer class_id,HttpSession session,HttpServletRequest request) {
		
		Teacher teacher = teacherServiceInterface.getCurrentTeacher();
		
		if(class_id == null) {
			class_id = teacherServiceInterface.findAllClsByTeacherIdAndSclId(teacher.getId(), teacher.getSchool().getId()).get(0).getId();
		}
		
		session.setAttribute("clsId", class_id);

		List<Attendence> attendence = teacherServiceInterface.findAttendenceByAndDate(LocalDate.now(), class_id);
		
		int totalStudent = teacherServiceInterface.findAllStdByClsId(class_id).size();

		model.addAttribute("dateNow", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy")));
		model.addAttribute("teacher", teacher);
		model.addAttribute("totalSCount",totalStudent);

		int tp = 0;
		for(Attendence a: attendence) {
			if(a.getDate().equals(LocalDate.now()) && a.getStatus().equals("PRESENT")) {
				tp++;
			}
		}
				
		model.addAttribute("totalPresent", tp == 0 ? "Not Update" : tp);
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/dashboard :: dash"; // fragment
	    }
		return "/teacher/index";
	}
	
	@GetMapping("/profile")
	public String profile(HttpServletRequest request) {

	    if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/profile :: profile"; // fragment
	    }

	    return "redirect:/teacher"; // full page
	}
	
	@GetMapping("/test")
	@ResponseBody
	public List<ClassEntity> test() {
		
		for(ClassEntity c : teacherServiceInterface.findAllClassByTeacher()) {
			System.out.println(c.getSection());
		}
		
		return teacherServiceInterface.findAllClassByTeacher();
	}
	
	@GetMapping("/students")
	public String studentList(HttpServletRequest request, Model model,HttpSession session) {
		

		Integer cls = (Integer) session.getAttribute("clsId");

		model.addAttribute("students", teacherServiceInterface.findAllStdByClsId(cls));
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/view_student :: studentlist"; // fragment
	    }

	    return "redirect:/teacher";
		
		
	}
	
	@GetMapping("/student/view/{id}")
	public String studentProfile(@PathVariable Integer id, HttpServletRequest request, Model model,HttpSession session) {
		

		Integer cls = (Integer) session.getAttribute("clsId");
		List<Attendence> attendences = studentServiceInterface.findAttendenceByStudentId(id);
		
		int count = attendences.size() == 0 ? 1 : attendences.size();
		int abs = 0;
		
		for(Attendence a1 : attendences) {
			if(a1.getStatus().equals("ABSENT")) {
				abs++;
			}
		}
		model.addAttribute("attendancePercent", ((count-abs)*100)/count);
		model.addAttribute("student", studentServiceInterface.findStudentByStudentId(id).get());
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/view_student_detail :: studentView"; // fragment
	    }

	    return "redirect:/teacher/students";
		
		
	}
	
	@GetMapping("/students-attendence")
	public String studentAttendence(Model model,HttpSession session,HttpServletRequest request) {
		

		Integer cls = (Integer) session.getAttribute("clsId");

//		teacherServiceInterface.findAttendenceByStudentIdAndDateAndClassEntityId(teacherServiceInterface.getCurrentTeacher().getSchool().getId(), LocalDate.now(), cls);
		
		List<Attendence> attendence = teacherServiceInterface.findAttendenceByAndDate(LocalDate.now(), cls);
		
		Map<Integer, Boolean> attendanceMap = new HashMap<>();
		
		for(Attendence at : attendence) {
			boolean st = false;
			if(at.getStatus().equals("PRESENT")) {
				st = true;
			}
			
			attendanceMap.put(at.getStudent().getId(), st);
		}

		model.addAttribute("allAttendence",attendanceMap);
		model.addAttribute("students", teacherServiceInterface.findAllStudentByClsId(cls));
		
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/student_attendence :: attendence"; // fragment
	    }

	    return "redirect:/teacher";
	}
	
	
	@GetMapping("/student/edit/{id}")
	public String editStudent(HttpServletRequest request, @PathVariable int id,Model model) {

		Optional<Student> student = studentServiceInterface.findStudentByStudentId(id);
		
		model.addAttribute("student", student.get());
		
	    if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/edit_student_detail :: studentEdit"; // fragment
	    }

	    return "redirect:/teacher"; // full page
	}
	
	@GetMapping("/students/fees")
	public String fees(HttpServletRequest request, Model model,HttpSession session) {

		Integer cls = (Integer) session.getAttribute("clsId");

		model.addAttribute("students", teacherServiceInterface.findAllStudentByClsId(cls));
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/fees :: fees"; // fragment
	    }

	    return "redirect:/teacher";
	    
	}
	
	@GetMapping("/students/fees/pay/{id}")
	public String pay(HttpServletRequest request, Model model,@PathVariable int id) {
		
		Student student = studentServiceInterface.findStudentByStudentId(id).get();

		
		model.addAttribute("student", student);
		
		model.addAttribute("feeDetail", teacherServiceInterface.findStudentFeeDetail(student));
		
		model.addAttribute("sess", teacherServiceInterface.getAcademicSession());
		
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/pay_student :: pay"; // fragment
	    }

	    return "redirect:/teacher";
	    
	}
	
	@GetMapping("/assignments")
	public String assignment(HttpServletRequest request, HttpSession session,Model model) {
		
		
		Integer cls = (Integer) session.getAttribute("clsId");
		List<Assignment> allAssignments = teacherServiceInterface.findAllAsinmtByClsId(cls);
		Collections.reverse(allAssignments);

		
		ClassEntity classEntity = teacherServiceInterface.findClassByClassId(cls).get();
		Teacher teacher = teacherServiceInterface.getCurrentTeacher();
		
		model.addAttribute("count", allAssignments.size());
		
		model.addAttribute("cls", classEntity.getClassName()+"-"+classEntity.getSection());
		
		model.addAttribute("assignments", allAssignments);
		
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/assignment :: assign"; // fragment
	    }

	    return "redirect:/teacher";
	}
	
	
	
//	POST MAPPING-----------------------------------------------------------------------------------------------------
	
	@PostMapping("/students-attendence")
	public String addstudentAttendence(Model model, HttpSession session, @RequestParam(required = false) List<Integer> attends) {
		
		Integer cls = (Integer) session.getAttribute("clsId");

		model.addAttribute("students", teacherServiceInterface.findAllStudentByClsId(cls));
		
		teacherServiceInterface.saveAttendence(cls, attends);
		
		return "redirect:/teacher";
	}
	
	
	@PostMapping("/students/update")
	public String updateStudent(@ModelAttribute Student student) {
		
		Student s1 = studentServiceInterface.findStudentByStudentId(student.getId()).get();
		
		s1.setName(student.getName());
		s1.setRollNumber(student.getRollNumber());
		s1.setEmail(student.getEmail());
		s1.setPhone(student.getPhone());
		s1.setGender(student.getGender());
		s1.setParentName(student.getParentName());
		s1.setParentPhone(student.getParentPhone());
		
		studentServiceInterface.updateStudent(s1);
		
		return "redirect:/teacher";
	}
	
	@PostMapping("/student/fees/payment")
	public String payStudentFee(@RequestParam int studentId,@RequestParam String[] feeIds) {
		
		teacherServiceInterface.makeStudentPaymebnt(studentId,feeIds);
		
		return "redirect:/teacher";
	}
	
	@PostMapping("assignments/save")
	public String createStudentAssignment(@ModelAttribute Assignment assignment,HttpSession session) {
		
		Integer cls = (Integer) session.getAttribute("clsId");
		
		teacherServiceInterface.createStdAssignment(assignment, cls);
		
		return "redirect:/teacher";
	}
	
}
