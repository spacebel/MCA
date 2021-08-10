/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.web;

/**
 * This class represents request queries
 * @author mng
 */
public class Query {

    private String startDate;
    private String endDate;
    private String context;
    private String provider;
    private String collection;

    public Query(String startDate, String endDate, String context) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.context = context;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCollection() {
        return collection;
    }

    public String getShorterCollection() {
        if (collection != null && collection.length() > 60) {
            return collection.substring(0, 57) + "...";
        }
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

}
