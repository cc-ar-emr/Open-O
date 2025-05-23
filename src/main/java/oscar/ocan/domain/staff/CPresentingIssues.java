//CHECKSTYLE:OFF
/**
 * Copyright (c) 2005-2012. Centre for Research on Inner City Health, St. Michael's Hospital, Toronto. All Rights Reserved.
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
 * This software was written for
 * Centre for Research on Inner City Health, St. Michael's Hospital,
 * Toronto, Ontario, Canada
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-793 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.05.24 at 10:52:14 PM EDT 
//


package oscar.ocan.domain.staff;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}CThreat_to_others_attempted_suicide"/>
 *         &lt;element ref="{}CSpecific_symptom_of_serious_mental_illness"/>
 *         &lt;element ref="{}CPhysical_sexual_abuse"/>
 *         &lt;element ref="{}CEducational"/>
 *         &lt;element ref="{}COccupational_Employment_Vocational"/>
 *         &lt;element ref="{}CHousing"/>
 *         &lt;element ref="{}CFinancial"/>
 *         &lt;element ref="{}CLegal"/>
 *         &lt;element ref="{}CProblems_with_relationships"/>
 *         &lt;element ref="{}CProblems_with_substance_abuse_addictions"/>
 *         &lt;element ref="{}CActivities_of_daily_living"/>
 *         &lt;element ref="{}COther"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "cThreatToOthersAttemptedSuicide",
        "cSpecificSymptomOfSeriousMentalIllness",
        "cPhysicalSexualAbuse",
        "cEducational",
        "cOccupationalEmploymentVocational",
        "cHousing",
        "cFinancial",
        "cLegal",
        "cProblemsWithRelationships",
        "cProblemsWithSubstanceAbuseAddictions",
        "cActivitiesOfDailyLiving",
        "cOther"
})
@XmlRootElement(name = "CPresenting_Issues")
public class CPresentingIssues {

    @XmlElement(name = "CThreat_to_others_attempted_suicide", required = true)
    protected String cThreatToOthersAttemptedSuicide;
    @XmlElement(name = "CSpecific_symptom_of_serious_mental_illness", required = true)
    protected String cSpecificSymptomOfSeriousMentalIllness;
    @XmlElement(name = "CPhysical_sexual_abuse", required = true)
    protected String cPhysicalSexualAbuse;
    @XmlElement(name = "CEducational", required = true)
    protected String cEducational;
    @XmlElement(name = "COccupational_Employment_Vocational", required = true)
    protected String cOccupationalEmploymentVocational;
    @XmlElement(name = "CHousing", required = true)
    protected String cHousing;
    @XmlElement(name = "CFinancial", required = true)
    protected String cFinancial;
    @XmlElement(name = "CLegal", required = true)
    protected String cLegal;
    @XmlElement(name = "CProblems_with_relationships", required = true)
    protected String cProblemsWithRelationships;
    @XmlElement(name = "CProblems_with_substance_abuse_addictions", required = true)
    protected String cProblemsWithSubstanceAbuseAddictions;
    @XmlElement(name = "CActivities_of_daily_living", required = true)
    protected String cActivitiesOfDailyLiving;
    @XmlElement(name = "COther", required = true)
    protected COther cOther;

    /**
     * Gets the value of the cThreatToOthersAttemptedSuicide property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCThreatToOthersAttemptedSuicide() {
        return cThreatToOthersAttemptedSuicide;
    }

    /**
     * Sets the value of the cThreatToOthersAttemptedSuicide property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCThreatToOthersAttemptedSuicide(String value) {
        this.cThreatToOthersAttemptedSuicide = value;
    }

    /**
     * Gets the value of the cSpecificSymptomOfSeriousMentalIllness property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCSpecificSymptomOfSeriousMentalIllness() {
        return cSpecificSymptomOfSeriousMentalIllness;
    }

    /**
     * Sets the value of the cSpecificSymptomOfSeriousMentalIllness property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCSpecificSymptomOfSeriousMentalIllness(String value) {
        this.cSpecificSymptomOfSeriousMentalIllness = value;
    }

    /**
     * Gets the value of the cPhysicalSexualAbuse property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCPhysicalSexualAbuse() {
        return cPhysicalSexualAbuse;
    }

    /**
     * Sets the value of the cPhysicalSexualAbuse property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCPhysicalSexualAbuse(String value) {
        this.cPhysicalSexualAbuse = value;
    }

    /**
     * Gets the value of the cEducational property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCEducational() {
        return cEducational;
    }

    /**
     * Sets the value of the cEducational property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCEducational(String value) {
        this.cEducational = value;
    }

    /**
     * Gets the value of the cOccupationalEmploymentVocational property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCOccupationalEmploymentVocational() {
        return cOccupationalEmploymentVocational;
    }

    /**
     * Sets the value of the cOccupationalEmploymentVocational property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCOccupationalEmploymentVocational(String value) {
        this.cOccupationalEmploymentVocational = value;
    }

    /**
     * Gets the value of the cHousing property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCHousing() {
        return cHousing;
    }

    /**
     * Sets the value of the cHousing property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCHousing(String value) {
        this.cHousing = value;
    }

    /**
     * Gets the value of the cFinancial property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCFinancial() {
        return cFinancial;
    }

    /**
     * Sets the value of the cFinancial property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCFinancial(String value) {
        this.cFinancial = value;
    }

    /**
     * Gets the value of the cLegal property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCLegal() {
        return cLegal;
    }

    /**
     * Sets the value of the cLegal property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCLegal(String value) {
        this.cLegal = value;
    }

    /**
     * Gets the value of the cProblemsWithRelationships property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCProblemsWithRelationships() {
        return cProblemsWithRelationships;
    }

    /**
     * Sets the value of the cProblemsWithRelationships property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCProblemsWithRelationships(String value) {
        this.cProblemsWithRelationships = value;
    }

    /**
     * Gets the value of the cProblemsWithSubstanceAbuseAddictions property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCProblemsWithSubstanceAbuseAddictions() {
        return cProblemsWithSubstanceAbuseAddictions;
    }

    /**
     * Sets the value of the cProblemsWithSubstanceAbuseAddictions property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCProblemsWithSubstanceAbuseAddictions(String value) {
        this.cProblemsWithSubstanceAbuseAddictions = value;
    }

    /**
     * Gets the value of the cActivitiesOfDailyLiving property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCActivitiesOfDailyLiving() {
        return cActivitiesOfDailyLiving;
    }

    /**
     * Sets the value of the cActivitiesOfDailyLiving property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCActivitiesOfDailyLiving(String value) {
        this.cActivitiesOfDailyLiving = value;
    }

    /**
     * Gets the value of the cOther property.
     *
     * @return possible object is
     * {@link COther }
     */
    public COther getCOther() {
        return cOther;
    }

    /**
     * Sets the value of the cOther property.
     *
     * @param value allowed object is
     *              {@link COther }
     */
    public void setCOther(COther value) {
        this.cOther = value;
    }

}
