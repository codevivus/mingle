package com.mingle.match_service.repository;

import com.mingle.match_service.entity.Report;
import com.mingle.match_service.entity.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
}
