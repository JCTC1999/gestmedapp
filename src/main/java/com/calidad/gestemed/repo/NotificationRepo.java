package com.calidad.gestemed.repo;
// repo/NotificationRepo.java
import com.calidad.gestemed.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepo extends JpaRepository<Notification,Long> {}
