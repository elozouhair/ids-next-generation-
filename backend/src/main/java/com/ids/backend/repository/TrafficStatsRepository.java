package com.ids.backend.repository;

import com.ids.backend.model.TrafficStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TrafficStatsRepository extends JpaRepository<TrafficStats, Long> {

    TrafficStats findTopByOrderByTimestampDesc();

    @Query("SELECT t FROM TrafficStats t WHERE t.timestamp >= :since ORDER BY t.timestamp ASC")
    List<TrafficStats> findStatsSince(@Param("since") Instant since);

    @Query("SELECT AVG(t.attackPercentage) FROM TrafficStats t WHERE t.timestamp >= :since")
    Double averageAttackPercentage(@Param("since") Instant since);
}
