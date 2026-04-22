package com.sms.controller;

import java.net.http.HttpRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.sms.model.Attendence;
import com.sms.model.ClassEntity;
import com.sms.model.Student;
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


	public String getAcademicSession() {
	    LocalDate today = LocalDate.now();
	    int year = today.getYear();
	    int month = today.getMonthValue();

	    if (month >= 4) {
	        return year + "-" + (year + 1);
	    } else {
	        return (year - 1) + "-" + year;
	    }
	}
//	GET MAPPING------------------------------------------------------------------------
	
	@GetMapping
	public String dashboard(Model model,Integer class_id,HttpSession session) {
		
		model.addAttribute("classes",teacherServiceInterface.findAllClassByTeacher());
		
		Teacher teacher = teacherServiceInterface.getCurrentTeacher();
		
		if(class_id == null) {
			class_id = teacherServiceInterface.findAllClassByTeacher().get(0).getId();
		}
		
		if(userServiceInterface.findClassById(class_id).get().getClassTeacher().getId() != teacherServiceInterface.getCurrentTeacher().getId()) {
			return "error/403";
		}
		
		session.setAttribute("clsId", class_id);

		List<Attendence> attendence = teacherServiceInterface.findAttendenceByAndDate(LocalDate.now(), class_id);
		
		int ps = 0;
		int ts = teacherServiceInterface.findAllStudentByClassNameAndSection(teacherServiceInterface.findClassByClassId(class_id).get().getClassName(), teacherServiceInterface.findClassByClassId(class_id).get().getSection()).size();
		
		for(Attendence a: attendence) {
			if(a.getStatus().equals("PRESENT")) {
				ps++;
			}
		}
		
		model.addAttribute("dateNow", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy")));
		model.addAttribute("teacher", teacher);
		model.addAttribute("attCount",(ps*100)/ts);
		model.addAttribute("clsName","Class "+teacherServiceInterface.findClassByClassId(class_id).get().getClassName()+"("+teacherServiceInterface.findClassByClassId(class_id).get().getSection()+")");
		model.addAttribute("totalSCount",teacherServiceInterface.findAllStudentByClassNameAndSection(teacherServiceInterface.findClassByClassId(class_id).get().getClassName(), teacherServiceInterface.findClassByClassId(class_id).get().getSection()).size());

		return "/teacher/index";
	}
	
//	@GetMapping
//	public String index() {
//		return "teacher/index";
//	}
	
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
	public String findAllStudent(HttpServletRequest request, Model model,HttpSession session) {
		

		Integer cls = (Integer) session.getAttribute("clsId");

		model.addAttribute("students", teacherServiceInterface.findAllStudentByClsId(cls));
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/view_student :: studentlist"; // fragment
	    }

	    return "redirect:/teacher";
		
		
	}
	
	@GetMapping("/student/view/{id}")
	public String viewStudentDetail(@PathVariable Integer id, HttpServletRequest request, Model model,HttpSession session) {
		

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

		
		model.addAttribute("student", studentServiceInterface.findStudentByStudentId(id).get());
		
		
		
		List<String> monthList = new ArrayList<>(Arrays.asList(
			    "April", "May", "June", "July", "August", "September",
			    "October", "November", "December", "January", "February", "March"
			));
		
		model.addAttribute("monthList", monthList);
		model.addAttribute("sess", getAcademicSession());
		
		System.out.println(getAcademicSession());
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
	        return "teacher/pay_student :: pay"; // fragment
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
	public String updateStudent(Model model, HttpSession session) {
		
		
		
		return "redirect:/teacher/student";
	}
}
