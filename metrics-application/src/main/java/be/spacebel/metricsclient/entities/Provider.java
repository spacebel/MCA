/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a provider data
 * 
 * @author mng
 */
public class Provider implements Comparable<Provider> {

    private String name;
    private long visits;
    private long failedVisits;
    private List<Collection> collections;

    public Provider() {
        collections = new ArrayList<>();
    }

    public Provider(Provider newProvider, boolean cloneCollections) {
        this.name = newProvider.getName();
        if (cloneCollections && newProvider.getCollections() != null) {
            collections = new ArrayList<>(newProvider.getCollections().size());
            for (Collection col : newProvider.getCollections()) {
                collections.add(new Collection(col));
            }
        }else{
            collections = new ArrayList<>();
        }
    }

    public Provider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

    public void addCollection(Collection col) {
        if (collections == null) {
            collections = new ArrayList<>();
        }
        collections.add(col);
    }

    @Override
    public int compareTo(Provider otherProvider) {
        if (visits < otherProvider.getVisits()) {
            return 1;
        }
        if (visits > otherProvider.getVisits()) {
            return -1;
        }
        return name.compareTo(otherProvider.getName());
    }
}
