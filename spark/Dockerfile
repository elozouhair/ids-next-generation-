FROM apache/spark:3.5.3

USER root

COPY target/ids-spark-1.0.0.jar /opt/spark/app/job.jar
COPY src/main/resources/application.conf /opt/spark/app/application.conf

RUN mkdir -p /opt/spark/app/models

CMD ["/opt/spark/bin/spark-submit", \
     "--class", "com.ids.spark.IdsSparkJob", \
     "--master", "spark://spark-master:7077", \
     "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.13:3.5.0,org.postgresql:postgresql:42.7.1", \
     "/opt/spark/app/job.jar"]
