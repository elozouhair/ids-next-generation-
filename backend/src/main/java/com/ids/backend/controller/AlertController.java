package com.ids.backend.controller;

import com.ids.backend.dto.AlertDto;
import com.ids.backend.model.AttackGeo;
import com.ids.backend.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(alertService.getRecentAlerts(page, size));
    }

    @GetMapping("/alerts/type/{type}")
    public ResponseEntity<List<AlertDto>> getAlertsByType(@PathVariable String type) {
        return ResponseEntity.ok(alertService.getAlertsByType(type));
    }

    @GetMapping("/alerts/high-severity")
    public ResponseEntity<Map<String, Object>> getHighSeverity(
            @RequestParam(defaultValue = "60") int minutes) {
        long count = alertService.getRecentHighSeverity(minutes);
        return ResponseEntity.ok(Map.of("count", count, "minutes", minutes));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(alertService.getDashboardStats());
    }

    @GetMapping("/alerts/count")
    public ResponseEntity<Map<String, Long>> getAlertCount() {
        return ResponseEntity.ok(Map.of("count", alertService.getAlertCount()));
    }

    @GetMapping("/alerts/attack-timeline")
    public ResponseEntity<List<Map<String, Object>>> getAttackTimeline(
            @RequestParam(defaultValue = "60") int minutes) {
        return ResponseEntity.ok(alertService.getAttackTimeline(minutes));
    }

    @GetMapping("/geo/recent")
    public ResponseEntity<List<AttackGeo>> getRecentGeo() {
        return ResponseEntity.ok(alertService.getRecentGeoLocations());
    }
}
