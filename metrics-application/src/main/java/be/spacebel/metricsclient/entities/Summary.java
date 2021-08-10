/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

/**
 * This class represents data that is used to show on Summary table of metrics
 * 
 * @author mng
 */
public class Summary {

    private long totalVisits;
    private long failureVisits;
    private double averageTime;
    private double minTime;
    private double maxTime;

    public Summary() {
        this.totalVisits = 0;
        this.failureVisits = 0;
        this.minTime = 0;
        this.maxTime = 0;
    }

    public Summary(long totalVisits, long failureVisits, double averageTime, double minTime, double maxTime) {
        this.totalVisits = totalVisits;
        this.failureVisits = failureVisits;
        this.averageTime = averageTime;
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    public long getTotalVisits() {
        return totalVisits;
    }

    public void setTotalVisits(long totalVisits) {
        this.totalVisits = totalVisits;
    }

    public long getSuccessVisits() {
        return totalVisits - failureVisits;
    }

    public long getFailureVisits() {
        return failureVisits;
    }

    public void setFailureVisits(long failureVisits) {
        this.failureVisits = failureVisits;
    }

    public double getAverageTime() {
        return averageTime;
    }

    public String getAverageTimeStr() {
        return (String.format("%.4fs", averageTime));
    }

    public void setAverageTime(double averageTime) {
        this.averageTime = averageTime;
    }

    public double getMinTime() {
        return minTime;
    }

    public String getMinTimeStr() {
        return (String.format("%.4fs", minTime));
    }

    public void setMinTime(double minTime) {
        this.minTime = minTime;
    }

    public double getMaxTime() {
        return maxTime;
    }

    public String getMaxTimeStr() {
        return (String.format("%.4fs", maxTime));
    }

    public void setMaxTime(double maxTime) {
        this.maxTime = maxTime;
    }

}
