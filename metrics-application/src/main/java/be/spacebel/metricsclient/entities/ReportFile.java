/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

import java.util.Objects;

/**
 * This class represents information of a report file
 * 
 * @author mng
 */
public class ReportFile {

    private final String name;
    private final String header;
    private final String footer;
    private final long startTime;
    private final long endTime;

    public ReportFile(String name, String header, String footer, long startTime, long endTime) {
        this.name = name;
        this.header = header;
        this.footer = footer;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public String getHeader() {
        return header;
    }

    public String getFooter() {
        return footer;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ReportFile)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        ReportFile otherReport = (ReportFile) obj;
        return (this.getName().equals(otherReport.getName()));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(getName());
        return hash;
    }

}
