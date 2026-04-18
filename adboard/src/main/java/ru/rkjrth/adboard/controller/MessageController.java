package ru.rkjrth.adboard.controller;

import jakarta.validation.Valid;
import ru.rkjrth.adboard.entity.Message;
import ru.rkjrth.adboard.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<Message> getAll() {
        return messageService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> getById(@PathVariable Long id) {
        return messageService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Message> create(@RequestParam Long senderId,
                                          @RequestParam Long receiverId,
                                          @RequestParam Long listingId,
                                          @Valid @RequestBody Message message) {
        return messageService.create(senderId, receiverId, listingId, message)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Message> update(@PathVariable Long id, @Valid @RequestBody Message message) {
        return messageService.update(id, message)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = messageService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/listing/{listingId}")
    public List<Message> getConversationForListing(@PathVariable Long listingId) {
        return messageService.getConversationForListing(listingId);
    }
}
