/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

import java.util.Objects;

/**
 * This class represents a collection data 
 * @author mng
 */
public class Collection implements Comparable<Collection> {

    private String id;
    private String realId;
    private String provider;
    private long visits;
    private long failedVisits;

    public Collection() {
    }

    public Collection(Collection newCol) {
        this.id = newCol.getId();
        this.provider = newCol.getProvider();
        this.realId = newCol.getRealId();
    }

    public Collection(String id, String provider) {
        this.id = id;
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getVisits() {
        return visits;
    }

    public void setVisits(long visits) {
        this.visits = visits;
    }

    public long getFailedVisits() {
        return failedVisits;
    }

    public void setFailedVisits(long failedVisits) {
        this.failedVisits = failedVisits;
    }

    public String getRealId() {
        return realId;
    }

    public void setRealId(String realId) {
        this.realId = realId;
    }

    @Override
    public int compareTo(Collection otherCol) {
        if (visits < otherCol.getVisits()) {
            return 1;
        }
        if (visits > otherCol.getVisits()) {
            return -1;
        }
        return id.compareTo(otherCol.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Collection)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.getId().equals(((Collection) obj).getId());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(getId());
        return hash;
    }
}
