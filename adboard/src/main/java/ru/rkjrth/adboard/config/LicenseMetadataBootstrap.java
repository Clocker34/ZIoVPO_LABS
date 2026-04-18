package ru.rkjrth.adboard.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.entity.LicenseProduct;
import ru.rkjrth.adboard.entity.LicenseType;
import ru.rkjrth.adboard.repository.LicenseProductRepository;
import ru.rkjrth.adboard.repository.LicenseTypeRepository;

/**
 * Если в БД ещё нет продуктов лицензий — создаёт демо-продукт и тип (удобно для Postman без ручных INSERT).
 */
@Component
@Order(2)
public class LicenseMetadataBootstrap implements CommandLineRunner {

    private final LicenseProductRepository productRepository;
    private final LicenseTypeRepository typeRepository;

    public LicenseMetadataBootstrap(
            LicenseProductRepository productRepository,
            LicenseTypeRepository typeRepository) {
        this.productRepository = productRepository;
        this.typeRepository = typeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        LicenseProduct p = new LicenseProduct();
        p.setName("ЗИоВПО — демо-продукт");
        p.setBlocked(false);
        productRepository.save(p);

        LicenseType t = new LicenseType();
        t.setName("Standard");
        t.setDefaultDurationInDays(365);
        t.setDescription("Автосоздание при пустой БД (локальная разработка / Postman)");
        typeRepository.save(t);
    }
}
