package com.sms.serviceInterface;

import java.math.BigDecimal;

import com.sms.model.FeeStructure;

public interface FeeStructureInterface {

	BigDecimal findFeeAmountByClsId(int clsid);

}
