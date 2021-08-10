package be.spacebel.metricsclient.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.X509Certificate;

/**
 *  HTTP client utilities
 * 
 * @author mng
 */
public class HttpInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(XmlUtils.class);

    public static final String HTTP_GET_DETAILS_URL = "url";
    public static final String HTTP_GET_DETAILS_RESPONSE = "response";
    public static final String HTTP_GET_DETAILS_ERROR_CODE = "errorCode";
    public static final String HTTP_GET_DETAILS_ERROR_MSG = "errorMsg";

    public static String httpGET(String location, Map<String, String> details) throws IOException {
        return httpGET(location, null, null, details);
    }

    public static String httpGET(String location, String username, String password, Map<String, String> details) throws IOException {
        LOG.debug("Invoke HTTP GET of URL " + location + ", username = " + username + ")");
        /*
         * Encode the parameter values
         */
        if (StringUtils.isNotEmpty(location)) {
            String baseUrl = StringUtils.substringBefore(location, "?");
            String queryString = StringUtils.substringAfter(location, "?");
            if (StringUtils.isNotEmpty(queryString)) {
                String[] paramArr = StringUtils.split(queryString, "&");
                /*
                 * LOG.debug("QueryString before parameters values: " +
                 * queryString);
                 */
                queryString = "";

                for (String param : paramArr) {
                    String key = StringUtils.substringBefore(param, "=");
                    String value = StringUtils.substringAfter(param, "=");
                    if (StringUtils.isNotEmpty(value)) {
                        /*
                         * decode the value first
                         */
                        value = URLDecoder.decode(value, "UTF-8");
                        /*
                         * encode again the value
                         */
                        value = URLEncoder.encode(value, "UTF-8");
                    }
                    queryString += key + "=" + value + "&";
                }
                /*
                 * Remove character "&" at the end of the string
                 */
                queryString = queryString.substring(0, queryString.length() - 1);

                location = baseUrl + "?" + queryString;
            }
        }

        URL url = new URL(location);
        CloseableHttpClient httpClient = null;
        String result = null;
        try {
            // disable SNI of Java 7 on runtime to avoid exception
            // unrecognized_name
            System.setProperty("jsse.enableSNIExtension", "false");

            HttpGet httpGet = new HttpGet(location);
            int timeout = 2 * 60 * 1000;
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout)
                    .setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
            httpGet.setConfig(requestConfig);

            if (details != null) {
                details.put(HTTP_GET_DETAILS_URL, location);
            }

            if ("https".equalsIgnoreCase(url.getProtocol())) {
                LOG.debug("Invoke HTTPS GET: " + location);
                
                // trust all certificates
                TrustStrategy acceptingTrustStrategy = (X509Certificate[] certificate, String authType) -> true ;
                try {
                    HttpClientBuilder httpBuilder = HttpClients.custom();
                    if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                        CredentialsProvider credsProvider = new BasicCredentialsProvider();
                        credsProvider.setCredentials(
                                new AuthScope(url.getHost(), url.getPort()),
                                new UsernamePasswordCredentials(username, password));
                        httpBuilder.setDefaultCredentialsProvider(credsProvider);
                    }

                    httpClient = httpBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .setSSLContext(new SSLContextBuilder().loadTrustMaterial(acceptingTrustStrategy).build())
                            .build();
                } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    LOG.debug(e.getMessage());
                    throw new IOException(e);
                }
            } else {
                LOG.debug("Invoke HTTP GET: " + location);
                if (StringUtils.isNotEmpty(username)
                        && StringUtils.isNotEmpty(password)) {
                    CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(
                            new AuthScope(url.getHost(), url.getPort()),
                            new UsernamePasswordCredentials(username, password));
                    httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
                } else {
                    httpClient = HttpClients.createDefault();
                }
            }

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String respStr = null;
                if (entity != null) {
                    respStr = EntityUtils.toString(entity);
                    EntityUtils.consume(entity);
                }
                if (status >= 200 && status < 300) {
                    result = respStr;
                } else {
                    if (details != null) {
                        details.put(HTTP_GET_DETAILS_ERROR_CODE, "" + status);
                        details.put(HTTP_GET_DETAILS_ERROR_MSG, respStr);
                    }
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (ConnectTimeoutException e) {
            if (details != null) {
                details.put(HTTP_GET_DETAILS_ERROR_CODE, "408");
                details.put(HTTP_GET_DETAILS_ERROR_MSG, "Request Timeout");
            }
        } catch (UnknownHostException e) {
            details.put(HTTP_GET_DETAILS_ERROR_CODE, "404");
            details.put(HTTP_GET_DETAILS_ERROR_MSG, "Unknown host: " + e.getMessage());
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
        //LOG.debug("result:" + result);
        return result;
    }
}
