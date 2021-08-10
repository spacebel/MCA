/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

import be.spacebel.metricsclient.utils.Utils;
import java.util.Objects;

/**
 * This class represents a Search Request Metric record
 *
 * @author mng
 */
public class SearchMetric implements Comparable<SearchMetric> {

    private String collectionId;
    private String countryCode;
    private String provider;
    private long queries;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
        sb.append(Utils.encloseField(provider)).append(separator);
        sb.append(Utils.encloseField(Long.toString(queries))).append(separator);
        return sb.toString();
    }

    @Override
    public int compareTo(SearchMetric otherMetric) {
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
        if (!(obj instanceof SearchMetric)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        SearchMetric otherMetric = (SearchMetric) obj;
        return (this.getCollectionId().equalsIgnoreCase(otherMetric.getCollectionId())
                && this.countryCode.equalsIgnoreCase(otherMetric.getCountryCode()));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(getCollectionId() + getCountryCode());
        return hash;
    }
}
