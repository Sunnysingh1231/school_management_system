package com.sms.controller;

import java.time.LocalDateTime;
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

import com.sms.model.Role;
import com.sms.model.School;
import com.sms.model.User;
import com.sms.repository.RoleRepository;
import com.sms.repository.SchoolRepository;
import com.sms.repository.UserRepository;
import com.sms.serviceInterface.SuperAdminServiceInterface;

@Controller
@RequestMapping("/super")
public class SuperController {
	
	@Autowired
	private SuperAdminServiceInterface superAdminServiceInterface;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private SchoolRepository schoolRepository;
		
	
// GET MAPPING-------------------------------------------------------------------------------------------------------------
	
	
	@GetMapping
	public String dashboard() {
		superAdminServiceInterface.initializeRoles();
		return "/super_admin_html/dashboard";
	}
	

	
	@GetMapping("/user_register")
	public String reg_form(Model model) {
		
		model.addAttribute("user", new User());
		
		List<School> allSchools = schoolRepository.findAll();
		
		model.addAttribute("schools", allSchools);
		
			
		return "/super_admin_html/user_registration_form";
	}
	
	@GetMapping("/register_school")
	public String addNewSchool(Model model) {
		
		model.addAttribute("school",new School());
			
		return "/super_admin_html/school_registration_form";
	}
	
	@GetMapping("/schools")
	public String viewAllSchool(Model model) {
		
		model.addAttribute("schools", schoolRepository.findAll());
		
		return "/super_admin_html/view_school";
	}
	
	@GetMapping("/view-school-detail/{id}")
	public String schoolViewDetail(@PathVariable int id, Model model) {
		
		Optional<School> school = schoolRepository.findById(id);
		
		if(school.isPresent()){
			model.addAttribute("school", school.get());
	    }
		
		return "/super_admin_html/view_school_detail";
	}
//	private List<String> states = List.of(
//			"Uttar Pradesh","Maharashtra","Bihar","West Bengal","Madhya Pradesh",
//			"Tamil Nadu","Rajasthan","Karnataka","Gujarat","Andhra Pradesh","Odisha",
//			"Telangana","Kerala","Jharkhand","Assam","Punjab","Chhattisgarh","Haryana",
//			"Uttarakhand","Himachal Pradesh","Tripura","Meghalaya","Manipur","Nagaland",
//			"Goa","Arunachal Pradesh","Mizoram","Sikkim"
//			);
//	@GetMapping("/searchState")
//	@ResponseBody
//	public List<String> searchState(@RequestParam("term") String term){
//		return states.stream().filter(s->s.toLowerCase().startsWith(term.toLowerCase())).toList();
//	}
	
	@GetMapping("/test")
	@ResponseBody
	public List<User> test() {
		return userRepository.findAll();
	}
	
	
	@PostMapping("/user_register")
	public String reg_form_log(@ModelAttribute User user,String role, String sch) {
				
		School s3 = new School();
		
		List<Role> r1 = roleRepository.findAll();
		
		List<School> s1 = schoolRepository.findAll();
		
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		
		for(School s2 : s1) {
			if(s2.getName().equals(sch)) {
				user.setSchool(s2);
			}
		}
		
		for(Role r2 : r1) {
			if(r2.getName().equals(role)) {
				user.setRoles(r2);
			}
		}
		
//		System.out.println(user);
				
		userRepository.save(user);
		
		return "super_admin_html/dashboard";
	}
	
	@PostMapping("/register_school")
	public String schoolRegister(@ModelAttribute School school) {
		
		school.setName(school.getName().toUpperCase());
		school.setCreatedAt(LocalDateTime.now());
		schoolRepository.save(school);
				
		return "/super_admin_html/dashboard";
	}

}
