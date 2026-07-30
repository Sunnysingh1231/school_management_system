package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer>{

	List<Notification> findAllByClassEntityId(int clsId);
		

}
