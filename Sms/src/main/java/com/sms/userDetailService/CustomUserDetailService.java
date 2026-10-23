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

import com.sms.model.User;
import com.sms.repository.UserRepository;

@Service
public class CustomUserDetailService implements UserDetailsService{

	@Autowired
	private UserRepository userRepository;
	
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				
		User user = userRepository.findByEmail(username);
		
		
		if(user != null) {
			return new org.springframework.security.core.userdetails.User(
					user.getEmail(),
					user.getPassword(),
	                List.of(new SimpleGrantedAuthority(user.getRoles().getName())));
		}
		
		throw new UsernameNotFoundException("User not found");
		
		
//		List<GrantedAuthority> authorities =
//                u1.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName()))
//                        .collect(Collectors.toList());
		
		
//		GrantedAuthority authority =
//                new SimpleGrantedAuthority(u1.getRoles().getName());
		
	}
}
