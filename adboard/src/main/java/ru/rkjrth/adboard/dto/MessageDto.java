package ru.rkjrth.adboard.dto;

public record MessageDto(Long id, String text, Long senderId, Long recipientId, Long listingId) {
}
