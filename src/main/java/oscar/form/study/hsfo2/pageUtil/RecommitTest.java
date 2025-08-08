//CHECKSTYLE:OFF
/**
 * Copyright (C) 2007  Heart & Stroke Foundation
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

package oscar.form.study.hsfo2.pageUtil;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import oscar.form.study.hsfo2.pageUtil.XMLTransferUtil.SoapElementKey;

public class RecommitTest {
    public static void main(String argvs[]) {
        sendTestSoap();
    }

    public static void recommit() {
        try {
            new RecommitHSFO2Action.ResubmitJob().execute(null);
        } catch (Exception exception) {
            ;
        }

    }

    public static void sendTestSoap() {
        final String webUrl = "https://www.clinforma.net/P-Prompt/DataReceiveTestWS/";
//    final String getDataDateRangeAction = "https://www.clinforma.net/P-Prompt/DataReceiveTestWS/GetDataDateRange";
        final String namespace = "https://www.clinforma.net/P-Prompt/DataReceiveWS/";
        final String siteCode = "999";
        final String userId = "oscar";
        final String passwd = "first9candy";
        int fileType = 18;

        HttpPost post = new HttpPost(webUrl);
//    post.setRequestHeader( "SOAPAction", getDataDateRangeAction );    //don't set soap action
        post.setHeader("Content-Type", "text/xml; charset=utf-8");

        String soapMsg =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"https://www.clinforma.net/P-Prompt/DataReceiveWS/\">"
                        + "<soapenv:Header/>"
                        + "<soapenv:Body>"
                        + "<dat:GetDataDateRange>"
                        + "  <dat:Site>" + siteCode + "</dat:Site>"
                        + "  <dat:UserID>" + userId + "</dat:UserID>"
                        + "  <dat:Password>" + passwd + "</dat:Password>"
                        + "  <dat:FileType>" + fileType + "</dat:FileType>"
                        + "</dat:GetDataDateRange>"
                        + "</soapenv:Body>"
                        + "</soapenv:Envelope>";


        /***
         String soapMsg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
         + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
         + "soapenv:Body"        //instead of "<soap-env:Body>"
         + "<GetDataDateRange xmlns=\"" + namespace + "\">"
         + "<Site>" + siteCode + "</Site>" + "<UserID>" + userId + "</UserID>" + "<Password>" + passwd + "</Password>"
         //      + "<FileType>" + fileType + "</FileType>"
         + "</GetDataDateRange>"
         + "</soapenv:Body>"      //"</soap:Body>"
         + "</soap:Envelope>";
         **/
        // Execute request
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            StringEntity entity = new StringEntity(soapMsg, "UTF-8");
            entity.setContentType("text/xml");
            post.setEntity(entity);

            try (CloseableHttpResponse response = httpclient.execute(post)) {
                int result = response.getStatusLine().getStatusCode();

                String rsXml = null;
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    rsXml = EntityUtils.toString(responseEntity, "UTF-8");
                }

                Map<SoapElementKey, Object> output = new HashMap<>();
                output.put(SoapElementKey.responseStatusCode, result);

                log("http response code: " + result);
                log("result: " + XMLTransferUtil.getElementValue(rsXml, "GetDataDateRangeResult"));
                log("response: " + rsXml);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String info) {


    }
}
