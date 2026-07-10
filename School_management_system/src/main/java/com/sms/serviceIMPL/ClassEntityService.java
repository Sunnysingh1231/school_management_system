package com.sms.serviceIMPL;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sms.model.ClassEntity;
import com.sms.repository.ClassRepository;
import com.sms.serviceInterface.ClassEntityInterface;

@Service
public class ClassEntityService implements ClassEntityInterface{

	@Autowired
	private ClassRepository classRepository;

//	@Override
//	public Optional<ClassEntity> findClassByClassNameAndScetion(String cls, String sec) {
//		return classRepository.findByClassNameAndSection(cls,sec);
//	}
}
