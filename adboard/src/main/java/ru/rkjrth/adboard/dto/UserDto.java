package ru.rkjrth.adboard.dto;

import java.util.UUID;

public record UserDto(UUID id, String name, String email) {
}
