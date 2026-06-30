package com.ids.backend.dto;

import java.time.Instant;

public class AlertDto {
    private Long id;
    private Instant timestamp;
    private String attackType;
    private String srcIp;
    private String dstIp;
    private Integer srcPort;
    private Integer dstPort;
    private String protocol;
    private String severity;
    private Double prediction;

    public AlertDto(Long id, Instant timestamp, String attackType, String srcIp, String dstIp,
                    Integer srcPort, Integer dstPort, String protocol, String severity, Double prediction) {
        this.id = id;
        this.timestamp = timestamp;
        this.attackType = attackType;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.severity = severity;
        this.prediction = prediction;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getAttackType() { return attackType; }
    public String getSrcIp() { return srcIp; }
    public String getDstIp() { return dstIp; }
    public Integer getSrcPort() { return srcPort; }
    public Integer getDstPort() { return dstPort; }
    public String getProtocol() { return protocol; }
    public String getSeverity() { return severity; }
    public Double getPrediction() { return prediction; }
}
