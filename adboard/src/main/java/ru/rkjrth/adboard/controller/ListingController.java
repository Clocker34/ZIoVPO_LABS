package ru.rkjrth.adboard.controller;

import jakarta.validation.Valid;
import ru.rkjrth.adboard.entity.Listing;
import ru.rkjrth.adboard.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public List<Listing> getAll() {
        return listingService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getById(@PathVariable Long id) {
        return listingService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Listing> create(@RequestParam Long ownerId,
                                          @RequestParam Long categoryId,
                                          @Valid @RequestBody Listing listing) {
        return listingService.create(ownerId, categoryId, listing)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Listing> update(@PathVariable Long id, @Valid @RequestBody Listing listing) {
        return listingService.update(id, listing)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = listingService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/publish") //Публикция объявления
    public ResponseEntity<Listing> publish(@PathVariable Long id) {
        return listingService.publish(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/close") //Закрытие объявления
    public ResponseEntity<Listing> close(@PathVariable Long id) {
        return listingService.close(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
