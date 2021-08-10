/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Elasticsearch utilities
 * 
 * @author mng
 */
public class ElasticsearchUtils {
   
    public final static String UNKNOWN_PROVIDER = "SPB_UNKNOWN";
    public final static String NON_COLLECTION = "SPB_NON_COLLECTION";
    public final static String UNKNOWN = "Unknown";
    
    public static RestHighLevelClient buildRestClient(String elasticsearchUrl) throws MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        URL url = new URL(elasticsearchUrl);

        RestClientBuilder builder;
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            HttpHost httpHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

            // trust all certificates
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] certificate, String authType) -> true;

            SSLContextBuilder sslBuilder = new SSLContextBuilder().loadTrustMaterial(acceptingTrustStrategy);
            final SSLContext sslContext = sslBuilder.build();

            builder = RestClient.builder(httpHost);
            if (StringUtils.isNotEmpty(url.getPath())) {
                builder = builder.setPathPrefix(url.getPath());
            }

            builder = builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(
                        HttpAsyncClientBuilder httpClientBuilder) {
                    return httpClientBuilder
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .setSSLContext(sslContext);
                }
            });

        } else {
            builder = RestClient.builder(HttpHost.create(elasticsearchUrl));
        }

        return new RestHighLevelClient(builder);
    }
}
