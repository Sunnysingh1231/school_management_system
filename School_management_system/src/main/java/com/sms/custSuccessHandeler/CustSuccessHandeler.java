package com.sms.custSuccessHandeler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustSuccessHandeler implements AuthenticationSuccessHandler{

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		
		if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))) {
			response.sendRedirect("/super");
		}
//		else if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))) {
//			response.sendRedirect("/");
//		}
		else if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))){
			response.sendRedirect("/admin");
		}
		else if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER"))){
			response.sendRedirect("/teacher");
		}
		else if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT"))){
			response.sendRedirect("/student");
		}
		else {
			response.sendRedirect("/");
		}
		
		
	}

}
