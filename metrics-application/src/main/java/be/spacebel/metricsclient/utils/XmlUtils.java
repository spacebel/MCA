package be.spacebel.metricsclient.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class represents an XML parser kit
 *
 * @author mng
 */
public class XmlUtils implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(XmlUtils.class);
    /**
     * Indicates whether or not the factory is configured to produce parsers
     * which are namespace aware.
     */
    private boolean isNamespaceAware = true;

    /**
     * Indicates whether or not the factory is configured to produce parsers
     * which validate the XML content during parse.
     */
    private boolean isValidating = false;

    /**
     * Set the isNamespaceAware to new value.
     *
     * @param newIsNamespaceAware - new value of isNamespaceAware     
     */
    public void setIsNamespaceAware(boolean newIsNamespaceAware) {
        isNamespaceAware = newIsNamespaceAware;
    }

    /**
     * Set the isValidating to new value.
     *
     * @param newIsValidating - new value of isValidating     
     */
    public void setIsValidating(boolean newIsValidating) {
        isValidating = newIsValidating;
    }    

    /**
     * Parse input XML string to an XML document.
     *
     * @param xmlSource XMl string
     * @param isNamespaceAware Indicate if namespace is aware
     * @return an XML document
     * @throws java.io.IOException
     */
    public Document stringToDOM(String xmlSource, boolean isNamespaceAware) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(isNamespaceAware);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlSource)));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Load InputStream into an XML document.
     *
     * @param in Inputstream
     * @return an XML document     
     */
    public Document toDom(InputStream in) {
        LOG.debug("load input stream to java DOM");
        Document doc = null;
        try {
            if (in != null) {
                doc = getDocumentBuilder().parse(in);
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOG.debug("Error while loading input stream to Java DOM: " + ex.getMessage());
        }
        return doc;
    }   

    public String getNodeAttValue(Node node, String attName) {
        String value = null;
        if (node.getAttributes() != null && node.getAttributes().getNamedItem(attName) != null) {
            value = node.getAttributes().getNamedItem(attName).getNodeValue();
        }
        return value;
    }

    public String getNodeValue(Node node) {
        String value = null;
        try {
            value = node.getFirstChild().getNodeValue();
            if (value != null) {
                value = StringUtils.trimToEmpty(value);
            }
        } catch (DOMException e) {

        }
        return value;
    }   

    public String getXpathValue(Node node, String xpath) {
        try {
            XPathExpression expr = getXPath().compile(xpath);
            Node xpathNode = (Node) expr.evaluate(node, XPathConstants.NODE);
            if (xpathNode != null) {
                String xpathValue = getNodeValue(xpathNode);
                return StringUtils.trimToEmpty(xpathValue);
            }
        } catch (XPathExpressionException e) {
            LOG.debug(e.getMessage());
        }
        return null;
    }

    public XPath getXPath() {
        NamespaceContext nsCtx = new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix.equals("os")) {
                    return "http://a9.com/-/spec/opensearch/1.1/";
                }

                if (prefix.equals("atom")) {
                    return "http://www.w3.org/2005/Atom";
                }

                if (prefix.equals("param")) {
                    return "http://a9.com/-/spec/opensearch/extensions/parameters/1.0/";
                }

                if (prefix.equals("dc")) {
                    return "http://purl.org/dc/elements/1.1/";
                }

                if (prefix.equals("wsse")) {
                    return "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
                }

                if (prefix.equals("om")) {
                    return "http://www.opengis.net/om/2.0";
                }

                if (prefix.equals("ows")) {
                    return "http://www.opengis.net/ows/2.0";
                }

                if (prefix.equals("xlink")) {
                    return "http://www.w3.org/1999/xlink";
                }

                if (prefix.equals("media")) {
                    return "http://search.yahoo.com/mrss/";
                }

                if (prefix.equals("georss")) {
                    return "http://www.georss.org/georss";
                }

                if (prefix.equals("gml32")) {
                    return "http://www.opengis.net/gml/3.2";
                }

                if (prefix.equals("meta3")) {
                    return "http://www.metalinker.org/";
                }

                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String val) {
                return null;
            }

            @Override
            public String getPrefix(String uri) {
                return null;
            }
        };

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(nsCtx);
        return xpath;
    }

    private DocumentBuilderFactory getDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(isNamespaceAware);
            factory.setValidating(isValidating);
            return factory;
        } catch (Exception ex) {
            LOG.error("XMLParser.getDocumentBuilderFactory().error:" + ex.getMessage());
            return null;
        }
    }

    /**
     * Creates a new instance of DocumentBuilder using the currently configured
     * parameters.
     *
     * @return a document builder
     * @exception RemoteException Description of Exception.
     * @exception ParserConfigurationException Description of Exception.     
     */
    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return getDocumentBuilderFactory().newDocumentBuilder();
    }
}
