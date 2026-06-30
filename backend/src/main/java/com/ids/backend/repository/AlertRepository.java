package com.ids.backend.repository;

import com.ids.backend.dto.AlertDto;
import com.ids.backend.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query(value = "SELECT a.id, a.timestamp, a.attack_type, a.src_ip, a.dst_ip, a.src_port, a.dst_port, a.protocol, a.severity, a.prediction FROM alerts a ORDER BY a.timestamp DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Object[]> findAlertPage(@Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT COUNT(*) FROM alerts", nativeQuery = true)
    long countAll();

    List<Alert> findBySeverityOrderByTimestampDesc(String severity);

    List<Alert> findByAttackTypeOrderByTimestampDesc(String attackType);

    @Query(value = "SELECT a.attack_type, COUNT(*) as cnt FROM alerts a GROUP BY a.attack_type ORDER BY cnt DESC LIMIT 10", nativeQuery = true)
    List<Object[]> countByAttackType();

    @Query(value = "SELECT a.src_ip, COUNT(*) as cnt FROM alerts a GROUP BY a.src_ip ORDER BY cnt DESC LIMIT 10", nativeQuery = true)
    List<Object[]> countBySourceIp();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.timestamp >= :since")
    long countRecentAlerts(@Param("since") Instant since);

    @Query(value = "SELECT COUNT(*) FROM alerts a WHERE a.severity = 'high' AND a.timestamp >= :since", nativeQuery = true)
    long countRecentHighSeverity(@Param("since") Instant since);

    long countByAttackType(String attackType);
}
