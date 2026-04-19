package com.sms.userDetailService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;

@Service
public class CustomUserDetailService implements UserDetailsService{

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private TeacherRepository teacherRepository;
	
	@Autowired
	private StudentRepository studentRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				
		User user = userRepository.findByEmail(username);
		Teacher teacher = teacherRepository.findByEmail(username);
		Student student = studentRepository.findByEmail(username);
		
		
		if(user != null) {
			return new org.springframework.security.core.userdetails.User(
					user.getEmail(),
					user.getPassword(),
	                List.of(new SimpleGrantedAuthority(user.getRoles().getName())));
		}
		
		else if(teacher != null) {
			return new org.springframework.security.core.userdetails.User(
					teacher.getEmail(),
					teacher.getPassword(),
	                List.of(new SimpleGrantedAuthority(teacher.getRole().getName())));
		}
		
		else if(student != null) {
			return new org.springframework.security.core.userdetails.User(
					student.getEmail(),
					student.getPassword(),
	                List.of(new SimpleGrantedAuthority(student.getRole().getName())));
		}
		
		throw new UsernameNotFoundException("User not found");
		
		
//		List<GrantedAuthority> authorities =
//                u1.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName()))
//                        .collect(Collectors.toList());
		
		
//		GrantedAuthority authority =
//                new SimpleGrantedAuthority(u1.getRoles().getName());
		
	}
}
