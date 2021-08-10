/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

/**
 * This class represents data of "group by" query
 * 
 * @author mng
 */
public class GroupByData implements Comparable<GroupByData> {

    private String name;
    private long osddVisits;
    private long searchVisits;
    private boolean osdd;

    public GroupByData() {
    }

    public GroupByData(String name, long osddVisits, long searchVisits, boolean osdd) {
        this.name = name;
        this.osddVisits = osddVisits;
        this.searchVisits = searchVisits;
        this.osdd = osdd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getOsddVisits() {
        return osddVisits;
    }

    public void setOsddVisits(long osddVisits) {
        this.osddVisits = osddVisits;
    }

    public long getSearchVisits() {
        return searchVisits;
    }

    public void setSearchVisits(long searchVisits) {
        this.searchVisits = searchVisits;
    }

    public void addSearchVisits(long visits) {
        this.searchVisits += visits;
    }

    public void addOsddVisits(long visits) {
        this.osddVisits += visits;
    }

    public long getTotalVisits() {
        return searchVisits + osddVisits;
    }

    @Override
    public int compareTo(GroupByData otherData) {

        long totalVisits = osddVisits;
        long otherVisits = otherData.getOsddVisits();

        if (!osdd) {
            totalVisits += searchVisits;
            otherVisits += otherData.getSearchVisits();
        }

        if (totalVisits < otherVisits) {
            return 1;
        }
        if (totalVisits > otherVisits) {
            return -1;
        }

        return name.compareTo(otherData.getName());
    }

}
