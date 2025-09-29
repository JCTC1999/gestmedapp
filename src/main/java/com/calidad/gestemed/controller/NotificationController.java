package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Notification;
import com.calidad.gestemed.repo.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepo notificationRepo;

    @GetMapping("/api/geofenceNotifications")
    public String notificationsPage(Model model) {
        model.addAttribute("notifications", notificationRepo.findByMessageContainingOrderByCreatedAtDesc("Alerta geocerca"));
        return "/tracking/notifications"; // Corresponde a notifications.html
    }

    @GetMapping("/api/notifications")
    @ResponseBody
    public List<Notification> getNotifications() {
        return notificationRepo.findAllByOrderByCreatedAtDesc();
    }
}