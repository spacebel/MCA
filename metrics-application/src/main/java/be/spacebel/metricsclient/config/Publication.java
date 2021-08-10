/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.config;

import be.spacebel.metricsclient.entities.Collection;
import be.spacebel.metricsclient.entities.OsddMetric;
import be.spacebel.metricsclient.entities.ReportFile;
import be.spacebel.metricsclient.entities.SearchMetric;
import be.spacebel.metricsclient.utils.ElasticsearchUtils;
import be.spacebel.metricsclient.utils.FtpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Defines scheduled tasks to publish metrics to the dedicated FTP server
 *
 * @author mng
 */
@Component
public class Publication {

    @Value("${ftp.server}")
    String ftpServer;

    @Value("${ftp.server.port}")
    int ftpServerPort;

    @Value("${ftp.username}")
    String ftpUsername;

    @Value("${ftp.password}")
    String ftpPassword;

    @Value("${ftp.report.directory}")
    String ftpReportDir;

    @Value("${report.local.directory}")
    String reportLocalDir;

    @Value("${number.of.daily.reports}")
    int maxReports;

    @Value("${report.file.name.prefix}")
    String fileNamePrefix;

    @Value("${report.file.name.source.id}")
    String fileNameSource;

    @Value("${report.file.name.search}")
    String fileNameSearch;

    @Value("${report.file.name.osdd}")
    String fileNameOsdd;

    @Value("${report.file.name.global}")
    String fileNameGlobal;

    @Value("${report.file.name.creation.date.format}")
    String fileCreationDateFormat;

    @Value("${report.file.name.extension}")
    String fileNameExtension;

    @Value("${report.header.prefix}")
    String headerPrefix;

    @Value("${report.header.extraction.date.format}")
    String headerExtractionDateFormat;

    @Value("${report.header.window.start.date.format}")
    String headerWindowStartDateFormat;

    @Value("${report.header.window.end.date.format}")
    String headerWindowEndDateFormat;

    @Value("${report.header.separator}")
    String headerSeparator;

    @Value("${report.header.source.id}")
    String headerSource;

    @Value("${report.header.search}")
    String headerSearch;

    @Value("${report.header.osdd}")
    String headerOsdd;

    @Value("${report.header.global}")
    String headerGlobal;

    @Value("${report.record.field.separator}")
    String fieldSeparator;

    @Value("${report.footer.prefix}")
    String footerPrefix;

    @Value("${report.footer.extraction.date.format}")
    String footerExtractionDateFormat;

    @Value("${report.footer.separator}")
    String footerSeparator;

//    @Value("${back.hours}")
//    int backHours;
    @Autowired
    private Config config;

    private static final Logger LOG = LoggerFactory.getLogger(Publication.class);

    /**
     * Scheduler to publish Search Request Metrics
     */
    @Scheduled(cron = "${cron.expression}")
    public void searchRequestMetrics() {

        try {
            LOG.debug("Start publishing Search Request Metrics (QUER) at " + currentDateTime());

            List<ReportFile> reportFiles = listReportFiles(fileNameSearch, headerSearch);
            LOG.debug("Number of reports : " + reportFiles.size());

            FtpClient ftpClient = new FtpClient(ftpServer, ftpServerPort, ftpUsername, ftpPassword, ftpReportDir);
            ftpClient.listMissingFiles(reportFiles);
            LOG.debug("Number of reports to be created : " + reportFiles.size());

            RestHighLevelClient client = ElasticsearchUtils.buildRestClient(config.getElasticsearchUrl());

            for (ReportFile reportFile : reportFiles) {
                LOG.debug("Start publishing report File : " + reportFile.getName() + " at " + currentDateTime());
                //long startTime = currentStartDateInSeconds();
                //long endTime = currentEndDateInSeconds(startTime);

                BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                //boolQueryBuilder.must(QueryBuilders.termQuery("code", 200));
                boolQueryBuilder.filter(
                        QueryBuilders.rangeQuery("timestamp")
                                .gt(reportFile.getStartTime())
                                .lte(reportFile.getEndTime()));

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(boolQueryBuilder);

                // group by collection
                TermsAggregationBuilder aggregation = AggregationBuilders
                        .terms("collections")
                        .field("parentIdentifier.keyword")
                        .size(10000); // set aggregation to maximum number (10000) to get all aggregations            

                // group by country to get number of requests per country per collection
                aggregation.subAggregation(AggregationBuilders
                        .terms("countries")
                        .field("country.keyword"));

                searchSourceBuilder.aggregation(aggregation);
                searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

                LOG.debug("Search Request Metrics Query: " + searchSourceBuilder.toString());

                SearchRequest searchRequest = new SearchRequest(config.getElasticsearchSearchIndex());
                searchRequest.source(searchSourceBuilder);

                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                LOG.debug("Search Request Metrics Responses: " + searchResponse.toString());

                Aggregations aggregations = searchResponse.getAggregations();

                List<String> dataRecords = new ArrayList<>();
                // Add header
                dataRecords.add(reportFile.getHeader());

                Map<String, Collection> colsMap = config.getCollections();
                List<SearchMetric> searchMetrics = new ArrayList<>();

                if (aggregations != null) {
                    Terms colTerms = aggregations.get("collections");
                    if (colTerms != null
                            && colTerms.getBuckets() != null
                            && colTerms.getBuckets().size() > 0) {
                        //System.out.println("Number of Buckets: " + colTerms.getBuckets().size());
                        colTerms.getBuckets().forEach((bucket) -> {
                            String colId = (String) bucket.getKey();

                            if (StringUtils.isNotEmpty(colId)) {
                                String metricColId;
                                String metricProvider;

                                if (colsMap.containsKey(colId)) {
                                    Collection col = colsMap.get(colId);
                                    metricColId = col.getId();
                                    metricProvider = col.getProvider();
                                } else {
                                    if (config.getRealColMap().containsKey(colId)) {
                                        Collection col = colsMap.get(config.getRealColMap().get(colId));
                                        metricColId = col.getId();
                                        metricProvider = col.getProvider();

                                    } else {
                                        metricColId = colId;
                                        metricProvider = ElasticsearchUtils.UNKNOWN;
                                        LOG.debug("Collection of unknown provider" + colId);
                                    }
                                }

                                Terms terms = bucket.getAggregations().get("countries");

                                long countriesQueries = 0;
                                if (terms != null
                                        && terms.getBuckets() != null
                                        && terms.getBuckets().size() > 0) {

                                    for (Terms.Bucket subBucket : terms.getBuckets()) {

                                        String key = subBucket.getKeyAsString();
                                        if (key != null && !key.isEmpty()
                                                && !key.equalsIgnoreCase("null")) {

                                            SearchMetric oneMetric = new SearchMetric();

                                            oneMetric.setCollectionId(metricColId);
                                            oneMetric.setProvider(metricProvider);
                                            oneMetric.setCountryCode(key);

                                            long oneCountryQueries = subBucket.getDocCount();
                                            LOG.debug("Queries per country ==> " + key + ":" + oneCountryQueries);
                                            countriesQueries += oneCountryQueries;

                                            int index = searchMetrics.indexOf(oneMetric);
                                            if (index == -1) {
                                                oneMetric.setQueries(oneCountryQueries);
                                                searchMetrics.add(oneMetric);
                                            } else {
                                                searchMetrics.get(index).addQueries(oneCountryQueries);
                                            }
                                        }
                                    }
                                }

                                //LOG.debug("Total queries " + bucket.getDocCount());
                                long defaultCountryQueries = bucket.getDocCount() - countriesQueries;
                                //LOG.debug("The rest queries " + defaultCountryQueries);
                                if (defaultCountryQueries > 0) {
                                    SearchMetric oneMetric = new SearchMetric();

                                    oneMetric.setCollectionId(metricColId);
                                    oneMetric.setProvider(metricProvider);
                                    oneMetric.setCountryCode(config.getDefaultCountryCode());

                                    oneMetric.addQueries(defaultCountryQueries);
                                    searchMetrics.add(oneMetric);
                                }
                            }
                        });

                        if (!searchMetrics.isEmpty()) {
                            PriorityQueue<SearchMetric> priorityQueue = new PriorityQueue<>(searchMetrics.size());
                            priorityQueue.addAll(searchMetrics);

                            // Add records
                            while (priorityQueue.size() > 0) {
                                String oneRecord = priorityQueue.poll().toLine(fieldSeparator);
                                //LOG.debug("Data record: " + oneRecord);
                                dataRecords.add(oneRecord);
                            }
                        }
                    } else {
                        LOG.debug("No result");
                    }
                } else {
                    LOG.debug("No result");
                }

                // Add footer
                dataRecords.add(reportFile.getFooter());

                writeToFile(reportFile.getName(), dataRecords);

                LOG.debug("Finish publishing report File : " + reportFile.getName() + " at " + currentDateTime());
            }// end for

            // Upload all generated reports to FTP server
            if (!reportFiles.isEmpty()) {
                ftpClient.upload(reportFiles, reportLocalDir);
            }

            LOG.debug("Finish publishing Search Request Metrics (QUER) at " + currentDateTime());

        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            LOG.error("Error while publishing Search Request Metrics (QUER) metrics " + e);
        }
    }

    /**
     * Scheduler to publish OSDD Request Metrics
     */
    @Scheduled(cron = "${cron.expression}")
    public void osddRequestMetrics() {

        try {
            LOG.debug("Start publishing OSDD Request Metrics (OSDD) at " + currentDateTime());

            List<ReportFile> reportFiles = listReportFiles(fileNameOsdd, headerOsdd);
            LOG.debug("Number of reports : " + reportFiles.size());

            FtpClient ftpClient = new FtpClient(ftpServer, ftpServerPort, ftpUsername, ftpPassword, ftpReportDir);
            ftpClient.listMissingFiles(reportFiles);
            LOG.debug("Number of reports to be created : " + reportFiles.size());

            RestHighLevelClient client = ElasticsearchUtils.buildRestClient(config.getElasticsearchUrl());

            for (ReportFile reportFile : reportFiles) {
                LOG.debug("Start publishing report File : " + reportFile.getName() + " at " + currentDateTime());

                BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                //boolQueryBuilder.must(QueryBuilders.termQuery("code", 200));
                boolQueryBuilder.filter(
                        QueryBuilders.rangeQuery("timestamp")
                                .gt(reportFile.getStartTime())
                                .lte(reportFile.getEndTime()));

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(boolQueryBuilder);

                // group by collection
                TermsAggregationBuilder aggregation = AggregationBuilders
                        .terms("collections")
                        .field("parentIdentifier.keyword")
                        .size(10000); // set aggregation to maximum number (10000) to get all aggregations            

                // group by country to get number of requests per country per collection
                aggregation.subAggregation(AggregationBuilders
                        .terms("countries")
                        .field("country.keyword")
                        .subAggregation(AggregationBuilders
                                .terms("clientids")
                                .field("source.keyword")));
                aggregation.subAggregation(AggregationBuilders
                        .missing("missing_country")
                        .field("country_name.keyword")
                        .subAggregation(AggregationBuilders
                                .terms("clientids")
                                .field("source.keyword")));

                // group by source to get number of requests per client ID per collection
                //aggregation.subAggregation();
                searchSourceBuilder.aggregation(aggregation);
                searchSourceBuilder.size(1);// set hits to 1 because we don't use hits

                LOG.debug("OSDD Request Metrics Query: " + searchSourceBuilder.toString());

                SearchRequest searchRequest = new SearchRequest(config.getElasticsearchOsddIndex());
                searchRequest.source(searchSourceBuilder);

                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                LOG.debug("OSDD Request Metrics Responses: " + searchResponse.toString());

                Aggregations aggregations = searchResponse.getAggregations();

                List<String> dataRecords = new ArrayList<>();
                // Add header
                dataRecords.add(reportFile.getHeader());

                List<OsddMetric> osddMetrics = new ArrayList<>();
                if (aggregations != null) {
                    Terms colTerms = aggregations.get("collections");
                    if (colTerms != null
                            && colTerms.getBuckets() != null
                            && colTerms.getBuckets().size() > 0) {
                        //System.out.println("Number of Buckets: " + colTerms.getBuckets().size());
                        colTerms.getBuckets().forEach((bucket) -> {
                            String colId = (String) bucket.getKey();

                            if (StringUtils.isNotEmpty(colId)) {

                                Terms countryTerms = bucket.getAggregations().get("countries");
                                if (countryTerms != null
                                        && countryTerms.getBuckets() != null
                                        && countryTerms.getBuckets().size() > 0) {

                                    countryTerms.getBuckets().forEach((subBucket) -> {
                                        String countryCode = subBucket.getKeyAsString();
                                        if (countryCode == null
                                                || countryCode.isEmpty()
                                                || countryCode.equalsIgnoreCase("null")) {
                                            countryCode = config.getDefaultCountryCode();
                                        }

                                        Terms clientIdTerms = subBucket.getAggregations().get("clientids");
                                        if (clientIdTerms != null
                                                && clientIdTerms.getBuckets() != null
                                                && clientIdTerms.getBuckets().size() > 0) {

                                            for (Terms.Bucket clientIdBucket : clientIdTerms.getBuckets()) {
                                                String clientId = clientIdBucket.getKeyAsString();
                                                if (clientId == null
                                                        || clientId.isEmpty()
                                                        || clientId.equalsIgnoreCase("null")) {
                                                    clientId = ElasticsearchUtils.UNKNOWN;
                                                }

                                                OsddMetric oneMetric = new OsddMetric(colId, countryCode, clientId, clientIdBucket.getDocCount());

                                                int index = osddMetrics.indexOf(oneMetric);
                                                if (index == -1) {
                                                    osddMetrics.add(oneMetric);
                                                } else {
                                                    osddMetrics.get(index).addQueries(clientIdBucket.getDocCount());
                                                }
                                            }
                                        }
                                    });
                                }

                                Missing missingTerms = bucket.getAggregations().get("missing_country");
                                Terms clientIdTerms = missingTerms.getAggregations().get("clientids");
                                if (clientIdTerms != null
                                        && clientIdTerms.getBuckets() != null
                                        && clientIdTerms.getBuckets().size() > 0) {
                                    for (Terms.Bucket clientIdBucket : clientIdTerms.getBuckets()) {
                                        String clientId = clientIdBucket.getKeyAsString();
                                        if (clientId == null
                                                || clientId.isEmpty()
                                                || clientId.equalsIgnoreCase("null")) {
                                            clientId = ElasticsearchUtils.UNKNOWN;
                                        }
                                        LOG.debug("Missing country clientId ==> " + clientId);
                                        OsddMetric oneMetric = new OsddMetric(colId, config.getDefaultCountryCode(), clientId, clientIdBucket.getDocCount());

                                        int index = osddMetrics.indexOf(oneMetric);
                                        if (index == -1) {
                                            osddMetrics.add(oneMetric);
                                        } else {
                                            osddMetrics.get(index).addQueries(clientIdBucket.getDocCount());
                                        }
                                    }
                                }
                            }
                        });

                        if (!osddMetrics.isEmpty()) {
                            PriorityQueue<OsddMetric> priorityQueue = new PriorityQueue<>(osddMetrics.size());
                            priorityQueue.addAll(osddMetrics);

                            // Add records
                            while (priorityQueue.size() > 0) {
                                String oneRecord = priorityQueue.poll().toLine(fieldSeparator);
                                LOG.debug("Data record: " + oneRecord);
                                dataRecords.add(oneRecord);
                            }
                        }
                    } else {
                        LOG.debug("No result");
                    }
                } else {
                    LOG.debug("No result");
                }

                // Add footer
                dataRecords.add(reportFile.getFooter());
                writeToFile(reportFile.getName(), dataRecords);

                LOG.debug("Finish publishing report File : " + reportFile.getName() + " at " + currentDateTime());
            }

            // Upload all generated reports to FTP server
            if (!reportFiles.isEmpty()) {
                ftpClient.upload(reportFiles, reportLocalDir);
            }

            LOG.debug("Finish publishing OSDD Request Metrics (OSDD) at " + currentDateTime());

        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            LOG.error("Error while publishing OSDD Request Metrics (OSDD) " + e);
        }
    }

    /**
     * Scheduler to publish Total Request Metrics
     */
    @Scheduled(cron = "${cron.expression}")
    public void totalRequestMetrics() {
        try {
            LOG.debug("Start publishing Total Request Metrics (TOTL) at " + currentDateTime());

            List<ReportFile> reportFiles = listReportFiles(fileNameGlobal, headerGlobal);
            LOG.debug("Number of reports : " + reportFiles.size());

            FtpClient ftpClient = new FtpClient(ftpServer, ftpServerPort, ftpUsername, ftpPassword, ftpReportDir);
            ftpClient.listMissingFiles(reportFiles);
            LOG.debug("Number of reports to be created : " + reportFiles.size());

            RestHighLevelClient client = ElasticsearchUtils.buildRestClient(config.getElasticsearchUrl());

            for (ReportFile reportFile : reportFiles) {
                LOG.debug("Start publishing report File : " + reportFile.getName() + " at " + currentDateTime());
                BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                //boolQueryBuilder.must(QueryBuilders.termQuery("code", 200));
                boolQueryBuilder.filter(
                        QueryBuilders.rangeQuery("timestamp")
                                .gt(reportFile.getStartTime())
                                .lte(reportFile.getEndTime()));

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(boolQueryBuilder);

                /*
                Firstly count on Search Request Index
                 */
                CountRequest countRequest = new CountRequest(config.getElasticsearchSearchIndex());
                countRequest.source(searchSourceBuilder);

                CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
                LOG.debug("Search Count Resp: " + countResponse);

                long total = countResponse.getCount();

                /*
                And then count on OSDD
                 */
                countRequest = new CountRequest(config.getElasticsearchOsddIndex());
                countRequest.source(searchSourceBuilder);

                countResponse = client.count(countRequest, RequestOptions.DEFAULT);
                LOG.debug("OSDD Count Resp: " + countResponse);
                total += countResponse.getCount();

                List<String> dataRecords = new ArrayList<>();

                // Add header
                dataRecords.add(reportFile.getHeader());

                // Add total requests
                dataRecords.add(Long.toString(total));

                // Add footer
                dataRecords.add(reportFile.getFooter());

                writeToFile(reportFile.getName(), dataRecords);

                LOG.debug("Finish publishing report File : " + reportFile.getName() + " at " + currentDateTime());
            }

            // Upload all generated reports to FTP server
            if (!reportFiles.isEmpty()) {
                ftpClient.upload(reportFiles, reportLocalDir);
            }

            LOG.debug("Finish publishing Total Request Metrics (TOTL) at " + currentDateTime());

        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            LOG.error("Error while publishing Total Request Metrics (TOTL) " + e);
        }
    }

    private String buildFileName(String dataType, long startTimeInSecond) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTimeInSecond * 1000);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        SimpleDateFormat formatter = new SimpleDateFormat(fileCreationDateFormat);
        String creationDate = formatter.format(cal.getTime());

        return fileNamePrefix
                + "_" + fileNameSource + "_" + dataType
                + "_" + creationDate
                + "." + fileNameExtension;
    }

    private String buildFileHeader(String headerType,
            long startTimeInSecond, long endTimeInSecond) {
        String extractionDate = formatDate(headerExtractionDateFormat, startTimeInSecond);
        String windowStartDate = formatDate(headerWindowStartDateFormat, startTimeInSecond);
        String windowEndDate = formatDate(headerWindowEndDateFormat, endTimeInSecond);

        return headerPrefix
                + headerSeparator + headerSource + "_" + headerType
                + headerSeparator + extractionDate
                + headerSeparator + windowStartDate
                + headerSeparator + windowEndDate
                + headerSeparator;
    }

    private String buildFileFooter(long endTimeInSecond) {
        String extractionDate = formatDate(footerExtractionDateFormat, endTimeInSecond);

        return footerPrefix + footerSeparator + extractionDate;
    }

    private void writeToFile(String fileName, List<String> records) throws IOException {
        Path path = Paths.get(reportLocalDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        String filePath = reportLocalDir + "/" + fileName;
        LOG.debug("Write data to file " + filePath);
        Files.write(Paths.get(filePath), records,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);
    }

    private String formatDate(String format, long timeInSecond) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(new Date(timeInSecond * 1000));
    }

    private String currentDateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
        return formatter.format(new Date());
    }

    private List<ReportFile> listReportFiles(String dataType, String headerType) {
        List<ReportFile> reportFiles = new ArrayList<>();

        Calendar now = Calendar.getInstance();
        // The crons are executed at midnight ==> get date of "backHours" hours before to make sure we get the correct date
        //now.add(Calendar.HOUR_OF_DAY, ((-1) * backHours));

        // add 1 minute to make sure the current date time pass to the new day if the scheduler is performed at midnight
        now.add(Calendar.MINUTE, 1);

        // get the previous date
        now.add(Calendar.DATE, -1);

        long time = now.getTimeInMillis();

        Calendar currentDate = Calendar.getInstance();
        currentDate.setTimeInMillis(time);

        // convert 23:59:59 to seconds
        long seconds = (23 * 60 * 60) + (59 * 60) + 59;

        for (int i = 0; i < maxReports; i++) {
            Calendar oneDay = Calendar.getInstance();
            oneDay.setTimeInMillis(currentDate.getTimeInMillis());
            if (i > 0) {
                // go back i days
                oneDay.add(Calendar.DAY_OF_YEAR, ((-1) * i));
            }

            oneDay.set(Calendar.HOUR_OF_DAY, 0);
            oneDay.set(Calendar.MINUTE, 0);
            oneDay.set(Calendar.SECOND, 0);

            long startTime = oneDay.getTimeInMillis() / 1000;
            long endTime = startTime + seconds;
            String fileName = buildFileName(dataType, startTime);
            String header = buildFileHeader(headerType, startTime, endTime);
            String footer = buildFileFooter(endTime);
            reportFiles.add(new ReportFile(fileName, header, footer, startTime, endTime));
        }

        return reportFiles;
    }

}
