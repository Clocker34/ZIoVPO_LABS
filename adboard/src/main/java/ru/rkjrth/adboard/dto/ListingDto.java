package ru.rkjrth.adboard.dto;

public record ListingDto(Long id, String title, String description, Double price,
                         Long authorId, Long categoryId, String status) {
}
