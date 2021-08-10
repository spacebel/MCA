/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

import be.spacebel.metricsclient.utils.Utils;
import java.util.Objects;

/**
 * This class represents a OSDD Request Metric record
 * 
 * @author mng
 */
public class OsddMetric implements Comparable<OsddMetric> {

    private String collectionId;
    private String countryCode;
    private String clientId;
    private long queries;

    public OsddMetric(String collectionId, String countryCode, String clientId, long queries) {
        this.collectionId = collectionId;
        this.countryCode = countryCode;
        this.clientId = clientId;
        this.queries = queries;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getQueries() {
        return queries;
    }

    public void setQueries(long queries) {
        this.queries = queries;
    }

    public void addQueries(long moreQueries) {
        this.queries += moreQueries;
    }

    public String toLine(String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(Utils.encloseField(collectionId)).append(separator);
        sb.append(Utils.encloseField(countryCode)).append(separator);
        sb.append(Utils.encloseField(clientId)).append(separator);
        sb.append(Utils.encloseField(Long.toString(queries))).append(separator);
        return sb.toString();
    }

    @Override
    public int compareTo(OsddMetric otherMetric) {
        if (queries < otherMetric.getQueries()) {
            return 1;
        }
        if (queries > otherMetric.getQueries()) {
            return -1;
        }
        return collectionId.compareTo(otherMetric.getCollectionId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OsddMetric)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        OsddMetric otherMetric = (OsddMetric) obj;
        return (this.getCollectionId().equalsIgnoreCase(otherMetric.getCollectionId())
                && this.countryCode.equalsIgnoreCase(otherMetric.getCountryCode())
                && this.clientId.equalsIgnoreCase(otherMetric.getClientId()));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(getCollectionId() + getCountryCode() + getClientId());
        return hash;
    }
}
