package ru.rkjrth.adboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.entity.Category;
import ru.rkjrth.adboard.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public Category create(Category category) {
        category.setId(null);
        return categoryRepository.save(category);
    }

    @Transactional
    public Optional<Category> update(Long id, Category updatedData) {
        return categoryRepository.findById(id).map(existing -> {
            existing.setName(updatedData.getName());
            existing.setDescription(updatedData.getDescription());
            return existing;
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            return false;
        }
        categoryRepository.deleteById(id);
        return true;
    }
}

