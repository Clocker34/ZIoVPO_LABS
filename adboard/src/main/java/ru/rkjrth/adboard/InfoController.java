package ru.rkjrth.adboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "service", "adboard",
                "version", "0.0.1",
                "status", "OK"
        );
    }
}
