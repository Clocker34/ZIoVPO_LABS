package ru.rkjrth.adboard.controller;

import jakarta.validation.Valid;
import ru.rkjrth.adboard.entity.Report;
import ru.rkjrth.adboard.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<Report> getAll() {
        return reportService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getById(@PathVariable Long id) {
        return reportService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Report> create(@RequestParam Long authorId,
                                         @RequestParam Long listingId,
                                         @Valid @RequestBody Report report) {
        return reportService.create(authorId, listingId, report)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Report> update(@PathVariable Long id, @Valid @RequestBody Report report) {
        return reportService.update(id, report)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = reportService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/in-review")
    public ResponseEntity<Report> markInReview(@PathVariable Long id) {
        return reportService.markInReview(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/resolve-and-close-listing")
    public ResponseEntity<Report> resolveAndCloseListing(@PathVariable Long id) {
        return reportService.resolveAndCloseListing(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
