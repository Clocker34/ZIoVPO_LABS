package ru.rkjrth.adboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.entity.Listing;
import ru.rkjrth.adboard.entity.Message;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.ListingRepository;
import ru.rkjrth.adboard.repository.MessageRepository;
import ru.rkjrth.adboard.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          ListingRepository listingRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    public List<Message> getAll() {
        return messageRepository.findAll();
    }

    public Optional<Message> getById(Long id) {
        return messageRepository.findById(id);
    }

    @Transactional
    public Optional<Message> create(Long senderId, Long receiverId, Long listingId, Message message) {
        Optional<User> senderOpt = userRepository.findById(senderId);
        Optional<User> receiverOpt = userRepository.findById(receiverId);
        Optional<Listing> listingOpt = listingRepository.findById(listingId);
        if (senderOpt.isEmpty() || receiverOpt.isEmpty() || listingOpt.isEmpty()) {
            return Optional.empty();
        }
        message.setId(null);
        message.setSender(senderOpt.get());
        message.setReceiver(receiverOpt.get());
        message.setListing(listingOpt.get());
        return Optional.of(messageRepository.save(message));
    }

    public List<Message> getConversationForListing(Long listingId) {
        return listingRepository.findById(listingId)
                .map(messageRepository::findByListing)
                .orElseGet(List::of);
    }

    @Transactional
    public Optional<Message> update(Long id, Message updatedData) {
        return messageRepository.findById(id).map(existing -> {
            existing.setText(updatedData.getText());
            return existing;
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!messageRepository.existsById(id)) {
            return false;
        }
        messageRepository.deleteById(id);
        return true;
    }
}

