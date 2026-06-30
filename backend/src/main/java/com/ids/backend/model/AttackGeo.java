package com.ids.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attacks_geo", indexes = {
    @Index(name = "idx_attacks_geo_timestamp", columnList = "timestamp")
})
public class AttackGeo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "src_ip", nullable = false, length = 45)
    private String srcIp;

    @Column(name = "attack_type", nullable = false, length = 100)
    private String attackType;

    private Double latitude;

    private Double longitude;

    @Column(length = 20)
    private String severity;

    @Column(nullable = false)
    private Instant timestamp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSrcIp() { return srcIp; }
    public void setSrcIp(String srcIp) { this.srcIp = srcIp; }

    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
