package com.ids.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "traffic_stats")
public class TrafficStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "total_packets")
    private Long totalPackets;

    @Column(name = "normal_count")
    private Long normalCount;

    @Column(name = "attack_count")
    private Long attackCount;

    @Column(name = "normal_percentage")
    private Double normalPercentage;

    @Column(name = "attack_percentage")
    private Double attackPercentage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Long getTotalPackets() { return totalPackets; }
    public void setTotalPackets(Long totalPackets) { this.totalPackets = totalPackets; }

    public Long getNormalCount() { return normalCount; }
    public void setNormalCount(Long normalCount) { this.normalCount = normalCount; }

    public Long getAttackCount() { return attackCount; }
    public void setAttackCount(Long attackCount) { this.attackCount = attackCount; }

    public Double getNormalPercentage() { return normalPercentage; }
    public void setNormalPercentage(Double normalPercentage) { this.normalPercentage = normalPercentage; }

    public Double getAttackPercentage() { return attackPercentage; }
    public void setAttackPercentage(Double attackPercentage) { this.attackPercentage = attackPercentage; }
}
