package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.NotificationReceiver;
import com.sms.model.Student;

@Repository
public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, Integer>{

	@Query("""
		    SELECT nr
		    FROM NotificationReceiver nr
		    JOIN FETCH nr.notification
		    WHERE nr.student = :student
		    """)
		List<NotificationReceiver> findAllByStudent(@Param("student") Student student);
	
	List<NotificationReceiver> findTop3ByStudentOrderByNotificationCreatedAtDesc(Student student);
	
	
}
