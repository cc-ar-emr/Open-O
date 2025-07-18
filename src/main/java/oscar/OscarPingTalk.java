//CHECKSTYLE:OFF
/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */


package oscar;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.chip.ping.client.PingTalkClient;
import org.chip.ping.client.PingTalkClientImpl;
import org.chip.ping.xml.DefaultCddmGenerator;
import org.chip.ping.xml.cddm.BodyContentType;
import org.chip.ping.xml.cddm.BodyType;
import org.chip.ping.xml.cddm.CddmType;
import org.chip.ping.xml.cddm.DataType;
import org.chip.ping.xml.talk.AddCddmResultType;
import org.chip.ping.xml.talk.AuthenticateResultType;
import org.oscarehr.util.MiscUtils;
import org.w3c.dom.Document;

/*
 * PingOscar.java
 *
 * Created on April 4, 2004, 9:16 PM
 */

/**
 * @author root
 */
public class OscarPingTalk {

    PingTalkClientImpl client;

    /**
     * Creates a new instance of OscarPingTalk
     */
    public OscarPingTalk() {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                try {
                    // Get the peer certificates
                    java.security.cert.Certificate[] certs = session.getPeerCertificates();
                    if (certs.length == 0) {
                        MiscUtils.getLogger().warn("No peer certificates found for hostname: " + urlHostName);
                        return false;
                    }
                    
                    // Check if it's an X509 certificate
                    if (!(certs[0] instanceof java.security.cert.X509Certificate)) {
                        MiscUtils.getLogger().warn("Certificate is not X509 for hostname: " + urlHostName);
                        return false;
                    }
                    
                    java.security.cert.X509Certificate x509Cert = (java.security.cert.X509Certificate) certs[0];
                    
                    // Check Subject Alternative Names first
                    java.util.Collection<java.util.List<?>> altNames = x509Cert.getSubjectAlternativeNames();
                    if (altNames != null) {
                        for (java.util.List<?> altName : altNames) {
                            if (altName.size() >= 2) {
                                Integer type = (Integer) altName.get(0);
                                String name = (String) altName.get(1);
                                // Type 2 is DNS name
                                if (type == 2 && matchesHostname(urlHostName, name)) {
                                    return true;
                                }
                            }
                        }
                    }
                    
                    // Check Common Name in subject
                    String subjectDN = x509Cert.getSubjectX500Principal().getName();
                    String cn = extractCommonName(subjectDN);
                    if (cn != null && matchesHostname(urlHostName, cn)) {
                        return true;
                    }
                    
                    MiscUtils.getLogger().warn("Hostname verification failed for: " + urlHostName + 
                                             " against certificate subject: " + subjectDN);
                    return false;
                    
                } catch (Exception e) {
                    MiscUtils.getLogger().error("Error during hostname verification for: " + urlHostName, e);
                    return false;
                }
            }
            
            private boolean matchesHostname(String hostname, String certName) {
                if (hostname == null || certName == null) {
                    return false;
                }
                
                // Convert to lowercase for comparison
                hostname = hostname.toLowerCase();
                certName = certName.toLowerCase();
                
                // Exact match
                if (hostname.equals(certName)) {
                    return true;
                }
                
                // Wildcard match (*.example.com matches subdomain.example.com)
                if (certName.startsWith("*.")) {
                    String domain = certName.substring(2);
                    return hostname.endsWith("." + domain);
                }
                
                return false;
            }
            
            private String extractCommonName(String subjectDN) {
                if (subjectDN == null) {
                    return null;
                }
                
                // Parse CN from subject DN (e.g., "CN=example.com, O=Organization, ...")
                String[] parts = subjectDN.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.toUpperCase().startsWith("CN=")) {
                        return part.substring(3);
                    }
                }
                return null;
            }
        });
    }

    public String connect(String username, String password) throws Exception {
        String serverURL = OscarProperties.getInstance().getProperty("PING-SERVER", ""); //"http://127.0.0.1:8080/ping-server/PingServlet";
        String actorTicket = null;
        Map properties = new Properties();
        properties.put(PingTalkClient.CERT_TRUST_KEY, PingTalkClient.ALL_CERTS_ACCEPTED);
        properties.put(PingTalkClient.VALIDATE_REQUESTS, Boolean.TRUE);
        properties.put(PingTalkClient.SERVER_LOCATION, serverURL);
        client = new PingTalkClientImpl(properties);

        AuthenticateResultType art = client.authenticate(username, password);
        if (art == null) {
            throw new Exception("Problem Authenticating User");
        }
        actorTicket = art.getActorTicket();
        return actorTicket;
    }

    public boolean sendCddm(String actorTicket, String pingId, String owner, String originAgent,
                            String author, String level1, String level2, DataType dataType) throws Exception {
        DefaultCddmGenerator cddmGenerator = new DefaultCddmGenerator();
        CddmType cddmType = cddmGenerator.generateDefaultCddm(owner, originAgent, author, level1,
                level2);
        BodyType bodyType = cddmType.getCddmBody();
        List dataList = bodyType.getData();
        dataList.add(dataType);
        MiscUtils.getLogger().debug(actorTicket + " " + pingId + " " + cddmType);
        AddCddmResultType adrt = client.addCddm(actorTicket, pingId, cddmType);
        return true;
    }

    public boolean sendCddm(String actorTicket, String pingId, CddmType cddmType) throws Exception {
        AddCddmResultType adrt = client.addCddm(actorTicket, pingId, cddmType);
        return true;
    }

    public CddmType getCddm(String owner, String originAgent, String author, String level1,
                            String level2, DataType dataType) {
        DefaultCddmGenerator cddmGenerator = new DefaultCddmGenerator();
        CddmType cddmType = cddmGenerator.generateDefaultCddm(owner, originAgent, author, level1,
                level2);
        BodyType bodyType = cddmType.getCddmBody();
        List dataList = bodyType.getData();
        dataList.add(dataType);
        return cddmType;
    }

    public DataType getDataType(org.w3c.dom.Element element) throws Exception {
        org.chip.ping.xml.cddm.ObjectFactory objectFactory = new org.chip.ping.xml.cddm.ObjectFactory();
        DataType dt = objectFactory.createDataType();
        dt.setBodyContentFormat("text/xml");
        BodyContentType content = objectFactory.createBodyContentType();
        content.setAny(element);
        dt.setBodyContent(content);
        return dt;
    }

    public DataType getDataType(Object body) throws Exception {

        JAXBContext context = JAXBContext
                .newInstance("org.chip.ping.xml.talk:org.chip.ping.xml.record:org.chip.ping.xml.pid:org.chip.ping.xml.cddm");
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        org.chip.ping.xml.cddm.ObjectFactory objectFactory = new org.chip.ping.xml.cddm.ObjectFactory();

        DataType dt = objectFactory.createDataType();
        dt.setBodyContentFormat("text/xml");

        BodyContentType content = objectFactory.createBodyContentType();

        marshaller.marshal(body, doc);
        content.setAny(doc.getDocumentElement());

        dt.setBodyContent(content);

        return dt;
    }

}
