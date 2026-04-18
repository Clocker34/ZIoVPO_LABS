package ru.rkjrth.adboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.entity.Category;
import ru.rkjrth.adboard.entity.Listing;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.CategoryRepository;
import ru.rkjrth.adboard.repository.ListingRepository;
import ru.rkjrth.adboard.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ListingService(ListingRepository listingRepository,
                          UserRepository userRepository,
                          CategoryRepository categoryRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Listing> getAll() {
        return listingRepository.findAll();
    }

    public Optional<Listing> getById(Long id) {
        return listingRepository.findById(id);
    }

    @Transactional
    public Optional<Listing> create(Long ownerId, Long categoryId, Listing listing) {
        Optional<User> ownerOpt = userRepository.findById(ownerId);
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (ownerOpt.isEmpty() || categoryOpt.isEmpty()) {
            return Optional.empty();
        }
        listing.setId(null);
        listing.setOwner(ownerOpt.get());
        listing.setCategory(categoryOpt.get());
        return Optional.of(listingRepository.save(listing));
    }

    @Transactional
    public Optional<Listing> update(Long id, Listing updatedData) {
        return listingRepository.findById(id).map(existing -> {
            existing.setTitle(updatedData.getTitle());
            existing.setDescription(updatedData.getDescription());
            existing.setPrice(updatedData.getPrice());
            existing.setStatus(updatedData.getStatus());
            return existing;
        });
    }

    @Transactional
    public Optional<Listing> publish(Long id) {
        return listingRepository.findById(id).map(listing -> {
            listing.setStatus(Listing.Status.PUBLISHED);
            return listing;
        });
    }

    @Transactional
    public Optional<Listing> close(Long id) {
        return listingRepository.findById(id).map(listing -> {
            listing.setStatus(Listing.Status.CLOSED);
            listing.setClosedAt(java.time.OffsetDateTime.now());
            return listing;
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!listingRepository.existsById(id)) {
            return false;
        }
        listingRepository.deleteById(id);
        return true;
    }
}

