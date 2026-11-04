package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Report;
import com.mingle.match_service.entity.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    List<Report> findByReporterOrderByCreatedAtAsc(Long reporter);

    /** Reports filed by the user. Reports about the user stay: they are evidence for moderators. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Report r where r.reporter = :userId")
    int deleteFiledBy(@org.springframework.data.repository.query.Param("userId") Long userId);
}
