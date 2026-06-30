package com.ids.backend.service;

import com.ids.backend.dto.AlertDto;
import com.ids.backend.model.Alert;
import com.ids.backend.model.AttackGeo;
import com.ids.backend.model.TrafficStats;
import com.ids.backend.repository.AlertRepository;
import com.ids.backend.repository.AttackGeoRepository;
import com.ids.backend.repository.TrafficStatsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final TrafficStatsRepository trafficStatsRepository;
    private final AttackGeoRepository attackGeoRepository;
    private final WebSocketAlertService webSocketAlertService;

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    private volatile Map<String, Object> dashboardCache;
    private final ReentrantLock cacheLock = new ReentrantLock();

    public AlertService(AlertRepository alertRepository,
                        TrafficStatsRepository trafficStatsRepository,
                        AttackGeoRepository attackGeoRepository,
                        WebSocketAlertService webSocketAlertService,
                        JdbcTemplate jdbcTemplate) {
        this.alertRepository = alertRepository;
        this.trafficStatsRepository = trafficStatsRepository;
        this.attackGeoRepository = attackGeoRepository;
        this.webSocketAlertService = webSocketAlertService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initMaterializedViews() {
        try {
            jdbcTemplate.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS mv_attack_distribution AS SELECT attack_type, COUNT(*) as cnt FROM alerts WHERE attack_type NOT IN ('BENIGN','Normal') GROUP BY attack_type ORDER BY cnt DESC");
            jdbcTemplate.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS mv_top_ips AS SELECT src_ip, COUNT(*) as cnt FROM alerts WHERE attack_type NOT IN ('BENIGN','Normal') GROUP BY src_ip ORDER BY cnt DESC LIMIT 50");
            jdbcTemplate.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS mv_attack_hourly AS SELECT date_trunc('hour', timestamp) as hour, attack_type, COUNT(*) as cnt FROM alerts WHERE attack_type NOT IN ('BENIGN','Normal') GROUP BY date_trunc('hour', timestamp), attack_type ORDER BY hour DESC");
            jdbcTemplate.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS mv_protocol_stats AS SELECT protocol, COUNT(*) as cnt FROM alerts WHERE attack_type NOT IN ('BENIGN','Normal') AND protocol IS NOT NULL GROUP BY protocol ORDER BY cnt DESC");
            jdbcTemplate.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS mv_severity_timeline AS SELECT date_trunc('hour', timestamp) as hour, severity, COUNT(*) as cnt FROM alerts WHERE attack_type NOT IN ('BENIGN','Normal') AND severity IS NOT NULL GROUP BY date_trunc('hour', timestamp), severity ORDER BY hour DESC");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_attack_distribution ON mv_attack_distribution(attack_type)");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_top_ips ON mv_top_ips(src_ip)");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_attack_hourly ON mv_attack_hourly(hour, attack_type)");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_protocol_stats ON mv_protocol_stats(protocol)");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_severity_timeline ON mv_severity_timeline(hour, severity)");
            log.info("Materialized views initialized successfully");
            dashboardCache = computeDashboardStats();
        } catch (Exception e) {
            log.warn("Failed to initialize materialized views: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void refreshMaterializedViews() {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_attack_distribution");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_ips");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_attack_hourly");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_protocol_stats");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_severity_timeline");
            log.info("Materialized views refreshed");
            Map<String, Object> fresh = computeDashboardStats();
            dashboardCache = fresh;
            webSocketAlertService.sendDashboardUpdate(fresh);
        } catch (Exception e) {
            log.warn("Failed to refresh materialized views: {}", e.getMessage());
        }
    }

    public Map<String, Object> getRecentAlerts(int page, int size) {
        int offset = page * size;
        List<Object[]> rows = alertRepository.findAlertPage(offset, size);
        long total = alertRepository.countAll();

        List<AlertDto> dtos = rows.stream()
                .map(r -> {
                    Instant ts = null;
                    if (r[1] != null) {
                        if (r[1] instanceof Instant) {
                            ts = (Instant) r[1];
                        } else if (r[1] instanceof java.sql.Timestamp) {
                            ts = ((java.sql.Timestamp) r[1]).toInstant();
                        } else {
                            ts = Instant.parse(r[1].toString());
                        }
                    }
                    return new AlertDto(
                            ((Number) r[0]).longValue(),
                            ts,
                            (String) r[2],
                            (String) r[3],
                            (String) r[4],
                            r[5] != null ? ((Number) r[5]).intValue() : null,
                            r[6] != null ? ((Number) r[6]).intValue() : null,
                            (String) r[7],
                            (String) r[8],
                            r[9] != null ? ((Number) r[9]).doubleValue() : null);
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", dtos);
        result.put("totalElements", total);
        result.put("totalPages", (total + size - 1) / size);
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> cached = dashboardCache;
        if (cached != null) return cached;
        if (cacheLock.tryLock()) {
            try {
                cached = dashboardCache;
                if (cached != null) return cached;
                Map<String, Object> fresh = computeDashboardStats();
                dashboardCache = fresh;
                return fresh;
            } finally {
                cacheLock.unlock();
            }
        }
        if (cached != null) return cached;
        return computeDashboardStats();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> computeDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long t0 = System.currentTimeMillis();

        stats.put("alerts_last_hour", alertRepository.countRecentAlerts(Instant.now().minus(1, ChronoUnit.HOURS)));
        stats.put("alerts_last_24h", alertRepository.countRecentAlerts(Instant.now().minus(24, ChronoUnit.HOURS)));

        List<Map<String, Object>> attackList = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT attack_type as type, cnt as count FROM mv_attack_distribution ORDER BY cnt DESC LIMIT 10");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>(row);
                attackList.add(item);
            }
        } catch (Exception e) {
            List<Object[]> attackTypes = alertRepository.countByAttackType();
            for (Object[] row : attackTypes) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", row[0]);
                item.put("count", row[1]);
                attackList.add(item);
            }
        }
        stats.put("attack_distribution", attackList);

        List<Map<String, Object>> ipList = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT src_ip as ip, cnt as count FROM mv_top_ips ORDER BY cnt DESC LIMIT 10");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>(row);
                ipList.add(item);
            }
        } catch (Exception e) {
            List<Object[]> topIps = alertRepository.countBySourceIp();
            for (int i = 0; i < Math.min(10, topIps.size()); i++) {
                Object[] row = topIps.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("ip", row[0]);
                item.put("count", row[1]);
                ipList.add(item);
            }
        }
        stats.put("top_ips", ipList);

        TrafficStats latestStats = trafficStatsRepository.findTopByOrderByTimestampDesc();
        if (latestStats != null) {
            stats.put("total_packets", latestStats.getTotalPackets());
            stats.put("normal_percentage", latestStats.getNormalPercentage());
            stats.put("attack_percentage", latestStats.getAttackPercentage());
        }

        List<TrafficStats> timeline = trafficStatsRepository.findStatsSince(Instant.now().minus(24, ChronoUnit.HOURS));
        stats.put("traffic_timeline", timeline);

        Double avgAttack = trafficStatsRepository.averageAttackPercentage(Instant.now().minus(24, ChronoUnit.HOURS));
        stats.put("avg_attack_percentage_24h", avgAttack != null ? avgAttack : 0.0);

        long highSeverityCount = alertRepository.countRecentHighSeverity(Instant.now().minus(1, ChronoUnit.HOURS));
        stats.put("high_severity_last_hour", highSeverityCount);

        List<Map<String, Object>> summaries = jdbcTemplate.queryForList(
            "SELECT attack_type as \"attackType\", count, avg_confidence as \"avgConfidence\", last_seen as \"lastSeen\" FROM attack_summary ORDER BY count DESC");
        stats.put("attack_summaries", summaries);

        long t1 = System.currentTimeMillis();
        log.info("Dashboard stats generated in {} ms", (t1 - t0));
        return stats;
    }

    public List<AlertDto> getAlertsByType(String type) {
        return alertRepository.findByAttackTypeOrderByTimestampDesc(type).stream()
                .map(a -> new AlertDto(a.getId(), a.getTimestamp(), a.getAttackType(), a.getSrcIp(), a.getDstIp(),
                        a.getSrcPort(), a.getDstPort(), a.getProtocol(), a.getSeverity(), a.getPrediction()))
                .collect(Collectors.toList());
    }

    public long getRecentHighSeverity(int minutes) {
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        return alertRepository.countRecentHighSeverity(since);
    }

    public long getAlertCount() {
        return alertRepository.count();
    }

    public List<AttackGeo> getRecentGeoLocations() {
        return attackGeoRepository.findTop500ByOrderByTimestampDesc();
    }

    public List<Map<String, Object>> getAttackTimeline(int minutes) {
        String sql = "SELECT date_trunc('minute', timestamp) as minute, attack_type, COUNT(*) as cnt " +
                     "FROM alerts WHERE timestamp >= NOW() - (? || ' minutes')::INTERVAL " +
                     "AND attack_type NOT IN ('BENIGN', 'Normal') " +
                     "GROUP BY date_trunc('minute', timestamp), attack_type ORDER BY minute ASC";
        return jdbcTemplate.queryForList(sql, String.valueOf(minutes));
    }
}
