package ru.rkjrth.adboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.entity.Listing;
import ru.rkjrth.adboard.entity.Report;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.ListingRepository;
import ru.rkjrth.adboard.repository.ReportRepository;
import ru.rkjrth.adboard.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public ReportService(ReportRepository reportRepository,
                         UserRepository userRepository,
                         ListingRepository listingRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    public List<Report> getAll() {
        return reportRepository.findAll();
    }

    public Optional<Report> getById(Long id) {
        return reportRepository.findById(id);
    }

    @Transactional
    public Optional<Report> create(Long authorId, Long listingId, Report report) {
        Optional<User> authorOpt = userRepository.findById(authorId);
        Optional<Listing> listingOpt = listingRepository.findById(listingId);
        if (authorOpt.isEmpty() || listingOpt.isEmpty()) {
            return Optional.empty();
        }
        report.setId(null);
        report.setAuthor(authorOpt.get());
        report.setListing(listingOpt.get());
        return Optional.of(reportRepository.save(report));
    }

    @Transactional
    public Optional<Report> update(Long id, Report updatedData) {
        return reportRepository.findById(id).map(existing -> {
            existing.setReason(updatedData.getReason());
            existing.setStatus(updatedData.getStatus());
            return existing;
        });
    }

    @Transactional
    public Optional<Report> markInReview(Long id) {
        return reportRepository.findById(id).map(report -> {
            report.setStatus(Report.Status.IN_REVIEW);
            return report;
        });
    }

    @Transactional
    public Optional<Report> resolveAndCloseListing(Long id) {
        return reportRepository.findById(id).map(report -> {
            report.setStatus(Report.Status.RESOLVED);
            Listing listing = report.getListing();
            if (listing != null) {
                listing.setStatus(Listing.Status.CLOSED);
                listing.setClosedAt(java.time.OffsetDateTime.now());
            }
            return report;
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!reportRepository.existsById(id)) {
            return false;
        }
        reportRepository.deleteById(id);
        return true;
    }
}

