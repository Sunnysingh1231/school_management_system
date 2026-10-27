package com.sms.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin")
public class SchoolAdminController {

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

		return "/school_admin/dashboard";

	}

}
