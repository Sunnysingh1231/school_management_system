package com.sms.serviceIMPL;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sms.model.FeeStructure;
import com.sms.repository.FeeStructureRepository;
import com.sms.serviceInterface.FeeStructureInterface;

@Service
public class FeeStructureService implements FeeStructureInterface{
	
	@Autowired
	private FeeStructureRepository feeStructureRepository;
	
	@Override
	public BigDecimal findFeeAmountByClsId(int clsid) {
		return feeStructureRepository.findAmountByClassEntityId(clsid);
	}
}
