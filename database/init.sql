CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS attacks_geo (
    id BIGSERIAL PRIMARY KEY,
    src_ip VARCHAR(45) NOT NULL,
    attack_type VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geom geometry(Point, 4326) GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)) STORED,
    severity VARCHAR(20) DEFAULT 'medium',
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_attacks_geo_geom ON attacks_geo USING GIST (geom);
CREATE INDEX idx_attacks_geo_timestamp ON attacks_geo(timestamp DESC);
CREATE INDEX idx_attacks_geo_attack_type ON attacks_geo(attack_type);

CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    attack_type VARCHAR(100) NOT NULL,
    src_ip VARCHAR(45) NOT NULL,
    dst_ip VARCHAR(45) NOT NULL,
    src_port INTEGER,
    dst_port INTEGER,
    protocol VARCHAR(10),
    severity VARCHAR(20) DEFAULT 'medium',
    prediction DOUBLE PRECISION,
    true_label DOUBLE PRECISION,
    raw_features TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS traffic_stats (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    total_packets BIGINT DEFAULT 0,
    normal_count BIGINT DEFAULT 0,
    attack_count BIGINT DEFAULT 0,
    normal_percentage DOUBLE PRECISION DEFAULT 0,
    attack_percentage DOUBLE PRECISION DEFAULT 0
);

CREATE TABLE IF NOT EXISTS attack_summary (
    id BIGSERIAL PRIMARY KEY,
    attack_type VARCHAR(100) NOT NULL,
    count BIGINT DEFAULT 0,
    last_seen TIMESTAMP DEFAULT NOW(),
    avg_confidence DOUBLE PRECISION DEFAULT 0
);

CREATE TABLE IF NOT EXISTS model_metrics (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    total_samples BIGINT DEFAULT 0,
    true_positives BIGINT DEFAULT 0,
    true_negatives BIGINT DEFAULT 0,
    false_positives BIGINT DEFAULT 0,
    false_negatives BIGINT DEFAULT 0,
    accuracy DOUBLE PRECISION DEFAULT 0,
    precision DOUBLE PRECISION DEFAULT 0,
    recall DOUBLE PRECISION DEFAULT 0,
    f1_score DOUBLE PRECISION DEFAULT 0
);

CREATE INDEX idx_alerts_timestamp ON alerts(timestamp DESC);
CREATE INDEX idx_alerts_attack_type ON alerts(attack_type);
CREATE INDEX idx_alerts_src_ip ON alerts(src_ip);
CREATE INDEX idx_traffic_stats_timestamp ON traffic_stats(timestamp DESC);
