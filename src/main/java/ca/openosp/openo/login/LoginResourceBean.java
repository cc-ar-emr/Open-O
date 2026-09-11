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
package ca.openosp.openo.login;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import ca.openosp.openo.commn.model.SystemPreferences;
import ca.openosp.openo.commn.service.AcceptableUseAgreementManager;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.utility.MiscUtils;

import ca.openosp.openo.utility.SpringUtils;
import ca.openosp.openo.utility.SSOUtility;
import ca.openosp.OscarProperties;

public class LoginResourceBean {

    private String supportLink;
    private String supportText;
    private String clinicText;
    private String clinicLink;
    private String clinicName;
    private String supportName;
    private String tabName;
    private String buildTag;
    private String ssoLoginUrl;
    private boolean ssoEnabled;
    private boolean oneIdEnabled;

    public boolean isSsoEnabled() {
        return ssoEnabled;
    }

    public void setSsoEnabled(boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    private final String ssoCallbackUrl = "#!" + "/ssoLogin.do" + "?loginStart=";

    private AcceptableUseAgreementManager acceptableUseAgreementManager;

    public LoginResourceBean() {
        OscarProperties oscarProperties = OscarProperties.getInstance();
        String oscarDocuments = oscarProperties.getProperty("BASE_DOCUMENT_DIR") + File.separator + "login";
        Properties loginProperties = new Properties();
        File propertiesFile = new File(oscarDocuments, ".env");

        if (propertiesFile.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
                loginProperties.load(fileInputStream);
            } catch (Exception e) {
                MiscUtils.getLogger().warn("Problem with fetching login resources " + e);
            }
        }

        if (loginProperties.containsKey("clinicText")) {
            this.clinicText = loginProperties.getProperty("clinicText");
        }

        if (loginProperties.containsKey("clinicLink")) {
            this.clinicLink = loginProperties.getProperty("clinicLink");
        }

        if (loginProperties.containsKey("clinicName")) {
            this.clinicName = loginProperties.getProperty("clinicName");
        }

        if (loginProperties.containsKey("supportName")) {
            this.supportName = loginProperties.getProperty("supportName");
        }

        if (loginProperties.containsKey("supportText")) {
            this.supportText = loginProperties.getProperty("supportText");
        }

        if (loginProperties.containsKey("supportLink")) {
            this.supportLink = loginProperties.getProperty("supportLink");
        }

        if (loginProperties.containsKey("tabName")) {
            this.tabName = loginProperties.getProperty("tabName");
        }

        this.buildTag = OscarProperties.getBuildDate() + " " + OscarProperties.getBuildTag();

        this.acceptableUseAgreementManager = new AcceptableUseAgreementManager();

        setSsoEnabled(SSOUtility.isSSOEnabled());
        this.oneIdEnabled = readOneIdEnabled();
    }

    /**
     * Whether the ONE ID sign-in button is shown, from the {@code oneid_enabled} setting.
     *
     * <p>Wrapped because this runs while the sign-in page is being built: a settings read that
     * failed - no row yet on an instance that has not run the migration, or a database the page
     * cannot reach - must hide the ONE ID button, not take the whole login screen down with it.</p>
     *
     * @return boolean true when ONE ID sign-in is switched on, false when it is off or unreadable
     */
    private boolean readOneIdEnabled() {
        try {
            EhrConnectivityManager ehrConnectivityManager =
                    SpringUtils.getBean(EhrConnectivityManager.class);
            return ehrConnectivityManager.getConfigFlag(
                    SystemPreferences.ONEID_KEYS.oneid_enabled, false);
        } catch (Exception e) {
            MiscUtils.getLogger().warn("Could not read the ONE ID sign-in setting; hiding the button", e);
            return false;
        }
    }

    public boolean isOneIdEnabled() {
        return oneIdEnabled;
    }

    public void setOneIdEnabled(boolean oneIdEnabled) {
        this.oneIdEnabled = oneIdEnabled;
    }

    public String getSupportLink() {
        return supportLink;
    }

    public void setSupportLink(@SuppressWarnings("unused") String supportLink) {
        this.supportLink = null;
    }

    public String getSupportText() {
        return supportText;
    }

    public void setSupportText(@SuppressWarnings("unused") String supportText) {
        this.supportText = null;
    }

    public String getClinicText() {
        return clinicText;
    }

    public void setClinicText(@SuppressWarnings("unused") String clinicText) {
        this.clinicText = null;
    }

    public String getClinicLink() {
        return clinicLink;
    }

    public void setClinicLink(@SuppressWarnings("unused") String clinicLink) {
        this.clinicLink = null;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(@SuppressWarnings("unused") String clinicName) {
        this.clinicName = null;
    }

    public String getBuildTag() {
        return buildTag;
    }

    public void setBuildTag(@SuppressWarnings("unused") String buildTag) {
        this.buildTag = null;
    }

    public AcceptableUseAgreementManager getAcceptableUseAgreementManager() {
        return acceptableUseAgreementManager;
    }

    public void setAcceptableUseAgreementManager(@SuppressWarnings("unused") AcceptableUseAgreementManager acceptableUseAgreementManager) {
        this.acceptableUseAgreementManager = null;
    }

    public String getSupportName() {
        return supportName;
    }

    public void setSupportName(@SuppressWarnings("unused") String supportName) {
        this.supportName = null;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public String getSsoLoginUrl() {
        return ssoLoginUrl;
    }

    public void setSsoLoginUrl(String ssoLoginUrl) {
        this.ssoLoginUrl = ssoLoginUrl;
    }
}
