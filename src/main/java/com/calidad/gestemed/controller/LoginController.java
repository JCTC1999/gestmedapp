package com.calidad.gestemed.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /*
     * Este método manejará las peticiones GET a "/login" y mostrará
     * la plantilla de la página de inicio de sesión.
     * @return El nombre de la plantilla HTML (sin la extensión .html).
     */
    @GetMapping("/login")
    public String loginView() {
        return "login";
    }
}