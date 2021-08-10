/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.config;

import be.spacebel.metricsclient.entities.Collection;
import be.spacebel.metricsclient.entities.MenuItem;
import be.spacebel.metricsclient.entities.Provider;
import be.spacebel.metricsclient.utils.HttpInvoker;
import be.spacebel.metricsclient.utils.Utils;
import be.spacebel.metricsclient.utils.XmlUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Declares @Bean method that will be processed by the Spring container to
 * generate configuration bean at runtime
 *
 * @author mng
 */
@Configuration
@EnableScheduling
@ComponentScan("be.spacebel")
@EnableWebMvc
public class LoadConfig implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(LoadConfig.class);

    private final XmlUtils xmlUtil = new XmlUtils();

    @Value("${application.name}")
    String appName;

    @Value("${date.range}")
    int numberOfDays;

    @Value("${top.collections}")
    int numOfTopCollections;

    @Value("${providers.file}")
    String providersFile;

    @Value("${opensearch.collection.catalogue.url}")
    String osgwUrl;

    @Value("${mimetype.parameter}")
    String osgwRequestMimeTypeParam;

    @Value("${matching.collection.id.regex}")
    String matchingColIdRegex;

    @Value("${elasticsearch.url}")
    String elasticsearchUrl;

    @Value("${elasticsearch.search.request.index}")
    String elasticsearchSearchIndex;

    @Value("${elasticsearch.osdd.request.index}")
    String elasticsearchOsddIndex;

    @Value("${number.of.collections.per.request}")
    int numOfColsPerReq;

    @Value("${default.country.code}")
    String defaultCountryCode;

    @Value("${default.country.name}")
    String defaultCountryName;

    @Value("${main.menu.items}")
    String menuItems;

    @Autowired
    Environment env;

    /**
     * Load all application configurations
     *
     * @return
     */
    @Bean
    public Config load() {
        Config config = new Config();
        config.setAppName(appName);
        config.setDateRange(numberOfDays);
//        config.setDefaultStartDate(Utils.defaultStartDate(numberOfDays));
//        config.setDefaultEndDate(Utils.defaultEndDate());
        //config.setOsgwUrl(osgwUrl);
        config.setElasticsearchUrl(elasticsearchUrl);
        config.setElasticsearchSearchIndex(elasticsearchSearchIndex);
        config.setElasticsearchOsddIndex(elasticsearchOsddIndex);
        config.setNumOfTopCollections(numOfTopCollections);
        config.setNumOfCollectionsPerRequest(numOfColsPerReq);

        config.setDefaultCountryCode(defaultCountryCode);
        config.setDefaultCountryName(defaultCountryName);

        setMenuItems(config);

        try {
            InputStream providerInput = null;
            if (providersFile != null && !providersFile.isEmpty()) {
                try {
                    providerInput = new FileInputStream(providersFile);
                } catch (FileNotFoundException e) {
                    LOG.debug(e.getMessage());
                    Resource resource = new ClassPathResource("providers.xml", this.getClass().getClassLoader());
                    providerInput = resource.getInputStream();
                }
            }

            if (providerInput == null) {
                Resource resource = new ClassPathResource("providers.xml", this.getClass().getClassLoader());
                providerInput = resource.getInputStream();
            }

            Document providersDoc = xmlUtil.toDom(providerInput);
            if (providersDoc != null) {
                NodeList orgNodeList = providersDoc.getDocumentElement().getElementsByTagName("organisation");
                if (orgNodeList != null
                        && orgNodeList.getLength() > 0) {
                    for (int i = 0; i < orgNodeList.getLength(); i++) {
                        String name = xmlUtil.getNodeAttValue(orgNodeList.item(i), "name");
                        String query = null;
                        NodeList children = orgNodeList.item(i).getChildNodes();
                        if (children != null && children.getLength() > 0) {
                            for (int k = 0; k < children.getLength(); k++) {
                                if (children.item(k).getNodeType() == Node.ELEMENT_NODE
                                        && "query".equals(children.item(k).getLocalName())) {
                                    query = xmlUtil.getNodeValue(children.item(k));
                                    break;
                                }
                            }
                        }
                        if (StringUtils.isNotEmpty(name)
                                && StringUtils.isNotEmpty(query)) {
                            LOG.debug("Provider = " + name + "; Query = " + query);
                            Provider provider = new Provider(name);
                            config.addProvider(provider);

                            String searchUrl = osgwUrl + "?" + osgwRequestMimeTypeParam + "&" + query;
                            while (StringUtils.isNotEmpty(searchUrl)) {
                                searchUrl = getCollections(config, provider, searchUrl);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error while loading providers " + e);
        }
        return config;
    }

    /**
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Add external header resources
        String extResource = env.getProperty("EXT_HEADER_RESOURCES");

        if (extResource != null && !extResource.trim().isEmpty()) {
            String osName = System.getProperty("os.name");
            LOG.debug("OS Name = " + osName);
            if (StringUtils.containsIgnoreCase(osName, "windows")) {
                extResource = "file:///" + Utils.trimSlashs(extResource) + "/";
            } else {
                extResource = "file:/" + Utils.trimSlashs(extResource) + "/";
            }
            LOG.debug("Add external resources folder : " + extResource);

            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");
            registry.addResourceHandler("/banner/**")
                    .addResourceLocations(extResource);

        } else {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");
        }

    }

    private String getCollections(Config config, Provider provider, String searchUrl) {
        String nextPageUrl = null;
        try {
            Map<String, String> errorDetails = new HashMap<>();
            String osResponse = HttpInvoker.httpGET(searchUrl, errorDetails);
            String errorCode = errorDetails.get(
                    HttpInvoker.HTTP_GET_DETAILS_ERROR_CODE);
            String errorMsg = errorDetails.get(
                    HttpInvoker.HTTP_GET_DETAILS_ERROR_MSG);

            if (errorCode != null) {
                LOG.debug("Error case: (" + errorCode + ") " + errorMsg);
            } else {
                LOG.debug("Success case");
                if (StringUtils.isNotEmpty(osResponse)) {
                    Document responseDoc = xmlUtil.stringToDOM(osResponse, true);
                    XPath xpath = xmlUtil.getXPath();
                    XPathExpression expr = xpath.compile("./atom:feed/atom:entry");
                    NodeList entryNodeList = (NodeList) expr.evaluate(responseDoc,
                            XPathConstants.NODESET);
                    if (entryNodeList.getLength() > 0) {
                        for (int idx = 0; idx < entryNodeList.getLength(); idx++) {
                            Node entryNode = entryNodeList.item(idx);
                            expr = xpath.compile("./atom:link[@rel='search' and @type='application/opensearchdescription+xml']");
                            Node osddUrlNode = (Node) expr.evaluate(entryNode, XPathConstants.NODE);
                            String realColId = null;
                            if (osddUrlNode != null) {
                                String osddUrl = xmlUtil.getNodeAttValue(osddUrlNode, "href");
                                if (StringUtils.isNotEmpty(osddUrl)) {
                                    if (StringUtils.isNotEmpty(matchingColIdRegex)) {
                                        Matcher m = Pattern.compile(matchingColIdRegex).matcher(osddUrl);
                                        while (m.find()) {
                                            realColId = m.group(1);
                                            if (StringUtils.isNotEmpty(realColId)) {
                                                try {
                                                    realColId = java.net.URLDecoder.decode(realColId, StandardCharsets.UTF_8.name());
                                                } catch (UnsupportedEncodingException e) {                                                    
                                                }                                                
                                                break;
                                            }
                                        }
                                    }
                                    /*
                                    if (osddUrl.contains("parentIdentifier=")) {
                                        osddUrl = StringUtils.substringAfter(osddUrl, "parentIdentifier=");
                                        realColId = StringUtils.substringBefore(osddUrl, "&");
                                    }else{
                                        realColId = StringUtils.substringBetween(osddUrl, "collections/series/items/", "/api");                                        
                                    }
                                     */

                                }
                            }
                            expr = xpath.compile("./dc:identifier");
                            Node xpathNode = (Node) expr.evaluate(entryNode, XPathConstants.NODE);
                            if (xpathNode != null) {
                                String collectionId = StringUtils.trimToEmpty(xmlUtil.getNodeValue(xpathNode));
                                LOG.debug(collectionId);

                                if (StringUtils.isNotEmpty(collectionId)) {
                                    Collection col = new Collection(collectionId, provider.getName());
                                    provider.addCollection(col);
                                    config.addCollection(col);

                                    if (StringUtils.isNotEmpty(realColId)
                                            && !realColId.equals(collectionId)) {
                                        col.setRealId(realColId);
                                        config.putToRealColMap(realColId, collectionId);
                                        LOG.debug(String.format("%s is real collection of %s", realColId, collectionId));
                                    }
                                }
                            }
                        }
                        expr = xpath.compile("./atom:feed/atom:link[@rel='next' and @type='application/atom+xml']");
                        Node urlNode = (Node) expr.evaluate(responseDoc, XPathConstants.NODE);
                        if (urlNode != null) {
                            nextPageUrl = xmlUtil.getNodeAttValue(urlNode, "href");
                            LOG.debug("Next Page Url: " + nextPageUrl);
                        }
                    }
                }
            }
        } catch (IOException | XPathExpressionException e) {
            LOG.error(e.getMessage());
        }
        return nextPageUrl;
    }

    private void setMenuItems(Config config) {
        String[] items = menuItems.split(",");
        List<MenuItem> menuItemList = new ArrayList<>();
        for (String item : items) {
            String text = StringUtils.substringBefore(item, "{");
            String url = StringUtils.substringBetween(item, "{", "}");
            menuItemList.add(new MenuItem(text, url));
        }
        config.setMenuItems(menuItemList);
    }
}
