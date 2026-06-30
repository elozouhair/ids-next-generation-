package com.ids.backend.repository;

import com.ids.backend.model.AttackGeo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AttackGeoRepository extends JpaRepository<AttackGeo, Long> {

    List<AttackGeo> findTop500ByOrderByTimestampDesc();

    @Query("SELECT a FROM AttackGeo a WHERE a.timestamp >= ?1 ORDER BY a.timestamp DESC")
    List<AttackGeo> findRecentSince(Instant since);

    long countByAttackType(String attackType);
}
