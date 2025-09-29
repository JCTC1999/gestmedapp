package com.calidad.gestemed.repo;
// repo/NotificationRepo.java
import com.calidad.gestemed.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification,Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByMessageContainingOrderByCreatedAtDesc(String message);


}