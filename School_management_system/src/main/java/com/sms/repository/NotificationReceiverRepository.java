package com.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.NotificationReceiver;

@Repository
public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, Integer>{

}
