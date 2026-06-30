package com.ids.spark;

import java.io.Serializable;

public class MetricsRow implements Serializable {
    private long batch_id;
    private long total_samples;
    private long true_positives;
    private long true_negatives;
    private long false_positives;
    private long false_negatives;
    private double accuracy;
    private double precision;
    private double recall;
    private double f1_score;

    public MetricsRow() {}

    public MetricsRow(long batch_id, long total_samples, long tp, long tn, long fp, long fn,
                      double accuracy, double precision, double recall, double f1) {
        this.batch_id = batch_id;
        this.total_samples = total_samples;
        this.true_positives = tp;
        this.true_negatives = tn;
        this.false_positives = fp;
        this.false_negatives = fn;
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.f1_score = f1;
    }

    public long getBatch_id() { return batch_id; }
    public void setBatch_id(long v) { this.batch_id = v; }
    public long getTotal_samples() { return total_samples; }
    public void setTotal_samples(long v) { this.total_samples = v; }
    public long getTrue_positives() { return true_positives; }
    public void setTrue_positives(long v) { this.true_positives = v; }
    public long getTrue_negatives() { return true_negatives; }
    public void setTrue_negatives(long v) { this.true_negatives = v; }
    public long getFalse_positives() { return false_positives; }
    public void setFalse_positives(long v) { this.false_positives = v; }
    public long getFalse_negatives() { return false_negatives; }
    public void setFalse_negatives(long v) { this.false_negatives = v; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double v) { this.accuracy = v; }
    public double getPrecision() { return precision; }
    public void setPrecision(double v) { this.precision = v; }
    public double getRecall() { return recall; }
    public void setRecall(double v) { this.recall = v; }
    public double getF1_score() { return f1_score; }
    public void setF1_score(double v) { this.f1_score = v; }
}