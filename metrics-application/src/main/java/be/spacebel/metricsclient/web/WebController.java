package be.spacebel.metricsclient.web;

import be.spacebel.metricsclient.config.Config;
import be.spacebel.metricsclient.entities.Collection;
import be.spacebel.metricsclient.entities.GroupByData;
import be.spacebel.metricsclient.entities.Summary;
import be.spacebel.metricsclient.entities.Provider;
import be.spacebel.metricsclient.utils.ElasticsearchUtils;
import be.spacebel.metricsclient.utils.Utils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.missing.ParsedMissing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A web controller handles all HTTP requests of Metrics Client
 *
 * @author mng
 */
@Controller
public class WebController {

    private final static String HOME_CONTEXT = "H";
    private final static String PROVIDER_CONTEXT = "P";
    private final static String COLLECTION_CONTEXT = "C";

    private static final Logger LOG = LoggerFactory.getLogger(WebController.class);

    private final Config config;

    @Autowired
    public WebController(Config config) {
        this.config = config;
    }

    @RequestMapping("/")
    public String mainPage(Model model,
            @RequestParam(value = "startdate", required = false) String start,
            @RequestParam(value = "enddate", required = false) String end)
            throws IOException, ParseException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (start == null) {
            start = Utils.defaultStartDate(config.getDateRange());            
        }
        if (end == null) {
            end = Utils.defaultEndDate();
        }
        return buildMainPage(new Query(start, end, HOME_CONTEXT), model);
        //return handleQuery(new Query(start, end, HOME_CONTEXT), model);
    }

    @PostMapping("/")
    public String onChangeTimeRange(Model model, Query query)
            throws IOException, ParseException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return buildMainPage(query, model);
    }

    @PostMapping("/provider/{name:.+}")
    public String onChangeTimeRange(@PathVariable("name") String provider,
            Model model, Query query)
            throws IOException, ParseException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        LOG.debug(" Submit form in Provider context ");
        LOG.debug(" Provider name " + provider);
        LOG.debug(" Start date " + query.getStartDate());
        LOG.debug(" End date " + query.getEndDate());
        return buildProviderPage(query, model);
        //return handleQuery(query, model);
    }

    @PostMapping("/collection/{id:.+}")
    public String onChangeCollectionTimeRange(
            @PathVariable("id") String collectionId,
            Model model, Query query)
            throws IOException, ParseException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        LOG.debug(" Submit form in Collection context ");
        LOG.debug(" Collection " + collectionId);
        LOG.debug(" Start date " + query.getStartDate());
        LOG.debug(" End date " + query.getEndDate());

        //return handleQuery(query, model);
        return buildCollectionPage(query, model);
    }

    @GetMapping("/provider/{name:.+}")
    public String onSelectProvider(@PathVariable("name") String name,
            @RequestParam(value = "startdate", required = false) String start,
            @RequestParam(value = "enddate", required = false) String end,
            Model model)
            throws ParseException, IOException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        LOG.debug(" Provider name " + name);
        LOG.debug(" Start date " + start);
        LOG.debug(" End date " + end);
        if (start == null) {
            start = Utils.defaultStartDate(config.getDateRange());
        }
        if (end == null) {
            end = Utils.defaultEndDate();
        }
        Query newQuery = new Query(start, end, PROVIDER_CONTEXT);
        newQuery.setProvider(name);
        return buildProviderPage(newQuery, model);
    }

    @GetMapping("/collection")
    public String onSelectCollection1(@RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "startdate", required = false) String start,
            @RequestParam(value = "enddate", required = false) String end,
            Model model)
            throws IOException, ParseException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (start == null) {
            start = Utils.defaultStartDate(config.getDateRange());
        }
        if (end == null) {
            end = Utils.defaultEndDate();;
        }

        Query newQuery = new Query(start, end, COLLECTION_CONTEXT);
        newQuery.setCollection(id);
        return buildCollectionPage(newQuery, model);
    }

    @GetMapping("/collection/{id:.+}")
    public String onSelectCollection(@PathVariable("id") String collectionId,
            @RequestParam(value = "startdate", required = false) String start,
            @RequestParam(value = "enddate", required = false) String end,
            Model model)
            throws IOException, ParseException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        LOG.debug(" Collection " + collectionId);
        LOG.debug(" Start date " + start);
        LOG.debug(" End date " + end);

        if (start == null) {
            start = Utils.defaultStartDate(config.getDateRange());
        }
        if (end == null) {
            end = Utils.defaultEndDate();;
        }

        Query newQuery = new Query(start, end, COLLECTION_CONTEXT);
        newQuery.setCollection(collectionId);

        return buildCollectionPage(newQuery, model);

        //return handleQuery(newQuery, model);
    }

    private String toView(Model model, Query query,
            Summary searchReqSum, Summary osddReqSum, List<Provider> providers,
            List<Collection> collections) {
        model.addAttribute("menuItems", config.getMenuItems());
        model.addAttribute("formQuery", query);
        model.addAttribute("appTitle", config.getAppName());

        if (searchReqSum != null) {
            model.addAttribute("searchReqSum", searchReqSum);
        }

        if (osddReqSum != null) {
            model.addAttribute("osddReqSum", osddReqSum);
        }

        if (providers != null) {
            model.addAttribute("providers", providers);
        }

        if (collections != null) {
            model.addAttribute("collections", collections);
        }

        return "home";
    }

    private String buildMainPage(Query query, Model model)
            throws IOException, ParseException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        RestHighLevelClient client = ElasticsearchUtils.buildRestClient(config.getElasticsearchUrl());

        long startTime = Utils.startDateInSeconds(query.getStartDate());
        long endTime = Utils.endDateInSeconds(query.getEndDate());

        /**
         * Search on opensearch-request index
         */
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //boolQueryBuilder.must(QueryBuilders.termQuery("code", 200));
        boolQueryBuilder.filter(
                QueryBuilders.rangeQuery("timestamp")
                        .gt(startTime)
                        .lte(endTime));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        // group by collection
        TermsAggregationBuilder aggregation = AggregationBuilders
                .terms("collections")
                .field("parentIdentifier.keyword")
                .size(10000); // set aggregation to maximum number (10000) to get all aggregations

        // group by code to get number of success/failure requests per collection
        aggregation.subAggregation(AggregationBuilders
                .terms("codes")
                .field("code"));

        // group by country name to get number of requests per country per collection
        aggregation.subAggregation(AggregationBuilders
                .terms("countries")
                .field("country_name.keyword"));

        aggregation.subAggregation(AggregationBuilders
                .missing("missing_country")
                .field("country_name.keyword"));

        searchSourceBuilder.aggregation(aggregation);
        searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

        LOG.debug("Get logs in a period " + searchSourceBuilder.toString());

        SearchRequest searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        LOG.debug(searchResponse.toString());

        Aggregations aggregations = searchResponse.getAggregations();
        PriorityQueue<Collection> colPriorityQueue = new PriorityQueue<>();
        Map<String, Long> providerMap = new HashMap<>();
        Map<String, Collection> collections = new HashMap<>();
        Map<String, GroupByData> queriesPerCountry = new HashMap<>();

        Summary searchRequestSum = new Summary();
        parseResults(aggregations, collections, providerMap, searchRequestSum, queriesPerCountry, false);

        /**
         * Search on opensearch-osdd index
         */
        searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();

        Summary osddRequestSum = new Summary();
        parseResults(aggregations, collections, providerMap, osddRequestSum, queriesPerCountry, true);

        /**
         * get search request max/min/avg time
         */
        searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.aggregation(AggregationBuilders
                .max("max_time").field("request_time"));
        searchSourceBuilder.aggregation(AggregationBuilders
                .min("min_time").field("request_time"));
        searchSourceBuilder.aggregation(AggregationBuilders
                .avg("avg_time").field("request_time"));
        searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

        // get on opensearch-request index
        searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();
        getTimeInfo(aggregations, searchRequestSum);

        // get on opensearch-osdd index
        searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();
        getTimeInfo(aggregations, osddRequestSum);

        List<Provider> providers = null;
        if (config.getProviders() != null) {
            // use PriorityQueue to sort providers by number of visits
            PriorityQueue<Provider> providerPriorityQueue = new PriorityQueue<>(config.getProviders().size());
            for (Map.Entry<String, Provider> p : config.getProviders().entrySet()) {
                Provider newProvider = new Provider(p.getValue(), false);
                if (providerMap.containsKey(newProvider.getName())) {
                    newProvider.setVisits(providerMap.get(newProvider.getName()));
                } else {
                    long visits = 0;
                    newProvider.setVisits(visits);
                }
                providerPriorityQueue.add(newProvider);
            }
            if (providerMap.containsKey(ElasticsearchUtils.UNKNOWN_PROVIDER)) {
                Provider newProvider = new Provider(ElasticsearchUtils.UNKNOWN_PROVIDER);
                newProvider.setVisits(providerMap.get(ElasticsearchUtils.UNKNOWN_PROVIDER));
                providerPriorityQueue.add(newProvider);
            }

            providers = new ArrayList<>();
            while (providerPriorityQueue.size() > 0) {
                providers.add(providerPriorityQueue.poll());
            }
        }

        List<Collection> topCols = new ArrayList<>();
        colPriorityQueue.addAll(collections.values());

        int count = 0;
        while (count < config.getNumOfTopCollections()
                && colPriorityQueue.size() > 0) {
            topCols.add(colPriorityQueue.poll());
            count++;
        }

        List<GroupByData> countries = sortData(queriesPerCountry, false);
        model.addAttribute("countries", countries);

        return toView(model, query, searchRequestSum, osddRequestSum, providers, topCols);
    }

    private String buildProviderPage(Query query, Model model)
            throws IOException, ParseException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        RestHighLevelClient client = ElasticsearchUtils.buildRestClient(config.getElasticsearchUrl());

        long startTime = Utils.startDateInSeconds(query.getStartDate());
        long endTime = Utils.endDateInSeconds(query.getEndDate());

        /**
         * Search on opensearch-request index
         */
        Provider provider = config.getProvider(query.getProvider());
        if (provider != null) {
            if (provider.getCollections() != null
                    && provider.getCollections().size() > 0) {
                Map<String, Collection> collectionMap = new HashMap<>();
                List<String> colIds = new ArrayList<>();
                for (Collection col : provider.getCollections()) {
                    collectionMap.put(col.getId(), new Collection(col));
                    colIds.add(col.getId());
                    // add real collectionId
                    if (StringUtils.isNotEmpty(col.getRealId())) {
                        colIds.add(col.getRealId());
                    }
                }

                Summary searchRequestSum = new Summary();
                Summary osddRequestSum = new Summary();
                Map<String, GroupByData> queriesPerCountry = new HashMap<>();

                double totalSearchTimes = 0;
                double totalOsddTimes = 0;

                int fromIndex = 0;
                int toIndex = 0;
                boolean keepGoing = true;
                while (keepGoing) {
                    toIndex += config.getNumOfCollectionsPerRequest();
                    if (toIndex > colIds.size()) {
                        toIndex = colIds.size();
                        keepGoing = false;
                    }

                    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                    boolQueryBuilder.must(QueryBuilders
                            .termsQuery("parentIdentifier.keyword",
                                    colIds.subList(fromIndex, toIndex)));
                    boolQueryBuilder.filter(
                            QueryBuilders.rangeQuery("timestamp")
                                    .gt(startTime)
                                    .lte(endTime));
                    // increase from index
                    fromIndex = toIndex + 1;

                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.timeout(new TimeValue(180, TimeUnit.SECONDS));
                    searchSourceBuilder.query(boolQueryBuilder);

                    // group by collection
                    TermsAggregationBuilder aggregation = AggregationBuilders
                            .terms("collections")
                            .field("parentIdentifier.keyword")
                            .size(10000); // set aggregation to maximum number (10000) to get all aggregations

                    // group by code to get number of success/failure requests per collection
                    aggregation.subAggregation(AggregationBuilders
                            .terms("codes")
                            .field("code"));

                    // group by country name to get number of requests per country per collection
                    aggregation.subAggregation(AggregationBuilders
                            .terms("countries")
                            .field("country_name.keyword"));

                    aggregation.subAggregation(AggregationBuilders
                            .missing("missing_country")
                            .field("country_name.keyword"));

                    searchSourceBuilder.aggregation(aggregation);
                    searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

                    LOG.debug("Get logs by provider in a period " + searchSourceBuilder.toString());
                    /**
                     * Search on opensearch-request index
                     */
                    SearchRequest searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());

                    searchRequest.source(searchSourceBuilder);
                    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                    //System.out.println("MNG:" + searchResponse.toString());
                    Aggregations aggregations = searchResponse.getAggregations();
                    long numOfSearchReqs = parseProviderResults(aggregations, collectionMap, searchRequestSum, queriesPerCountry, false);

                    /**
                     * Search on opensearch-osdd index
                     */
                    searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
                    searchRequest.source(searchSourceBuilder);
                    searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                    aggregations = searchResponse.getAggregations();
                    long numOfOsddReqs = parseProviderResults(aggregations, collectionMap, osddRequestSum, queriesPerCountry, true);

                    /**
                     * get search request max/min/avg time
                     */
                    searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.query(boolQueryBuilder);
                    searchSourceBuilder.aggregation(AggregationBuilders
                            .max("max_time").field("request_time"));
                    searchSourceBuilder.aggregation(AggregationBuilders
                            .min("min_time").field("request_time"));
                    searchSourceBuilder.aggregation(AggregationBuilders
                            .avg("avg_time").field("request_time"));
                    searchSourceBuilder.size(1);// set hits to 1 because we don't use hits
                    LOG.debug("Get aggregations by provider in a period " + searchSourceBuilder.toString());

                    searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
                    searchRequest.source(searchSourceBuilder);
                    searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                    aggregations = searchResponse.getAggregations();

                    if (aggregations != null) {
                        Max maxTime = aggregations.get("max_time");
                        if (maxTime != null) {
                            double newValue = Utils.validateDouble(maxTime.getValue());
                            if (searchRequestSum.getMaxTime() == 0) {
                                searchRequestSum.setMaxTime(newValue);
                            } else {
                                if (newValue > searchRequestSum.getMaxTime()) {
                                    searchRequestSum.setMaxTime(newValue);
                                }
                            }
                        }
                        Min minTime = aggregations.get("min_time");
                        if (minTime != null) {
                            double newValue = Utils.validateDouble(minTime.getValue());
                            if (searchRequestSum.getMinTime() == 0) {
                                searchRequestSum.setMinTime(newValue);
                            } else {
                                if (newValue < searchRequestSum.getMinTime()) {
                                    searchRequestSum.setMinTime(newValue);
                                }
                            }
                        }
                        Avg avgTime = aggregations.get("avg_time");
                        if (avgTime != null) {
                            double avgValue = Utils.validateDouble(avgTime.getValue());
                            if (avgValue > 0) {
                                totalSearchTimes += numOfSearchReqs * avgValue;
                            }
                        }
                    }

                    /**
                     * get OSDD request max/min/avg time
                     */
                    searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());

                    searchRequest.source(searchSourceBuilder);
                    searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                    aggregations = searchResponse.getAggregations();

                    if (aggregations != null) {
                        Max maxTime = aggregations.get("max_time");
                        if (maxTime != null) {
                            double newValue = Utils.validateDouble(maxTime.getValue());
                            if (osddRequestSum.getMaxTime() == 0) {
                                osddRequestSum.setMaxTime(newValue);
                            } else {
                                if (newValue > osddRequestSum.getMaxTime()) {
                                    osddRequestSum.setMaxTime(newValue);
                                }
                            }
                        }
                        Min minTime = aggregations.get("min_time");
                        if (minTime != null) {
                            double newValue = Utils.validateDouble(minTime.getValue());
                            if (osddRequestSum.getMaxTime() == 0) {
                                osddRequestSum.setMinTime(newValue);
                            } else {
                                if (newValue < osddRequestSum.getMaxTime()) {
                                    osddRequestSum.setMinTime(newValue);
                                }
                            }
                        }
                        Avg avgTime = aggregations.get("avg_time");
                        if (avgTime != null) {
                            double avgValue = Utils.validateDouble(avgTime.getValue());
                            if (avgValue > 0) {
                                totalOsddTimes += numOfOsddReqs * avgValue;
                            }

                        }
                    }
                }

                // calculate avg of search request time
                if (searchRequestSum.getTotalVisits() > 0) {
                    searchRequestSum.setAverageTime(totalSearchTimes / searchRequestSum.getTotalVisits());
                }
                // calculate avg of osdd request time
                if (osddRequestSum.getTotalVisits() > 0) {
                    osddRequestSum.setAverageTime(totalOsddTimes / osddRequestSum.getTotalVisits());
                }

                // use PriorityQueue to sort
                PriorityQueue<Collection> colPriorityQueue = new PriorityQueue<>(collectionMap.size());
                colPriorityQueue.addAll(collectionMap.values());
                List<Collection> colList = new ArrayList<>();
                while (colPriorityQueue.size() > 0) {
                    colList.add(colPriorityQueue.poll());
                }

                List<GroupByData> countries = sortData(queriesPerCountry, false);
                model.addAttribute("countries", countries);

                return toView(model, query, searchRequestSum, osddRequestSum, null, colList);
            } else {
                //throw new IOException(String.format("The provider %s has no collection", query.getProvider()));
                return toView(model, query, new Summary(), new Summary(), null, new ArrayList<>());
            }
        } else {
            throw new IOException(String.format("The provider %s doesn't exist", query.getProvider()));
        }
    }

    private String buildCollectionPage(Query query, Model model)
            throws IOException, ParseException, MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Collection col = config.getCollections().get(query.getCollection());
//        if (col == null) {
//            //throw new IOException("The collection " + query.getCollection() + " does not exist");
//            //query.setProvider(UNKNOW_PROVIDER);
//        }

        if (col != null) {
            query.setProvider(col.getProvider());
        }

        RestHighLevelClient client = ElasticsearchUtils.buildRestClient(config.getElasticsearchUrl());

        long startTime = Utils.startDateInSeconds(query.getStartDate());
        long endTime = Utils.endDateInSeconds(query.getEndDate());
        List<String> colIds = new ArrayList<>();
        colIds.add(query.getCollection());
        /*
            add real collection of the given collection
         */
        if (col != null
                && StringUtils.isNotEmpty(col.getRealId())) {
            colIds.add(col.getRealId());
        }

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (colIds.size() == 1) {
            boolQueryBuilder.must(QueryBuilders
                    .termQuery("parentIdentifier.keyword", colIds.get(0)));
        } else {
            boolQueryBuilder.must(QueryBuilders
                    .termsQuery("parentIdentifier.keyword", colIds));
        }
        boolQueryBuilder.filter(
                QueryBuilders.rangeQuery("timestamp")
                        .gt(startTime)
                        .lte(endTime));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //searchSourceBuilder.timeout(new TimeValue(180, TimeUnit.SECONDS));
        searchSourceBuilder.query(boolQueryBuilder);

        // group by collection
        TermsAggregationBuilder aggregation = AggregationBuilders
                .terms("collections")
                .field("parentIdentifier.keyword")
                .size(10000); // set aggregation to maximum number (10000) to get all aggregations

        // group by code to get number of success/failure requests per collection
        aggregation.subAggregation(AggregationBuilders
                .terms("codes")
                .field("code"));

        // group by country name to get number of requests per country per collection
        aggregation.subAggregation(AggregationBuilders
                .terms("countries")
                .field("country_name.keyword"));

        aggregation.subAggregation(AggregationBuilders
                .missing("missing_country")
                .field("country_name.keyword"));

        // group by source to get number of requests per client ID per collection
        aggregation.subAggregation(AggregationBuilders
                .terms("clientids")
                .field("source.keyword"));

        searchSourceBuilder.aggregation(aggregation);
        searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

        LOG.debug("Get logs by collection in a period " + searchSourceBuilder.toString());

        Summary searchRequestSum = new Summary();
        Summary osddRequestSum = new Summary();
        Map<String, GroupByData> queriesPerCountry = new HashMap<>();
        Map<String, GroupByData> queriesPerClientId = new HashMap<>();

        /**
         * Search on opensearch-request index
         */
        SearchRequest searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Aggregations aggregations = searchResponse.getAggregations();
        parseCollectionResults(aggregations, searchRequestSum, queriesPerCountry, queriesPerClientId, false);

        LOG.debug(searchResponse.toString());
        /**
         * Search on opensearch-osdd index
         */
        searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();
        parseCollectionResults(aggregations, osddRequestSum, queriesPerCountry, queriesPerClientId, true);

        LOG.debug(searchResponse.toString());

        /**
         * get search request max/min/avg time
         */
        searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.aggregation(AggregationBuilders
                .max("max_time").field("request_time"));
        searchSourceBuilder.aggregation(AggregationBuilders
                .min("min_time").field("request_time"));
        searchSourceBuilder.aggregation(AggregationBuilders
                .avg("avg_time").field("request_time"));
        searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

        // get on opensearch-request index
        searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();
        getTimeInfo(aggregations, searchRequestSum);

        // get on opensearch-osdd index
        searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();
        getTimeInfo(aggregations, osddRequestSum);

        /*
            prepare data for chart
         */
        searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        // group by timestamp"
        aggregation = AggregationBuilders
                .terms("dates")
                .field("timestamp")
                .size(10000); // set aggregation to maximum number (10000) to get all aggregations        
        searchSourceBuilder.aggregation(aggregation);
        searchSourceBuilder.size(1);// set hits to 1 because we don't use hits
        LOG.debug("Query to get data for the chart " + searchSourceBuilder.toString());

        // get on opensearch-request index
        searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();

        Map<String, Long> data = buildChartData(aggregations);
        List<Long> searchRequestVisits = new ArrayList<>(data.values());
        List<String> searchRequestDates = new ArrayList<>(data.keySet());
        Collections.sort(searchRequestVisits);
        Collections.sort(searchRequestDates);
        model.addAttribute("searchRequestVisits", searchRequestVisits);
        model.addAttribute("searchRequestDates", searchRequestDates);

        // get on opensearch-osdd index
        searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
        searchRequest.source(searchSourceBuilder);
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        aggregations = searchResponse.getAggregations();

        data = buildChartData(aggregations);
        List<Long> osddRequestVisits = new ArrayList<>(data.values());
        List<String> osddRequestDates = new ArrayList<>(data.keySet());
        Collections.sort(osddRequestVisits);
        Collections.sort(osddRequestDates);
        model.addAttribute("osddRequestVisits", osddRequestVisits);
        model.addAttribute("osddRequestDates", osddRequestDates);

        List<GroupByData> countries = sortData(queriesPerCountry, false);
        List<GroupByData> clientIds = sortData(queriesPerClientId, true);

        model.addAttribute("countries", countries);
        model.addAttribute("clientIds", clientIds);

        return toView(model, query, searchRequestSum, osddRequestSum, null, null);
    }

    private void parseResults(Aggregations aggregations,
            Map<String, Collection> collections,
            Map<String, Long> providerMap, Summary requestSum,
            Map<String, GroupByData> queriesPerCountry, boolean osdd) {

        Map<String, Collection> colsMap = config.getCollections();
        if (aggregations != null) {
            Terms colTerms = aggregations.get("collections");
            if (colTerms != null
                    && colTerms.getBuckets() != null
                    && colTerms.getBuckets().size() > 0) {
                //System.out.println("Number of Buckets: " + colTerms.getBuckets().size());
                for (Terms.Bucket bucket : colTerms.getBuckets()) {
                    String colId = (String) bucket.getKey();
                    long visits = bucket.getDocCount();
                    Collection newCol;
                    if (StringUtils.isNotEmpty(colId)) {
                        if (colsMap.containsKey(colId)) {
                            newCol = new Collection(colsMap.get(colId));
                        } else {
                            if (config.getRealColMap().containsKey(colId)) {
                                newCol = new Collection(colsMap.get(config.getRealColMap().get(colId)));
                            } else {
                                newCol = new Collection();
                                newCol.setId(colId);
                                newCol.setProvider(ElasticsearchUtils.UNKNOWN_PROVIDER);
                                LOG.debug("Collection of unknow provider" + colId);
                            }
                        }
                    } else {
                        newCol = new Collection();
                        newCol.setId(ElasticsearchUtils.NON_COLLECTION);
                        newCol.setProvider(ElasticsearchUtils.UNKNOWN_PROVIDER);
                        LOG.debug("Visits of Unknow collection: " + visits);
                    }

                    long failedVisitsPerCol = 0;
                    Terms codeTerms = bucket.getAggregations().get("codes");
                    if (codeTerms != null
                            && codeTerms.getBuckets() != null
                            && codeTerms.getBuckets().size() > 0) {
                        for (Terms.Bucket codeBucket : codeTerms.getBuckets()) {
                            if (200 != (Long) codeBucket.getKey()) {
                                LOG.debug(String.format("Collection %s has %s failure requests with code %s",
                                        colId, codeBucket.getDocCount(), codeBucket.getKey()));
                                failedVisitsPerCol += codeBucket.getDocCount();
                            }
                        }
                    }

                    requestSum.setFailureVisits(requestSum.getFailureVisits() + failedVisitsPerCol);
                    requestSum.setTotalVisits(requestSum.getTotalVisits() + visits);

                    newCol.setVisits(visits);
                    if (collections.containsKey(colId)) {
                        newCol = collections.get(colId);
                        newCol.setVisits(newCol.getVisits() + visits);
                    } else {
                        collections.put(colId, newCol);
                    }

                    if (providerMap.containsKey(newCol.getProvider())) {
                        long newVisists = visits + providerMap.get(newCol.getProvider());
                        providerMap.put(newCol.getProvider(), newVisists);
                    } else {
                        providerMap.put(newCol.getProvider(), visits);
                    }

                    // get queries that were requested by unknown countries
                    getUnknownCountryVisits(bucket, queriesPerCountry, osdd);

                    // get queries per countries 
                    getGroupByData(bucket, "countries", queriesPerCountry, osdd);
                }
            } else {
                LOG.debug("No result");
            }
        } else {
            LOG.debug("No result");
        }
    }

    private long parseProviderResults(Aggregations aggregations,
            Map<String, Collection> collections, Summary requestSum,
            Map<String, GroupByData> queriesPerCountry, boolean osdd) {
        long numberOfRequests = 0;
        if (aggregations != null) {
            Terms colTerms = aggregations.get("collections");
            if (colTerms != null
                    && colTerms.getBuckets() != null
                    && colTerms.getBuckets().size() > 0) {
                for (Terms.Bucket bucket : colTerms.getBuckets()) {
                    String realId = (String) bucket.getKey();
                    long visits = bucket.getDocCount();
                    String colId = null;
                    if (collections.containsKey(realId)) {
                        colId = realId;
                    } else {
                        if (config.getRealColMap().containsKey(realId)) {
                            colId = config.getRealColMap().get(realId);
                        } else {
                            LOG.debug("Unknow collection " + realId);
                        }
                    }

                    if (StringUtils.isNotEmpty(colId)
                            && collections.containsKey(colId)) {
                        Collection col = collections.get(colId);
                        long failedVisitsPerCol = 0;
                        Terms codeTerms = bucket.getAggregations().get("codes");
                        if (codeTerms != null
                                && codeTerms.getBuckets() != null
                                && codeTerms.getBuckets().size() > 0) {
                            for (Terms.Bucket codeBucket : codeTerms.getBuckets()) {
                                if (200 != (Long) codeBucket.getKey()) {
                                    LOG.debug(String.format("Collection %s has %s failure requests with code %s",
                                            colId, codeBucket.getDocCount(), codeBucket.getKey()));
                                    failedVisitsPerCol += codeBucket.getDocCount();
                                }
                            }
                        }
                        col.setVisits(col.getVisits() + visits);
                        col.setFailedVisits(col.getFailedVisits() + failedVisitsPerCol);

                        requestSum.setFailureVisits(requestSum.getFailureVisits() + failedVisitsPerCol);
                        requestSum.setTotalVisits(requestSum.getTotalVisits() + visits);
                        numberOfRequests += visits;
                    }

                    // get queries that were requested by unknown countries
                    getUnknownCountryVisits(bucket, queriesPerCountry, osdd);

                    // get queries per countries 
                    getGroupByData(bucket, "countries", queriesPerCountry, osdd);
                }
            } else {
                LOG.debug("No result");
            }
        } else {
            LOG.debug("No result");
        }
        return numberOfRequests;
    }

    private void parseCollectionResults(Aggregations aggregations, Summary requestSum,
            Map<String, GroupByData> queriesPerCountry, Map<String, GroupByData> queriesPerClientId, boolean osdd) {
        if (aggregations != null) {
            Terms colTerms = aggregations.get("collections");
            if (colTerms != null
                    && colTerms.getBuckets() != null
                    && colTerms.getBuckets().size() > 0) {
                for (Terms.Bucket bucket : colTerms.getBuckets()) {
                    requestSum.setTotalVisits(requestSum.getTotalVisits() + bucket.getDocCount());
                    long failedVisits = 0;
                    Terms codeTerms = bucket.getAggregations().get("codes");
                    if (codeTerms != null
                            && codeTerms.getBuckets() != null
                            && codeTerms.getBuckets().size() > 0) {
                        for (Terms.Bucket codeBucket : codeTerms.getBuckets()) {
                            if (200 != (Long) codeBucket.getKey()) {
                                failedVisits += codeBucket.getDocCount();
                            }
                        }
                    }
                    requestSum.setFailureVisits(requestSum.getFailureVisits() + failedVisits);

                    // get queries that were requested by unknown countries
                    getUnknownCountryVisits(bucket, queriesPerCountry, osdd);

                    // get queries per countries 
                    getGroupByData(bucket, "countries", queriesPerCountry, osdd);

                    getGroupByData(bucket, "clientids", queriesPerClientId, osdd);
                }
            }
        }
    }

    private void getGroupByData(Terms.Bucket bucket,
            String groupName, Map<String, GroupByData> groupData, boolean osdd) {
        Terms terms = bucket.getAggregations().get(groupName);
        if (terms != null
                && terms.getBuckets() != null
                && terms.getBuckets().size() > 0) {
            for (Terms.Bucket subBucket : terms.getBuckets()) {
                String key = subBucket.getKeyAsString();
                if (key == null || key.isEmpty() || key.equalsIgnoreCase("null")) {
                    if (osdd) {
                        // Client ID
                        key = ElasticsearchUtils.UNKNOWN;
                    } else {
                        // Country
                        key = config.getDefaultCountryName();
                    }
                }

                if (groupData.containsKey(key)) {
                    GroupByData existingData = groupData.get(key);
                    if (osdd) {
                        existingData.addOsddVisits(subBucket.getDocCount());
                    } else {
                        existingData.addSearchVisits(subBucket.getDocCount());
                    }
                } else {
                    if (osdd) {
                        groupData.put(key, new GroupByData(key, subBucket.getDocCount(), 0, osdd));
                    } else {
                        groupData.put(key, new GroupByData(key, 0, subBucket.getDocCount(), osdd));
                    }

                }
            }
        }
    }

    private List<GroupByData> sortData(Map<String, GroupByData> data, boolean verifyOsddVisists) {
        List<GroupByData> sortedData = new ArrayList<>();

        if (data != null && !data.isEmpty()) {
            PriorityQueue<GroupByData> priorityQueue = new PriorityQueue<>(data.size());
            priorityQueue.addAll(data.values());

            while (priorityQueue.size() > 0) {
                GroupByData gbData = priorityQueue.poll();
                if (verifyOsddVisists) {
                    if (gbData.getOsddVisits() > 0) {
                        sortedData.add(gbData);
                    }
                } else {
                    sortedData.add(gbData);
                }
            }
        }

        return sortedData;
    }

    private void getTimeInfo(Aggregations aggregations, Summary requestSum) {
        if (aggregations != null) {
            Max maxTime = aggregations.get("max_time");
            if (maxTime != null) {
                requestSum.setMaxTime(Utils.validateDouble(maxTime.getValue()));
            }
            Min minTime = aggregations.get("min_time");
            if (minTime != null) {
                requestSum.setMinTime(Utils.validateDouble(minTime.getValue()));
            }
            Avg avgTime = aggregations.get("avg_time");
            if (avgTime != null) {
                requestSum.setAverageTime(Utils.validateDouble(avgTime.getValue()));
            }
        }
    }

    private Map<String, Long> buildChartData(Aggregations aggregations) {
        Map<String, Long> data = new HashMap<>();
        if (aggregations != null) {
            Terms dateTerms = aggregations.get("dates");
            if (dateTerms != null
                    && dateTerms.getBuckets() != null
                    && dateTerms.getBuckets().size() > 0) {
                for (Terms.Bucket bucket : dateTerms.getBuckets()) {
                    if (bucket.getKeyAsNumber() != null) {
                        String date = Utils.formatDate(bucket.getKeyAsNumber().longValue());
                        if (data.containsKey(date)) {
                            long newVisits = data.get(date) + bucket.getDocCount();
                            data.put(date, newVisits);
                        } else {
                            data.put(date, bucket.getDocCount());
                        }
                    }
                    LOG.debug(bucket.getKey() + ": " + bucket.getDocCount());
                }
            }
        }
        return data;
    }

    private void getUnknownCountryVisits(Terms.Bucket bucket,
            Map<String, GroupByData> queriesPerCountry, boolean osdd) {

        ParsedMissing missingTerms = bucket.getAggregations().get("missing_country");

        if (missingTerms != null) {
            String key = config.getDefaultCountryName();
            long queries = missingTerms.getDocCount();
            if (queries > 0) {
                if (queriesPerCountry.containsKey(key)) {
                    GroupByData existingCountry = queriesPerCountry.get(key);
                    if (osdd) {
                        existingCountry.addOsddVisits(queries);
                    } else {
                        existingCountry.addSearchVisits(queries);
                    }
                } else {
                    if (osdd) {
                        queriesPerCountry.put(key, new GroupByData(key, queries, 0, osdd));
                    } else {
                        queriesPerCountry.put(key, new GroupByData(key, 0, queries, osdd));
                    }
                }
            }
        }
    }        
}
