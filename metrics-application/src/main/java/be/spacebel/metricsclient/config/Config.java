/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.config;

import be.spacebel.metricsclient.entities.Collection;
import be.spacebel.metricsclient.entities.MenuItem;
import be.spacebel.metricsclient.entities.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representation of application configurations
 *
 * @author mng
 */
public class Config {

    private String appName;
    //private String defaultStartDate;
    //private String defaultEndDate;
    private int dateRange;
    //private String osgwUrl;

    private String elasticsearchUrl;
    private String elasticsearchSearchIndex;
    private String elasticsearchOsddIndex;

    private int numOfTopCollections;
    private int numOfCollectionsPerRequest;

    private Map<String, Provider> providers;
    private Map<String, Collection> collections;
    private Map<String, String> realColMap;

    private String defaultCountryCode;
    private String defaultCountryName;

    private List<MenuItem> menuItems;

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    public Map<String, Collection> getCollections() {
        if (collections == null) {
            return new ConcurrentHashMap<>();
        }
        return collections;
    }

    public void setCollections(Map<String, Collection> collections) {
        this.collections = collections;
    }

    public Map<String, String> getRealColMap() {
        if (realColMap == null) {
            realColMap = new ConcurrentHashMap<>();
        }
        return realColMap;
    }

    public void setRealColMap(Map<String, String> realColMap) {
        this.realColMap = realColMap;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getDateRange() {
        return dateRange;
    }

    public void setDateRange(int dateRange) {
        this.dateRange = dateRange;
    }

//    public String getDefaultStartDate() {
//        return defaultStartDate;
//    }
//
//    public void setDefaultStartDate(String defaultStartDate) {
//        this.defaultStartDate = defaultStartDate;
//    }
//
//    public String getDefaultEndDate() {
//        return defaultEndDate;
//    }
//
//    public void setDefaultEndDate(String defaultEndDate) {
//        this.defaultEndDate = defaultEndDate;
//    }
//    public String getOsgwUrl() {
//        return osgwUrl;
//    }
//
//    public void setOsgwUrl(String osgwUrl) {
//        this.osgwUrl = osgwUrl;
//    }
    public String getElasticsearchUrl() {
        return elasticsearchUrl;
    }

    public void setElasticsearchUrl(String elasticsearchUrl) {
        this.elasticsearchUrl = elasticsearchUrl;
    }

    public String getElasticsearchSearchIndex() {
        return elasticsearchSearchIndex;
    }

    public void setElasticsearchSearchIndex(String elasticsearchSearchIndex) {
        this.elasticsearchSearchIndex = elasticsearchSearchIndex;
    }

    public String getElasticsearchOsddIndex() {
        return elasticsearchOsddIndex;
    }

    public void setElasticsearchOsddIndex(String elasticsearchOsddIndex) {
        this.elasticsearchOsddIndex = elasticsearchOsddIndex;
    }

    public int getNumOfTopCollections() {
        return numOfTopCollections;
    }

    public void setNumOfTopCollections(int numOfTopCollections) {
        this.numOfTopCollections = numOfTopCollections;
    }

    public int getNumOfCollectionsPerRequest() {
        return numOfCollectionsPerRequest;
    }

    public void setNumOfCollectionsPerRequest(int numOfCollectionsPerRequest) {
        this.numOfCollectionsPerRequest = numOfCollectionsPerRequest;
    }

    public String getDefaultCountryCode() {
        return defaultCountryCode;
    }

    public void setDefaultCountryCode(String defaultCountryCode) {
        this.defaultCountryCode = defaultCountryCode;
    }

    public String getDefaultCountryName() {
        return defaultCountryName;
    }

    public void setDefaultCountryName(String defaultCountryName) {
        this.defaultCountryName = defaultCountryName;
    }

    public void addProvider(Provider provider) {
        if (providers == null) {
            providers = new HashMap<>();
        }
        providers.putIfAbsent(provider.getName(), provider);
    }

    public void addCollection(Collection col) {
        if (collections == null) {
            collections = new HashMap<>();
        }
        collections.putIfAbsent(col.getId(), col);
    }

    public void putToRealColMap(String realColId, String colId) {
        if (realColMap == null) {
            realColMap = new ConcurrentHashMap<>();
        }
        realColMap.putIfAbsent(realColId, colId);
    }

    public Provider getProvider(String name) {
        if (providers != null
                && providers.containsKey(name)) {
            return providers.get(name);
        }
        return null;
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

}
