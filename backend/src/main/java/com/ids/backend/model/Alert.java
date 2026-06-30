package com.ids.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alerts_timestamp", columnList = "timestamp"),
    @Index(name = "idx_alerts_attack_type", columnList = "attack_type"),
    @Index(name = "idx_alerts_src_ip", columnList = "src_ip"),
    @Index(name = "idx_alerts_severity", columnList = "severity")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "attack_type", nullable = false, length = 100)
    private String attackType;

    @Column(name = "src_ip", nullable = false, length = 45)
    private String srcIp;

    @Column(name = "dst_ip", nullable = false, length = 45)
    private String dstIp;

    @Column(name = "src_port")
    private Integer srcPort;

    @Column(name = "dst_port")
    private Integer dstPort;

    @Column(length = 10)
    private String protocol;

    @Column(length = 20)
    private String severity;

    private Double prediction;

    @Column(name = "raw_features", columnDefinition = "TEXT")
    private String rawFeatures;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }

    public String getSrcIp() { return srcIp; }
    public void setSrcIp(String srcIp) { this.srcIp = srcIp; }

    public String getDstIp() { return dstIp; }
    public void setDstIp(String dstIp) { this.dstIp = dstIp; }

    public Integer getSrcPort() { return srcPort; }
    public void setSrcPort(Integer srcPort) { this.srcPort = srcPort; }

    public Integer getDstPort() { return dstPort; }
    public void setDstPort(Integer dstPort) { this.dstPort = dstPort; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Double getPrediction() { return prediction; }
    public void setPrediction(Double prediction) { this.prediction = prediction; }

    public String getRawFeatures() { return rawFeatures; }
    public void setRawFeatures(String rawFeatures) { this.rawFeatures = rawFeatures; }
}
