package com.sms.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.ClassEntity;
import com.sms.model.FeeStructure;
import com.sms.model.School;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, Integer>{

	boolean existsBySchoolAndClassEntityAndFeeTypeAndSession(School school, ClassEntity classEntity, String string,
			String string2);

	List<FeeStructure> findAllFeeStructureBySchool(School school);

	Optional<FeeStructure> findBySchoolAndClassEntityIdAndFeeType(School school, Integer classId, String feeType);

	@Query("SELECT f.amount FROM FeeStructure f WHERE f.classEntity.id = :id")
	BigDecimal findAmountByClassEntityId(@Param("id") int id);


}
