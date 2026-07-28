package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.NotificationReceiver;
import com.sms.model.Student;

@Repository
public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, Integer>{

	List<NotificationReceiver> findAllByStudent(Student student);

}
