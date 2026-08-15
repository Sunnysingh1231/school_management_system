package com.sms.serviceIMPL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sms.model.StudentFee;
import com.sms.repository.StudentFeeRepository;
import com.sms.serviceInterface.StudentFeeServiceInterface;

@Service
public class StudentFeeService implements StudentFeeServiceInterface{
	
	@Autowired
	private StudentFeeRepository studentFeeRepository;
	
	@Override
	public void saveStudentFee(StudentFee sf) {
		studentFeeRepository.save(sf);
	}

}
