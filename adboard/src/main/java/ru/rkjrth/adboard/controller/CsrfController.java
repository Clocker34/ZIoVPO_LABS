package ru.rkjrth.adboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public void csrf() {
        // Spring Security выставляет cookie XSRF-TOKEN
    }
}
