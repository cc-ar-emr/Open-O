package org.oscarehr.common.service.myoscar;

import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.oscarehr.common.dao.AllergyDao;
import org.oscarehr.common.dao.SentToPHRTrackingDao;
import org.oscarehr.common.model.Allergy;
import org.oscarehr.common.model.SentToPHRTracking;
import org.oscarehr.myoscar_server.ws.ItemAlreadyExistsException_Exception;
import org.oscarehr.myoscar_server.ws.ItemCompletedException_Exception;
import org.oscarehr.myoscar_server.ws.MedicalDataTransfer2;
import org.oscarehr.myoscar_server.ws.MedicalDataType;
import org.oscarehr.myoscar_server.ws.NoSuchItemException_Exception;
import org.oscarehr.myoscar_server.ws.NotAuthorisedException_Exception;
import org.oscarehr.phr.PHRAuthentication;
import org.oscarehr.phr.util.MyOscarServerWebServicesManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.oscarehr.util.XmlUtils;
import org.w3c.dom.Document;

public final class AllergiesManager {
	private static final Logger logger = MiscUtils.getLogger();
	private static final String OSCAR_ALLERGIES_DATA_TYPE = "ALLERGY";
	private static final SentToPHRTrackingDao sentToPHRTrackingDao = (SentToPHRTrackingDao) SpringUtils.getBean("sentToPHRTrackingDao");

	public static void sendAllergiesToMyOscar(PHRAuthentication auth, Integer demographicId) throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserConfigurationException, NotAuthorisedException_Exception, NoSuchItemException_Exception, ItemCompletedException_Exception {
		// get last synced info

		// get the items for the person which are changed since last sync
		// for each item
		// send the item or update it

		Date startSyncTime = new Date();
		SentToPHRTracking sentToPHRTracking = MyOscarMedicalDataManagerUtils.getExistingOrCreateInitialSentToPHRTracking(demographicId, OSCAR_ALLERGIES_DATA_TYPE, MyOscarServerWebServicesManager.getMyOscarServerBaseUrl());
		logger.debug("sendAllergiesToMyOscar : demographicId=" + demographicId + ", lastSyncTime=" + sentToPHRTracking.getSentDatetime());

		AllergyDao allergyDao = (AllergyDao) SpringUtils.getBean("allergyDao");
		List<Allergy> changedAllergies = allergyDao.findByDemographicIdUpdatedAfterDate(demographicId, sentToPHRTracking.getSentDatetime());
		for (Allergy allergy : changedAllergies) {
			logger.debug("sendAllergiesToMyOscar : allergyId=" + allergy.getId());

			MedicalDataTransfer2 medicalDataTransfer = toMedicalDataTransfer(auth, allergy);

			try {
				MyOscarMedicalDataManagerUtils.addMedicalData(auth, medicalDataTransfer, OSCAR_ALLERGIES_DATA_TYPE, allergy.getId());
			} catch (ItemAlreadyExistsException_Exception e) {
				MyOscarMedicalDataManagerUtils.updateMedicalData(auth, medicalDataTransfer, OSCAR_ALLERGIES_DATA_TYPE, allergy.getId());
			}
		}

		sentToPHRTracking.setSentDatetime(startSyncTime);
		sentToPHRTrackingDao.merge(sentToPHRTracking);
	}

	public static Document toXml(Allergy allergy) throws ParserConfigurationException {
		Document doc = XmlUtils.newDocument("Allergy");

		String temp = StringUtils.trimToNull(allergy.getDescription());
		if (temp != null) XmlUtils.appendChildToRootIgnoreNull(doc, "Description", temp);

		temp = StringUtils.trimToNull(allergy.getReaction());
		if (temp != null) XmlUtils.appendChildToRootIgnoreNull(doc, "Reaction", temp);

		Integer tempInt = allergy.getHiclSeqno();
		if (tempInt != null) XmlUtils.appendChildToRoot(doc, "HiclSeqno", tempInt.toString());

		tempInt = allergy.getHicSeqno();
		if (tempInt != null) XmlUtils.appendChildToRoot(doc, "HicSeqno", tempInt.toString());

		tempInt = allergy.getAgcsp();
		if (tempInt != null) XmlUtils.appendChildToRoot(doc, "Agcsp", tempInt.toString());

		tempInt = allergy.getAgccs();
		if (tempInt != null) XmlUtils.appendChildToRoot(doc, "Agccs", tempInt.toString());

		tempInt = allergy.getTypeCode();
		if (tempInt != null) XmlUtils.appendChildToRoot(doc, "TypeCode", tempInt.toString());

		temp = StringUtils.trimToNull(allergy.getDrugrefId());
		if (temp != null) XmlUtils.appendChildToRootIgnoreNull(doc, "DrugrefId", temp);

		if (allergy.getStartDate() != null) {
			temp = DateFormatUtils.ISO_DATETIME_FORMAT.format(allergy.getStartDate());
			XmlUtils.appendChildToRootIgnoreNull(doc, "StartDate", temp);
		}

		temp = StringUtils.trimToNull(allergy.getAgeOfOnset());
		if (temp != null) XmlUtils.appendChildToRootIgnoreNull(doc, "AgeOfOnset", temp);

		temp = StringUtils.trimToNull(allergy.getSeverityOfReaction());
		if (temp != null) {
			// not too worries about i18n, just sending something better than a number
			if ("1".equals(temp)) temp = "Mild";
			else if ("2".equals(temp)) temp = "Moderate";
			else if ("3".equals(temp)) temp = "Severe";
			else if ("4".equals(temp)) temp = "Unknown";

			XmlUtils.appendChildToRootIgnoreNull(doc, "SeverityOfReaction", temp);
		}

		temp = StringUtils.trimToNull(allergy.getOnsetOfReaction());
		if (temp != null) {
			// not too worries about i18n, just sending something better than a number
			if ("1".equals(temp)) temp = "Immediate";
			else if ("2".equals(temp)) temp = "Gradual";
			else if ("3".equals(temp)) temp = "Slow";
			else if ("4".equals(temp)) temp = "Unknown";

			XmlUtils.appendChildToRootIgnoreNull(doc, "OnsetOfReaction", temp);
		}

		temp = StringUtils.trimToNull(allergy.getRegionalIdentifier());
		if (temp != null) XmlUtils.appendChildToRootIgnoreNull(doc, "RegionalIdentifier", temp);

		temp = StringUtils.trimToNull(allergy.getLifeStage());
		if (temp != null) XmlUtils.appendChildToRootIgnoreNull(doc, "LifeStage", temp);

		return (doc);
	}

	/**
	 * This method may return null for invalid allergy entries... we have some of those, specifically when no provider can be identified to be responsible for this send.
	 */
	private static MedicalDataTransfer2 toMedicalDataTransfer(PHRAuthentication auth, Allergy allergy) throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserConfigurationException {

		// okay big anomaly here, some records do not have providers numbers. This is really invalid data.
		// Our attempt will be to check for a provider number, if none exists, we'll try to use the person who is sending - if it's a person
		// otherwise... we can't do anything, we can't set this to null, it makes no sense from a thread, at least if its clicked by a person they have double checked it.
		String providerNo = allergy.getProviderNo();
		LoggedInInfo loggedInInfo = LoggedInInfo.loggedInInfo.get();

		if (providerNo == null) {
			if (loggedInInfo.loggedInProvider != null) // in case it's background thread.
			{
				providerNo = loggedInInfo.loggedInProvider.getProviderNo();
			}
		}

		if (providerNo != null) {
			MedicalDataTransfer2 medicalDataTransfer = MyOscarMedicalDataManagerUtils.getEmptyMedicalDataTransfer2(auth, allergy.getEntryDate(), providerNo, allergy.getDemographicNo());
			// don't ask me why but allergies are currently changeable in oscar, therefore, they're never completed.
			medicalDataTransfer.setCompleted(false);

			Document doc = toXml(allergy);
			medicalDataTransfer.setData(XmlUtils.toString(doc, false));

			medicalDataTransfer.setMedicalDataType(MedicalDataType.ALLERGY.name());

			medicalDataTransfer.setOriginalSourceId(MyOscarMedicalDataManagerUtils.generateSourceId(loggedInInfo.currentFacility.getName(), OSCAR_ALLERGIES_DATA_TYPE, allergy.getId()));

			boolean active = true;
			if (allergy.getArchived() != null && "1".equals(allergy.getArchived())) active = false;
			medicalDataTransfer.setActive(active);

			return (medicalDataTransfer);
		} else {
			return (null);
		}

	}

}