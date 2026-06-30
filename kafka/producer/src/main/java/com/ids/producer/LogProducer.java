package com.ids.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LogProducer {

    private static final Logger log = LoggerFactory.getLogger(LogProducer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();

    private static final String[] PROTOCOLS = {"TCP", "UDP", "ICMP"};
    private static final String[] ATTACK_IPS = {"192.168.1.100", "10.0.0.50", "172.16.0.25",
            "192.168.1.200", "10.0.0.100", "172.16.0.50"};
    private static final String[] NORMAL_IPS = {"192.168.1.10", "10.0.0.1", "172.16.0.1",
            "192.168.1.20", "10.0.0.2", "172.16.0.2"};

    public static void main(String[] args) throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092");
        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", "network-traffic");
        double attackRatio = Double.parseDouble(System.getenv().getOrDefault("ATTACK_RATIO", "0.3"));
        int messagesPerSecond = Integer.parseInt(System.getenv().getOrDefault("MESSAGES_PER_SECOND", "100"));

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down producer...");
            producer.close();
        }));

        log.info("Starting IDS Log Producer -> Kafka topic '{}' at {} ({} msg/s, {}% attack)",
                topic, bootstrapServers, messagesPerSecond, attackRatio * 100);

        while (true) {
            for (int i = 0; i < messagesPerSecond; i++) {
                boolean isAttack = random.nextDouble() < attackRatio;
                NetworkTraffic traffic = generateTraffic(isAttack);

                String json = mapper.writeValueAsString(traffic);

                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, traffic.srcIp, json);

                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error sending to Kafka", exception);
                    }
                });
            }

            if (messagesPerSecond <= 100) {
                TimeUnit.MILLISECONDS.sleep(950);
            }

            if (log.isDebugEnabled()) {
                log.debug("Sent {} messages to topic '{}'", messagesPerSecond, topic);
            } else if (System.currentTimeMillis() % 10000 < 1000) {
                log.info("Sent {} messages/sec to topic '{}'", messagesPerSecond, topic);
            }
        }
    }

    private static NetworkTraffic generateTraffic(boolean isAttack) {
        NetworkTraffic traffic = new NetworkTraffic();

        String[] ips = isAttack ? ATTACK_IPS : NORMAL_IPS;
        traffic.srcIp = ips[random.nextInt(ips.length)];
        traffic.dstIp = "10.0.0." + (random.nextInt(254) + 1);
        traffic.srcPort = isAttack ? random.nextInt(65535) : (1024 + random.nextInt(50000));
        traffic.dstPort = isAttack ? random.nextInt(100) + 1 : (80 + random.nextInt(4) * 80);
        traffic.protocol = PROTOCOLS[random.nextInt(PROTOCOLS.length)];
        traffic.timestamp = java.time.Instant.now().toString();

        if (isAttack) {
            traffic.flowDuration = random.nextDouble() * 1000;
            traffic.totalFwdPackets = 1000 + random.nextDouble() * 9000;
            traffic.totalBackwardPackets = 500 + random.nextDouble() * 3000;
            traffic.fwdPacketLengthMax = 1000 + random.nextDouble() * 500;
            traffic.fwdPacketLengthMin = 40 + random.nextDouble() * 100;
            traffic.fwdPacketLengthMean = 200 + random.nextDouble() * 400;
            traffic.bwdPacketLengthMax = 800 + random.nextDouble() * 400;
            traffic.bwdPacketLengthMin = 40 + random.nextDouble() * 100;
            traffic.bwdPacketLengthMean = 150 + random.nextDouble() * 300;
            traffic.flowBytesPerSec = 500000 + random.nextDouble() * 2000000;
            traffic.flowPacketsPerSec = 5000 + random.nextDouble() * 15000;
            traffic.fwdIatTotal = 1000 + random.nextDouble() * 5000;
            traffic.fwdIatMean = 100 + random.nextDouble() * 500;
            traffic.bwdIatTotal = 500 + random.nextDouble() * 2000;
            traffic.bwdIatMean = 50 + random.nextDouble() * 200;
            traffic.fwdPshFlags = random.nextInt(5);
            traffic.bwdPshFlags = random.nextInt(3);
            traffic.fwdUrgFlags = random.nextInt(2);
            traffic.bwdUrgFlags = random.nextInt(2);
            traffic.finFlagCount = random.nextInt(10);
            traffic.synFlagCount = 50 + random.nextInt(200);
            traffic.rstFlagCount = 10 + random.nextInt(50);
            traffic.pshFlagCount = random.nextInt(20);
            traffic.ackFlagCount = 100 + random.nextInt(500);
            traffic.urgFlagCount = random.nextInt(5);
            traffic.packetLengthMean = 300 + random.nextDouble() * 500;
            traffic.packetLengthStd = 100 + random.nextDouble() * 200;
            traffic.packetLengthVariance = 10000 + random.nextDouble() * 40000;
            traffic.avgPacketSize = 400 + random.nextDouble() * 400;
            traffic.avgFwdSegmentSize = 300 + random.nextDouble() * 300;
            traffic.avgBwdSegmentSize = 200 + random.nextDouble() * 200;
            traffic.subflowFwdPackets = 500 + random.nextDouble() * 3000;
            traffic.subflowFwdBytes = 100000 + random.nextDouble() * 500000;
            traffic.subflowBwdPackets = 200 + random.nextDouble() * 1000;
            traffic.subflowBwdBytes = 50000 + random.nextDouble() * 200000;
            traffic.initWinBytesForward = 10000 + random.nextDouble() * 50000;
            traffic.initWinBytesBackward = 5000 + random.nextDouble() * 20000;
            traffic.activeMean = 100 + random.nextDouble() * 500;
            traffic.idleMean = 10 + random.nextDouble() * 50;
            traffic.label = 1.0;
        } else {
            traffic.flowDuration = 10000 + random.nextDouble() * 90000;
            traffic.totalFwdPackets = random.nextDouble() * 100;
            traffic.totalBackwardPackets = random.nextDouble() * 50;
            traffic.fwdPacketLengthMax = random.nextDouble() * 200;
            traffic.fwdPacketLengthMin = random.nextDouble() * 50;
            traffic.fwdPacketLengthMean = random.nextDouble() * 100;
            traffic.bwdPacketLengthMax = random.nextDouble() * 200;
            traffic.bwdPacketLengthMin = random.nextDouble() * 50;
            traffic.bwdPacketLengthMean = random.nextDouble() * 100;
            traffic.flowBytesPerSec = random.nextDouble() * 100000;
            traffic.flowPacketsPerSec = random.nextDouble() * 1000;
            traffic.fwdIatTotal = 5000 + random.nextDouble() * 50000;
            traffic.fwdIatMean = 500 + random.nextDouble() * 5000;
            traffic.bwdIatTotal = 2000 + random.nextDouble() * 20000;
            traffic.bwdIatMean = 200 + random.nextDouble() * 2000;
            traffic.fwdPshFlags = random.nextInt(2);
            traffic.bwdPshFlags = random.nextInt(1);
            traffic.fwdUrgFlags = 0;
            traffic.bwdUrgFlags = 0;
            traffic.finFlagCount = random.nextInt(3);
            traffic.synFlagCount = random.nextInt(10);
            traffic.rstFlagCount = random.nextInt(3);
            traffic.pshFlagCount = random.nextInt(5);
            traffic.ackFlagCount = random.nextInt(50);
            traffic.urgFlagCount = 0;
            traffic.packetLengthMean = random.nextDouble() * 100;
            traffic.packetLengthStd = random.nextDouble() * 50;
            traffic.packetLengthVariance = random.nextDouble() * 2500;
            traffic.avgPacketSize = random.nextDouble() * 200;
            traffic.avgFwdSegmentSize = random.nextDouble() * 100;
            traffic.avgBwdSegmentSize = random.nextDouble() * 100;
            traffic.subflowFwdPackets = random.nextDouble() * 50;
            traffic.subflowFwdBytes = random.nextDouble() * 10000;
            traffic.subflowBwdPackets = random.nextDouble() * 30;
            traffic.subflowBwdBytes = random.nextDouble() * 5000;
            traffic.initWinBytesForward = random.nextDouble() * 5000;
            traffic.initWinBytesBackward = random.nextDouble() * 3000;
            traffic.activeMean = random.nextDouble() * 1000;
            traffic.idleMean = random.nextDouble() * 500;
            traffic.label = 0.0;
        }

        return traffic;
    }

    public static class NetworkTraffic {
        public String srcIp;
        public String dstIp;
        public int srcPort;
        public int dstPort;
        public String protocol;
        public String timestamp;
        public double flowDuration;
        public double totalFwdPackets;
        public double totalBackwardPackets;
        public double fwdPacketLengthMax;
        public double fwdPacketLengthMin;
        public double fwdPacketLengthMean;
        public double bwdPacketLengthMax;
        public double bwdPacketLengthMin;
        public double bwdPacketLengthMean;
        public double flowBytesPerSec;
        public double flowPacketsPerSec;
        public double fwdIatTotal;
        public double fwdIatMean;
        public double bwdIatTotal;
        public double bwdIatMean;
        public int fwdPshFlags;
        public int bwdPshFlags;
        public int fwdUrgFlags;
        public int bwdUrgFlags;
        public int finFlagCount;
        public int synFlagCount;
        public int rstFlagCount;
        public int pshFlagCount;
        public int ackFlagCount;
        public int urgFlagCount;
        public double packetLengthMean;
        public double packetLengthStd;
        public double packetLengthVariance;
        public double avgPacketSize;
        public double avgFwdSegmentSize;
        public double avgBwdSegmentSize;
        public double subflowFwdPackets;
        public double subflowFwdBytes;
        public double subflowBwdPackets;
        public double subflowBwdBytes;
        public double initWinBytesForward;
        public double initWinBytesBackward;
        public double activeMean;
        public double idleMean;
        public double label;
    }
}
