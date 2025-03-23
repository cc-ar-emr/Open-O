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


package oscar.oscarRx.pageUtil;

import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.oscarehr.app.OAuth1Utils;
import org.oscarehr.common.dao.AppDefinitionDao;
import org.oscarehr.common.dao.AppUserDao;
import org.oscarehr.common.model.AppDefinition;
import org.oscarehr.common.model.AppUser;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import oscar.OscarProperties;
import oscar.oscarRx.util.TimingOutCallback;
import oscar.oscarRx.util.TimingOutCallback.TimeoutException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Deprecated do not use.  This class uses Sessions, drops streams, and causes corruption
 * with prescription writing.
 * MyDrugRef is no longer an active option.
 */
@Deprecated
public final class RxMyDrugrefInfoAction {

    private static final Logger log2 = MiscUtils.getLogger();

    public static void removeNullFromVector(Vector v) {
        while (v != null && v.contains(null)) {
            v.remove(null);
        }
    }

    public Vector getMyDrugrefInfo(LoggedInInfo loggedInInfo, String command, Vector drugs, String providerNo, String myDrugrefId) {

        removeNullFromVector(drugs);

        Vector vec = new Vector();
        log2.debug("CALL : FETCH:" + drugs);
        Object obj = callOAuthService(loggedInInfo, command, drugs, myDrugrefId);
        log2.debug("RETURNED " + obj);
        if (obj instanceof Vector) {

            vec = (Vector) obj;

        } else if (obj instanceof Hashtable) {

            Object holbrook = ((Hashtable) obj).get("Holbrook Drug Interactions");
            if (holbrook instanceof Vector) {

                vec = (Vector) holbrook;

            }
            Enumeration e = ((Hashtable) obj).keys();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();

                log2.debug(s + " " + ((Hashtable) obj).get(s) + " " + ((Hashtable) obj).get(s).getClass().getName());
            }
        }
        return vec;
    }

    public Vector callOAuthService(LoggedInInfo loggedInInfo, String procedureName, Vector params, String myDrugrefId) {
        try {
            AppDefinitionDao appDefinitionDao = SpringUtils.getBean(AppDefinitionDao.class);
            AppUserDao appUserDao = SpringUtils.getBean(AppUserDao.class);

            AppDefinition k2aApp = appDefinitionDao.findByName("K2A");

            boolean useXMLRPC = false;
            Vector result = null;

            if (k2aApp != null) {
                AppUser k2aUser = appUserDao.findForProvider(k2aApp.getId(), loggedInInfo.getLoggedInProviderNo());

                if (k2aUser != null) {

                    String requestURI = "/ws/api/" + procedureName;

                    String requestURIWithParams = null;
                    if (params != null && !params.isEmpty() && params.size() > 0) {
                        requestURIWithParams = requestURI + "?";
                        for (int i = 0; i < params.size(); i++) {
                            if (procedureName.contains("guidelines")) {
                                requestURIWithParams += "uuidCodes=" + params.get(i) + "&";
                            } else {
                                requestURIWithParams += "atcCodes=" + params.get(i) + "&";
                            }
                        }
                        requestURIWithParams = requestURIWithParams.substring(0, requestURIWithParams.length() - 1);
                    }


                    if (requestURIWithParams == null) {
                        requestURIWithParams = requestURI;
                    }

                    String jsonString = OAuth1Utils.getOAuthGetResponse(loggedInInfo, k2aApp, k2aUser, requestURIWithParams, requestURI);
                    //Convert JSON return to Vector/Hashtable
                    JSONArray jsonArray = new JSONArray();

                    if (jsonString != null && !jsonString.isEmpty()) {
                        jsonArray = new JSONArray(jsonString);
                        result = new Vector();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject eform = jsonArray.getJSONObject(i);
                            Hashtable drugInfo = new Hashtable();

                            Iterator iterator = eform.keys();
                            while (iterator.hasNext()) {
                                String key = (String) iterator.next();
                                if (key.equals("significance") || key.equals("version")) {
                                    drugInfo.put(key, eform.get(key).toString());
                                } else if (key.equals("updated_at") || key.equals("updatedAt") || key.equals("created_at")) {
                                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                                    Date date = formatter.parse(eform.get(key).toString());
                                    drugInfo.put(key, date);
                                } else {

                                    drugInfo.put(key.toLowerCase(), eform.get(key));
                                }
                            }

                            result.add(drugInfo);
                        }
                    }
                } else {
                    useXMLRPC = true;
                }
            } else {
                useXMLRPC = true;
            }

            if (useXMLRPC) {
                Vector newParams = new Vector();

                //Convert from OAuth procedure name to xml rpc procedure name
                if (procedureName.equals("atcfetch/getWarnings")) {
                    newParams.addElement("warnings_byATC");
                    procedureName = "Fetch";
                } else if (procedureName.equals("atcfetch/getBulletins")) {
                    newParams.addElement("bulletins_byATC");
                    procedureName = "Fetch";
                } else if (procedureName.equals("atcfetch/getInteractions")) {
                    newParams.addElement("interactions_byATC");
                    procedureName = "Fetch";
                } else if (procedureName.equals("guidelines/getGuidelineIds")) {
                    procedureName = "GetGuidelineIds";
                } else if (procedureName.equals("guidelines/getGuidelines")) {
                    procedureName = "GetGuidelines";
                }

                if (params != null) {
                    newParams.add(params);
                }

                if (myDrugrefId != null && !myDrugrefId.trim().equals("")) {
                    log2.debug("putting >" + myDrugrefId + "< in the request");
                    newParams.addElement(myDrugrefId);
                    //params.addElement("true");
                }
                log2.debug("#CALLmyDRUGREF-" + procedureName);
                Object object = null;

                String server_url = OscarProperties.getInstance().getProperty("MY_DRUGREF_URL", "http://know2act.org/backend/api");

                TimingOutCallback callback = new TimingOutCallback(10 * 1000);
                try {
                    log2.debug("server_url :" + server_url);
                    if (!System.getProperty("http.proxyHost", "").isEmpty()) {
                        //The Lite client won't recgonize JAVA_OPTS as it uses a customized http
                        XmlRpcClient server = new XmlRpcClient(server_url);
                        server.executeAsync(procedureName, newParams, callback);
                    } else {
                        XmlRpcClientLite server = new XmlRpcClientLite(server_url);
                        server.executeAsync(procedureName, newParams, callback);
                    }
                    object = callback.waitForResponse();
                } catch (TimeoutException e) {
                    log2.debug("No response from server." + server_url);
                } catch (Throwable ethrow) {
                    log2.debug("Throwing error." + ethrow.getMessage());
                }
                result = (Vector) object;
            }

            return result;
        } catch (Exception e) {
            log2.error("Failed to retrieve K2A drug ref information", e);
            return null;
        }
    }
}
