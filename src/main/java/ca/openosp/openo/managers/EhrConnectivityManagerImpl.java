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
package ca.openosp.openo.managers;

import ca.openosp.openo.commn.dao.OMDGatewayTransactionLogDao;
import ca.openosp.openo.commn.dao.SecurityDao;
import ca.openosp.openo.commn.dao.SystemPreferencesDao;
import ca.openosp.openo.commn.dao.UAODao;
import ca.openosp.openo.commn.dao.UserPropertyDAO;
import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.commn.model.Security;
import ca.openosp.openo.commn.model.SystemPreferences;
import ca.openosp.openo.commn.model.UAO;
import ca.openosp.openo.commn.model.UserProperty;
import ca.openosp.openo.integration.oneId.OneIdSession;
import ca.openosp.openo.integration.oneId.OneIdSessionDao;
import ca.openosp.openo.integration.oneId.OneIdViewlet;
import ca.openosp.openo.integration.oneId.OneIdViewletDao;
import ca.openosp.openo.utility.LoggedInInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Default {@code EhrConnectivityManager} backed by {@code SystemPreferencesDao} and
 * {@code OMDGatewayTransactionLogDao}. Blank or whitespace-only configuration values are treated as
 * absent for the default-returning reads.
 *
 * @since 2026-07-01
 */
@Service
public class EhrConnectivityManagerImpl implements EhrConnectivityManager {

    @Autowired
    private SystemPreferencesDao systemPreferencesDao;

    @Autowired
    private OMDGatewayTransactionLogDao transactionLogDao;

    @Autowired
    private SecurityDao securityDao;

    @Autowired
    private OneIdSessionDao oneIdSessionDao;

    @Autowired
    private UAODao uaoDao;

    @Autowired
    private OneIdViewletDao oneIdViewletDao;

    @Autowired
    private UserPropertyDAO userPropertyDAO;

    @Autowired
    private SecurityInfoManager securityInfoManager;

    private static final String MULTI_WINDOW_NOTICE_PROPERTY = "oneid.multiwindow.notice";

    @Override
    public SystemPreferences getConfig(Enum<?> key) {
        return systemPreferencesDao.findPreferenceByName(key);
    }

    @Override
    public String getConfigValue(Enum<?> key) {
        SystemPreferences pref = getConfig(key);
        return pref == null ? null : pref.getValue();
    }

    @Override
    public String getConfigValue(Enum<?> key, String defaultValue) {
        String value = getConfigValue(key);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    @Override
    public boolean getConfigFlag(Enum<?> key, boolean defaultValue) {
        String value = getConfigValue(key);
        return (value != null && !value.trim().isEmpty()) ? "true".equals(value.trim()) : defaultValue;
    }

    @Override
    public SystemPreferences saveConfig(LoggedInInfo loggedInInfo, Enum<?> key, String value) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.WRITE);
        SystemPreferences pref = systemPreferencesDao.findPreferenceByName(key);
        if (pref == null) {
            pref = new SystemPreferences(key.name(), value);
            systemPreferencesDao.persist(pref);
        } else {
            pref.setValue(value);
            pref.setUpdateDate(new Date());
            systemPreferencesDao.merge(pref);
        }
        return pref;
    }

    @Override
    public List<OMDGatewayTransactionLog> getRecentLogs(LoggedInInfo loggedInInfo, String providerNo, String externalSystem, int maxRows) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.READ);
        // Both filters are applied. Returning on the first one set would list rows that contradict
        // the other, which the screen redisplays as though it had been used.
        return transactionLogDao.find(providerNo, externalSystem, maxRows);
    }

    @Override
    public List<Security> findProvidersByOneId(String subject) {
        return securityDao.findByOneIdKey(subject);
    }

    @Override
    public void saveOneIdSession(OneIdSession oneIdSession) {
        // The provider number is the assigned id, so this writes a provider's first session and
        // replaces a later one without needing to know which it is.
        oneIdSessionDao.merge(oneIdSession);
    }

    @Override
    public void removeOneIdSession(LoggedInInfo loggedInInfo, String providerNo) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        OneIdSession existing = oneIdSessionDao.find(providerNo);
        if (existing != null) {
            oneIdSessionDao.remove(existing);
        }
    }

    @Override
    public void removeOwnOneIdSession(LoggedInInfo loggedInInfo) {
        String providerNo = (loggedInInfo == null) ? null : loggedInInfo.getLoggedInProviderNo();
        if (providerNo == null) {
            throw new SecurityException("no acting provider to clear a ONE ID session for");
        }
        OneIdSession existing = oneIdSessionDao.find(providerNo);
        if (existing != null) {
            oneIdSessionDao.remove(existing);
        }
    }

    @Override
    public boolean clearOneIdBinding(LoggedInInfo loggedInInfo, String providerNo) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        Security securityRecord = securityDao.getByProviderNo(providerNo);
        if (securityRecord == null) {
            return false;
        }
        securityRecord.setOneIdKey(null);
        securityRecord.setOneIdEmail(null);
        securityDao.updateOneIdKey(securityRecord);
        return true;
    }

    @Override
    public List<UAO> findUaosByProvider(LoggedInInfo loggedInInfo, String providerNo) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.READ);
        return uaoDao.findByProvider(providerNo);
    }

    @Override
    public UAO findUao(LoggedInInfo loggedInInfo, Integer id) {
        UAO uao = uaoDao.find(id);
        if (uao == null) {
            return null;
        }
        checkProviderAccess(loggedInInfo, uao.getProviderNo(), SecurityInfoManager.READ);
        return uao;
    }

    @Override
    public void createUao(LoggedInInfo loggedInInfo, UAO uao) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.WRITE);
        uaoDao.persist(uao);
    }

    @Override
    public void updateUao(LoggedInInfo loggedInInfo, UAO uao) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.WRITE);
        uaoDao.merge(uao);
    }

    @Override
    public void setDefaultUao(LoggedInInfo loggedInInfo, UAO uao, String providerNo) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        uaoDao.setAsDefault(uao, providerNo);
    }

    @Override
    public void setSessionUao(LoggedInInfo loggedInInfo, String providerNo, String uaoValue, String uaoFriendlyName) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        OneIdSession session = oneIdSessionDao.find(providerNo);
        if (session != null) {
            session.setUaoUpi(uaoValue);
            session.setUaoName(uaoFriendlyName);
            oneIdSessionDao.merge(session);
        }
    }

    @Override
    public OneIdSession findOneIdSession(LoggedInInfo loggedInInfo, String providerNo) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.READ);
        return oneIdSessionDao.find(providerNo);
    }

    @Override
    public boolean clearSessionUaoIfWithdrawn(LoggedInInfo loggedInInfo, String providerNo, String uaoValue) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        return oneIdSessionDao.clearSessionUaoIfWithdrawn(providerNo, uaoValue) > 0;
    }

    @Override
    public void setSessionHubTopic(LoggedInInfo loggedInInfo, String providerNo, String hubTopic) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        OneIdSession session = oneIdSessionDao.find(providerNo);
        if (session != null) {
            session.setHubTopic(hubTopic);
            oneIdSessionDao.merge(session);
        }
    }

    @Override
    public void setOwnSessionHubTopic(LoggedInInfo loggedInInfo, String hubTopic) {
        String providerNo = (loggedInInfo == null) ? null : loggedInInfo.getLoggedInProviderNo();
        if (providerNo == null) {
            throw new SecurityException("no acting provider to record a hub topic for");
        }
        OneIdSession session = oneIdSessionDao.find(providerNo);
        if (session != null) {
            session.setHubTopic(hubTopic);
            oneIdSessionDao.merge(session);
        }
    }

    @Override
    public List<OneIdViewlet> findAllViewlets(LoggedInInfo loggedInInfo) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.READ);
        return oneIdViewletDao.findAllOrderByName();
    }

    @Override
    public OneIdViewlet findViewlet(LoggedInInfo loggedInInfo, Integer id) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.READ);
        return oneIdViewletDao.find(id);
    }

    @Override
    public void createViewlet(LoggedInInfo loggedInInfo, OneIdViewlet viewlet) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.WRITE);
        oneIdViewletDao.persist(viewlet);
    }

    @Override
    public void updateViewlet(LoggedInInfo loggedInInfo, OneIdViewlet viewlet) {
        checkPrivilege(loggedInInfo, SecurityInfoManager.WRITE);
        oneIdViewletDao.merge(viewlet);
    }

    @Override
    public List<OneIdViewlet> findEchartViewlets(LoggedInInfo loggedInInfo) {
        checkEhrAccess(loggedInInfo, SecurityInfoManager.READ);
        List<OneIdViewlet> viewlets = oneIdViewletDao.findAllActiveAndShowInEchartTrue();
        return viewlets == null ? new ArrayList<>() : viewlets;
    }

    @Override
    public OneIdViewlet findActiveViewletByKey(LoggedInInfo loggedInInfo, String key) {
        checkEhrAccess(loggedInInfo, SecurityInfoManager.READ);
        OneIdViewlet viewlet = oneIdViewletDao.queryOneIdViewletForKey(key);
        if (viewlet == null || viewlet.isDeleted()) {
            return null;
        }
        return viewlet;
    }

    /**
     * Guards the provider-facing use of an EHR service: listing the services on a chart and
     * launching one.
     *
     * <p>Use is {@code _ehr.connectivity}, not {@code _admin.ehrConnectivity}. The two objects
     * split configuring the gateway from using it, and a launch is use: it puts a patient into the
     * Ontario Health CMS context and opens their record in an external service. Setting up
     * endpoints and registering viewlets does not carry that.
     *
     * @param loggedInInfo LoggedInInfo the acting provider session
     * @param privilege String the right being asked for
     */
    private void checkEhrAccess(LoggedInInfo loggedInInfo, String privilege) {
        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_ehr.connectivity", privilege, null)) {
            throw new SecurityException("missing required sec object (_ehr.connectivity)");
        }
    }

    private void checkPrivilege(LoggedInInfo loggedInInfo, String privilege) {
        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_admin.ehrConnectivity", privilege, null)) {
            throw new SecurityException("missing required sec object (_admin.ehrConnectivity)");
        }
    }

    @Override
    public boolean isMultiWindowNoticeEnabled(LoggedInInfo loggedInInfo, String providerNo) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.READ);
        return readMultiWindowNotice(providerNo);
    }

    @Override
    public boolean setMultiWindowNoticeEnabled(LoggedInInfo loggedInInfo, String providerNo, boolean enabled) {
        checkProviderAccess(loggedInInfo, providerNo, SecurityInfoManager.WRITE);
        boolean previous = readMultiWindowNotice(providerNo);
        userPropertyDAO.saveProp(providerNo, MULTI_WINDOW_NOTICE_PROPERTY, String.valueOf(enabled));
        return previous;
    }

    private boolean readMultiWindowNotice(String providerNo) {
        UserProperty property = userPropertyDAO.getProp(providerNo, MULTI_WINDOW_NOTICE_PROPERTY);
        return property == null || !"false".equalsIgnoreCase(property.getValue());
    }

    private void checkProviderAccess(LoggedInInfo loggedInInfo, String targetProviderNo, String privilege) {
        if (securityInfoManager.hasPrivilege(loggedInInfo, "_admin.ehrConnectivity", privilege, null)) {
            return;
        }
        String actingProviderNo = (loggedInInfo == null) ? null : loggedInInfo.getLoggedInProviderNo();
        if (actingProviderNo != null
                && actingProviderNo.equals(targetProviderNo)
                && securityInfoManager.hasPrivilege(loggedInInfo, "_ehr.connectivity", privilege, null)) {
            return;
        }
        throw new SecurityException(
                "missing required sec object (_admin.ehrConnectivity, or _ehr.connectivity on own provider)");
    }
}
