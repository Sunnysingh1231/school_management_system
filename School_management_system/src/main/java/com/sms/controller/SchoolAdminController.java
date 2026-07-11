package com.sms.controller;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sms.model.ClassEntity;
import com.sms.model.FeeStructure;
import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.repository.ClassRepository;
import com.sms.repository.RoleRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.serviceInterface.TeacherServiceInterface;
import com.sms.serviceInterface.UserServiceInterface;

import jakarta.validation.constraints.Null;

@Controller
@RequestMapping("/admin")
public class SchoolAdminController {
	
	
	@Autowired
	private TeacherServiceInterface teacherServiceInterface;
	
	@Autowired
	private UserServiceInterface userServiceInterface;
	
	@Autowired
	private TeacherRepository teacherRepository;
	
	
// GET MAPPING-------------------------------------------------------------------
	
	
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
	
	@GetMapping
	public String dashboard(Model model) {
		
		
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		userServiceInterface.initializeClasses();
		userServiceInterface.initializeFee();
		
		return "/school_admin/dashboard";
		
	}
	
	@GetMapping("/test")
	@ResponseBody
	public Teacher test() {
		
		// SET SCHOOL NAME TO HEADER NAVBAR
//		System.out.println(userServiceInterface.findAllClasses());
		
		return teacherRepository.findByEmail("surya@gmail.com");
		
	}
	
	@GetMapping("/teacher-register")
	public String reg_form(Model model) {
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		model.addAttribute("teacher",new Teacher());
				
		return "/school_admin/teacher_registration_form";
		
	}
	
	@GetMapping("/view-teacher")
	public String viewTeacher(Model model) {
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		model.addAttribute("teachers",userServiceInterface.findAllTeachers());
		
		return "/school_admin/view_teacher";
		
	}
	
	@GetMapping("/view-classes")
	public String viewAllClasses(Model model) {
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		model.addAttribute("classes",userServiceInterface.findAllClasses());
		
		return "/school_admin/view_classes";
		
	}
	
	@GetMapping("/assign-class-teacher")
	public String assignClassTeacher(Model model) {
		
//		 SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
//		
		model.addAttribute("classes",userServiceInterface.findAllClasses());
		
		return "/school_admin/assign_class_teacher";
		
	}
	
	@GetMapping("/edit-class-teacher/{id}")
	public String editClassTeacher(@PathVariable int id, Model model) {
		
//		 SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		model.addAttribute("class", userServiceInterface.findClassById(id).get());
		model.addAttribute("teachers",userServiceInterface.findAllTeachers());

		if(userServiceInterface.getCurrentUser().getSchool().getId() != userServiceInterface.findClassById(id).get().getSchool().getId()) {
			return "error/403";
		}
		
		return "/school_admin/edit_class_teacher";
		
	}
	
	@GetMapping("/register-student")
	public String student_Registration(Model model) {
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		model.addAttribute("student",new Student());
				
		return "/school_admin/student_registration_form";
		
	}
	
	@GetMapping("/view-student")
	public String viewAllStudent(Model model,String cls,String sec) {
		
		if(cls == null || sec == null) {
			cls = "all";
			sec="all";
		}
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		
		if(!cls.equals("all") && !sec.equals("all")) {
			model.addAttribute("students",userServiceInterface.findAllStudentByClsAndSec(cls, sec));
		}
		else if(!cls.equals("all") && sec.equals("all")){
			model.addAttribute("students",userServiceInterface.findAllStudentByCls(cls));
			
		}
		else if(cls.equals("all") && !sec.equals("all")) {
			model.addAttribute("students",userServiceInterface.findAllStudentBySec(sec));
		}
		else {
			model.addAttribute("students",userServiceInterface.findAllStudent());
		}
		
		System.out.println(cls+" "+sec);
		
		return "/school_admin/view_student";
		
	}
	
	@GetMapping("/manage-student-fee")
	public String manageStudentFee(Model model) {
		
		// SET SCHOOL NAME TO HEADER NAVBAR
		model.addAttribute("schoolName",userServiceInterface.schoolName());
		
		model.addAttribute("feeStructure",userServiceInterface.findAllFeeStructureBySchool());
		
		model.addAttribute("sess",getAcademicSession());
				
		return "/school_admin/student_fee_manage";
		
	}
	
	
//	POST MAPPING------------------------------------------------------------------------
	
	
	@PostMapping("/teacher-register")
	public String techerRegistration(@ModelAttribute Teacher teacher, String ROLE_TEACHER) {
		
		userServiceInterface.addTeacher(teacher,"ROLE_TEACHER");
		
		return "/school_admin/dashboard";
	}
	
	@PostMapping("/edit-class-teacher/{id}")
	public String assignTeacherToClass(@PathVariable int id,int teacher) {

		
		userServiceInterface.assignTeacherToClass(id, teacher);
		
		return "/school_admin/dashboard";
	}
	
	@PostMapping("/register-student")
	public String studentRegistration(@ModelAttribute Student student,String classn,String section) {
		
		
		userServiceInterface.addStudent(student, "ROLE_STUDENT", classn, section);
		
		return "/school_admin/dashboard";
	}
	
	@PostMapping("/save-fee")
	public String setStudentFee(@RequestParam int classId, int amount, String feeType) {
				
		userServiceInterface.updateFeeStructure(classId, feeType, amount);
		
		return "redirect:/admin/manage-student-fee";
		
		
	}
}
