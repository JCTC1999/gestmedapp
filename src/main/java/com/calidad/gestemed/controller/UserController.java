package com.calidad.gestemed.controller;

import com.calidad.gestemed.repo.RoleRepository;
import com.calidad.gestemed.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;
    private final RoleRepository roleRepository;

    /*
     * Muestra el formulario para crear un nuevo usuario.
     */
    @GetMapping("/new")
    public String showRegistrationForm(Model model) {
        // Obtenemos todos los roles disponibles para mostrarlos como checkboxes
        // Filtramos el rol de ADMIN para no permitir crear más administradores desde la UI
        var availableRoles = roleRepository.findAll().stream()
                .filter(role -> !role.getName().equals("ROLE_ADMIN"))
                .collect(Collectors.toList());

        model.addAttribute("allRoles", availableRoles);
        return "users/new"; // Corresponde al archivo new.html en la carpeta templates/users
    }

    /*
     * Procesa el envío del formulario de registro.
     */
    @PostMapping
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(required = false) Set<String> roleNames,
                               RedirectAttributes redirectAttributes) {

        // Validar que se haya seleccionado al menos un rol
        if (roleNames == null || roleNames.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar al menos un rol para el usuario.");
            return "redirect:/admin/users/new";
        }

        try {
            userService.registerNewUser(username, password, roleNames);
            // Mensaje de éxito para mostrar en la siguiente página
            redirectAttributes.addFlashAttribute("success", "Usuario '" + username + "' registrado exitosamente.");
        } catch (Exception e) {
            // Mensaje de error si, por ejemplo, el usuario ya existe
            redirectAttributes.addFlashAttribute("error", "No se pudo registrar el usuario: " + e.getMessage());
            return "redirect:/admin/users/new";
        }

        return "redirect:/admin/roles";
    }
}