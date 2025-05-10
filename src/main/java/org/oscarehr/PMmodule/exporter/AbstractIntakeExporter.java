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
package org.oscarehr.PMmodule.exporter;                                                                                                                            
                                                                                                                                                                   
import java.io.InputStream;                                                                                                                                        
import java.util.ArrayList;                                                                                                                                        
import java.util.Collections;                                                                                                                                      
import java.util.List;                                                                                                                                             
                                                                                                                                                                   
import javax.xml.parsers.DocumentBuilder;                                                                                                                          
import javax.xml.parsers.DocumentBuilderFactory;                                                                                                                   
import javax.xml.parsers.ParserConfigurationException;                                                                                                             
                                                                                                                                                                   
import org.apache.commons.lang.StringUtils;                                                                                                                        
import org.apache.logging.log4j.Logger;                                                                                                                            
import org.oscarehr.PMmodule.model.Intake;                                                                                                                         
import org.oscarehr.PMmodule.model.IntakeAnswer;                                                                                                                   
import org.oscarehr.PMmodule.service.GenericIntakeManager;                                                                                                         
import org.oscarehr.util.MiscUtils;                                                                                                                                
import org.w3c.dom.Document;                                                                                                                                       
import org.w3c.dom.Element;                                                                                                                                        
import org.w3c.dom.NamedNodeMap;                                                                                                                                   
import org.w3c.dom.Node;                                                                                                                                           
import org.w3c.dom.NodeList;                                                                                                                                       
                                                                                                                                                                   
public abstract class AbstractIntakeExporter {                                                                                                                     
    private static final String FIELD_FIELD = "field";                                                                                                             
    private static final String FIELD_DESC = "desc";                                                                                                               
    private static final String FIELD_DATEFORMAT = "dateformat";                                                                                                   
    private static final String FIELD_MAXSIZE = "maxsize";                                                                                                         
    private static final String FIELD_COLPOS = "colpos";                                                                                                           
    private static final String FIELD_TYPE = "type";                                                                                                               
    private static final String FIELD_NAME = "name";                                                                                                               
    private static final String FIELD_QUESTION = "question";                                                                                                       
                                                                                                                                                                   
    private static final String ANSWER_DECLINED = "declined";                                                                                                      
                                                                                                                                                                   
    // Maximum number of clients to process in a single batch                                                                                                      
    private static final int MAX_CLIENTS_PER_BATCH = 1000;                                                                                                         
                                                                                                                                                                   
    private GenericIntakeManager genericIntakeManager;                                                                                                             
    private List<Integer> clients;                                                                                                                                 
    private Integer programId;                                                                                                                                     
    private Integer facilityId;                                                                                                                                    
    private String fieldsFile;                                                                                                                                     
                                                                                                                                                                   
    private IValidator validator;                                                                                                                                  
                                                                                                                                                                   
    protected Intake intake;                                                                                                                                       
    protected List<DATISField> fields;                                                                                                                             
                                                                                                                                                                   
    private static final Logger log = MiscUtils.getLogger();                                                                                                       
                                                                                                                                                                   
    public String export() throws ExportException {                                                                                                                
        StringBuilder buf = new StringBuilder();                                                                                                                   
                                                                                                                                                                   
        try {                                                                                                                                                      
            initExport();                                                                                                                                          
                                                                                                                                                                   
            if (clients == null || clients.isEmpty()) {                                                                                                            
                log.info("No clients to export");                                                                                                                  
                return "";                                                                                                                                         
            }                                                                                                                                                      
                                                                                                                                                                   
            // Log only the count, not any identifiable information                                                                                                
            log.info("Number of Clients to export: {}", clients.size());                                                                                           
                                                                                                                                                                   
            // Process clients in batches to prevent resource exhaustion                                                                                           
            for (int i = 0; i < clients.size(); i += MAX_CLIENTS_PER_BATCH) {                                                                                      
                int endIndex = Math.min(i + MAX_CLIENTS_PER_BATCH, clients.size());                                                                                
                List<Integer> clientBatch = clients.subList(i, endIndex);                                                                                          
                                                                                                                                                                   
                for (Integer clientId : clientBatch) {                                                                                                             
                    if (clientId == null) {                                                                                                                        
                        log.warn("Skipping null client ID");                                                                                                       
                        continue;                                                                                                                                  
                    }                                                                                                                                              
                                                                                                                                                                   
                    try {                                                                                                                                          
                        getIntakeForClient(clientId);                                                                                                              
                        if (intake != null) {                                                                                                                      
                            buf.append(exportData()).append("\n");                                                                                                 
                        } else {                                                                                                                                   
                            log.debug("No intake found for client ID: {}", clientId);                                                                              
                        }                                                                                                                                          
                    } catch (Exception e) {                                                                                                                        
                        log.error("Error processing client ID: " + clientId, e);                                                                                   
                        // Continue with next client instead of failing the entire export                                                                          
                    }                                                                                                                                              
                }                                                                                                                                                  
            }                                                                                                                                                      
        } catch (Exception e) {                                                                                                                                    
            throw new ExportException("Failed to complete export", e);                                                                                             
        }                                                                                                                                                          
                                                                                                                                                                   
        return buf.toString();                                                                                                                                     
    }                                                                                                                                                              
                                                                                                                                                                   
    protected abstract String exportData() throws ExportException;                                                                                                 
                                                                                                                                                                   
    private void initExport() throws ExportException {                                                                                                             
        try {                                                                                                                                                      
            fields = new ArrayList<DATISField>();                                                                                                                  
                                                                                                                                                                   
            if (StringUtils.isBlank(fieldsFile)) {                                                                                                                 
                throw new ExportException("Fields file path cannot be null or empty");                                                                             
            }                                                                                                                                                      
                                                                                                                                                                   
            loadFields(fieldsFile);                                                                                                                                
            log.debug("Fields loaded from file: {}", fieldsFile);                                                                                                  
                                                                                                                                                                   
            if (facilityId == null) {                                                                                                                              
                throw new ExportException("Facility ID cannot be null");                                                                                           
            }                                                                                                                                                      
                                                                                                                                                                   
            if (clients == null || clients.isEmpty()) {                                                                                                            
                // Get all clients for the specified facility...                                                                                                   
                log.debug("Fetching clients for facility ID: {}", facilityId);                                                                                     
                clients = genericIntakeManager.getIntakeClientsByFacilityId(facilityId);                                                                           
                                                                                                                                                                   
                // Defensive copy to prevent modification from outside                                                                                             
                if (clients != null) {                                                                                                                             
                    clients = new ArrayList<>(clients);                                                                                                            
                } else {                                                                                                                                           
                    clients = Collections.emptyList();                                                                                                             
                }                                                                                                                                                  
            }                                                                                                                                                      
                                                                                                                                                                   
        } catch (Exception t) {                                                                                                                                    
            throw new ExportException("Failed to initialize export", t);                                                                                           
        }                                                                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    private void getIntakeForClient(Integer clientId) throws ExportException {                                                                                     
        if (clientId == null) {                                                                                                                                    
            throw new ExportException("Client ID cannot be null");                                                                                                 
        }                                                                                                                                                          
                                                                                                                                                                   
        if (facilityId == null) {                                                                                                                                  
            throw new ExportException("Facility ID cannot be null");                                                                                               
        }                                                                                                                                                          
                                                                                                                                                                   
        try {                                                                                                                                                      
            intake = genericIntakeManager.getMostRecentQuickIntake(clientId, facilityId);                                                                          
        } catch (Exception t) {                                                                                                                                    
            throw new ExportException("Failed to get intake for client ID: " + clientId, t);                                                                       
        }                                                                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    private void loadFields(String fieldsFile) throws Exception {                                                                                                  
        if (StringUtils.isBlank(fieldsFile)) {                                                                                                                     
            throw new ExportException("Fields file path cannot be null or empty");                                                                                 
        }                                                                                                                                                          
                                                                                                                                                                   
        InputStream inputStream = null;                                                                                                                            
        try {                                                                                                                                                      
            inputStream = this.getClass().getResourceAsStream(fieldsFile);                                                                                         
            if (inputStream == null) {                                                                                                                             
                throw new ExportException("Could not find resource: " + fieldsFile);                                                                               
            }                                                                                                                                                      
                                                                                                                                                                   
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();                                                                                 
                                                                                                                                                                   
            // Disable external entity processing to prevent XXE attacks                                                                                           
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);                                                                      
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);                                                                    
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);                                                                  
            factory.setXIncludeAware(false);                                                                                                                       
            factory.setExpandEntityReferences(false);                                                                                                              
                                                                                                                                                                   
            DocumentBuilder builder = factory.newDocumentBuilder();                                                                                                
            Document document = builder.parse(inputStream);                                                                                                        
                                                                                                                                                                   
            loadData(document);                                                                                                                                    
        } catch (ParserConfigurationException e) {                                                                                                                 
            throw new ExportException("Failed to configure XML parser", e);                                                                                        
        } finally {                                                                                                                                                
            if (inputStream != null) {                                                                                                                             
                try {                                                                                                                                              
                    inputStream.close();                                                                                                                           
                } catch (Exception e) {                                                                                                                            
                    log.warn("Failed to close input stream", e);                                                                                                   
                }                                                                                                                                                  
            }                                                                                                                                                      
        }                                                                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    private void loadData(Document document) throws Exception {                                                                                                    
        if (document == null) {                                                                                                                                    
            throw new ExportException("Document cannot be null");                                                                                                  
        }                                                                                                                                                          
                                                                                                                                                                   
        Element fieldsE = document.getDocumentElement();                                                                                                           
        if (fieldsE == null) {                                                                                                                                     
            throw new ExportException("Document has no root element");                                                                                             
        }                                                                                                                                                          
                                                                                                                                                                   
        NodeList nodes = fieldsE.getChildNodes();                                                                                                                  
        for (int i = 0; i < nodes.getLength(); i++) {                                                                                                              
            Node fieldNode = nodes.item(i);                                                                                                                        
            if (fieldNode == null || !FIELD_FIELD.equals(fieldNode.getNodeName())) {                                                                               
                continue;                                                                                                                                          
            }                                                                                                                                                      
                                                                                                                                                                   
            NamedNodeMap attributes = fieldNode.getAttributes();                                                                                                   
            if (attributes == null) {                                                                                                                              
                continue;                                                                                                                                          
            }                                                                                                                                                      
                                                                                                                                                                   
            Node nameNode = attributes.getNamedItem(FIELD_NAME);                                                                                                   
            Node typeNode = attributes.getNamedItem(FIELD_TYPE);                                                                                                   
            Node colPosNode = attributes.getNamedItem(FIELD_COLPOS);                                                                                               
            Node maxSizeNode = attributes.getNamedItem(FIELD_MAXSIZE);                                                                                             
                                                                                                                                                                   
            if (nameNode == null || typeNode == null || colPosNode == null || maxSizeNode == null) {                                                               
                log.warn("Skipping field with missing required attributes");                                                                                       
                continue;                                                                                                                                          
            }                                                                                                                                                      
                                                                                                                                                                   
            try {                                                                                                                                                  
                DATISField field = new DATISField();                                                                                                               
                field.setName(nameNode.getNodeValue().toUpperCase());                                                                                              
                field.setType(typeNode.getNodeValue());                                                                                                            
                field.setColumnPosition(Integer.parseInt(colPosNode.getNodeValue()));                                                                              
                field.setMaxSize(Integer.parseInt(maxSizeNode.getNodeValue()));                                                                                    
                                                                                                                                                                   
                Node descNode = attributes.getNamedItem(FIELD_DESC);                                                                                               
                if (descNode != null) {                                                                                                                            
                    field.setDescription(descNode.getNodeValue());                                                                                                 
                }                                                                                                                                                  
                                                                                                                                                                   
                if (DATISType.DATETIME.equals(field.getType())) {                                                                                                  
                    Node dateFormatNode = attributes.getNamedItem(FIELD_DATEFORMAT);                                                                               
                    if (dateFormatNode != null) {                                                                                                                  
                        field.setDateFormat(dateFormatNode.getNodeValue());                                                                                        
                    }                                                                                                                                              
                }                                                                                                                                                  
                                                                                                                                                                   
                Node questionNode = attributes.getNamedItem(FIELD_QUESTION);                                                                                       
                if (questionNode != null) {                                                                                                                        
                    field.setQuestion(questionNode.getNodeValue().toUpperCase());                                                                                  
                }                                                                                                                                                  
                                                                                                                                                                   
                fields.add(field);                                                                                                                                 
            } catch (NumberFormatException e) {                                                                                                                    
                log.warn("Skipping field with invalid numeric attributes", e);                                                                                     
            }                                                                                                                                                      
        }                                                                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    public GenericIntakeManager getGenericIntakeManager() {                                                                                                        
        return genericIntakeManager;                                                                                                                               
    }                                                                                                                                                              
                                                                                                                                                                   
    public void setGenericIntakeManager(GenericIntakeManager genericIntakeManager) {                                                                               
        this.genericIntakeManager = genericIntakeManager;                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    public List<Integer> getClients() {                                                                                                                            
        return clients != null ? Collections.unmodifiableList(clients) : null;                                                                                     
    }                                                                                                                                                              
                                                                                                                                                                   
    public void setClients(List<Integer> clients) {                                                                                                                
        this.clients = clients != null ? new ArrayList<>(clients) : null;                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    public Integer getProgramId() {                                                                                                                                
        return programId;                                                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    public void setProgramId(Integer programId) {                                                                                                                  
        this.programId = programId;                                                                                                                                
    }                                                                                                                                                              
                                                                                                                                                                   
    public Integer getFacilityId() {                                                                                                                               
        return facilityId;                                                                                                                                         
    }                                                                                                                                                              
                                                                                                                                                                   
    public void setFacilityId(Integer facilityId) {                                                                                                                
        this.facilityId = facilityId;                                                                                                                              
    }                                                                                                                                                              
                                                                                                                                                                   
    public String getFieldsFile() {                                                                                                                                
        return fieldsFile;                                                                                                                                         
    }                                                                                                                                                              
                                                                                                                                                                   
    public void setFieldsFile(String fieldsFile) {                                                                                                                 
        this.fieldsFile = fieldsFile;                                                                                                                              
    }                                                                                                                                                              
                                                                                                                                                                   
    protected void writeData(StringBuilder buf, IntakeAnswer ans, DATISField found) {                                                                              
        if (buf == null || ans == null || found == null) {                                                                                                         
            return;                                                                                                                                                
        }                                                                                                                                                          
                                                                                                                                                                   
        String value = getFieldValue(ans, found);                                                                                                                  
        buf.append(value);                                                                                                                                         
    }                                                                                                                                                              
                                                                                                                                                                   
    protected void writeKeyValue(StringBuilder buf, IntakeAnswer ans, DATISField found) {                                                                          
        if (buf == null || ans == null || found == null) {                                                                                                         
            return;                                                                                                                                                
        }                                                                                                                                                          
                                                                                                                                                                   
        String value = getFieldValue(ans, found);                                                                                                                  
        buf.append(found.getName()).append(" = ").append(value).append("\n");                                                                                      
    }                                                                                                                                                              
                                                                                                                                                                   
    protected void writeCSV(StringBuilder buf, IntakeAnswer ans, DATISField field) {                                                                               
        if (buf == null || ans == null || field == null) {                                                                                                         
            return;                                                                                                                                                
        }                                                                                                                                                          
                                                                                                                                                                   
        String value = getFieldValue(ans, field);                                                                                                                  
                                                                                                                                                                   
        if (validator != null) {                                                                                                                                   
            value = validator.validate(field, value);                                                                                                              
        }                                                                                                                                                          
                                                                                                                                                                   
        buf.append(value).append(",");                                                                                                                             
    }                                                                                                                                                              
                                                                                                                                                                   
    private String getFieldValue(IntakeAnswer ans, DATISField field) {                                                                                             
        if (ans == null || field == null) {                                                                                                                        
            return StringUtils.rightPad("", field != null ? field.getMaxSize() : 0, ' ');                                                                          
        }                                                                                                                                                          
                                                                                                                                                                   
        String value = ans.getValue();                                                                                                                             
        if (value == null) {                                                                                                                                       
            value = "";                                                                                                                                            
        }                                                                                                                                                          
                                                                                                                                                                   
        if (ANSWER_DECLINED.equalsIgnoreCase(value)) {                                                                                                             
            value = " ";                                                                                                                                           
        }                                                                                                                                                          
                                                                                                                                                                   
        int maxSize = field.getMaxSize();                                                                                                                          
        if (value.length() > maxSize) {                                                                                                                            
            value = value.substring(0, maxSize);                                                                                                                   
        } else {                                                                                                                                                   
            value = StringUtils.rightPad(value, maxSize, ' ');                                                                                                     
        }                                                                                                                                                          
                                                                                                                                                                   
        return value;                                                                                                                                              
    }                                                                                                                                                              
                                                                                                                                                                   
    public IValidator getValidator() {                                                                                                                             
        return validator;                                                                                                                                          
    }                                                                                                                                                              
                                                                                                                                                                   
    public void setValidator(IValidator validator) {                                                                                                               
        this.validator = validator;                                                                                                                                
    }                                                                                                                                                              
}                                                                
