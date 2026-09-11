//CHECKSTYLE:OFF
package ca.openosp.openo.documentManager;

import ca.openosp.openo.commn.dao.ConsultDocsDao;
import ca.openosp.openo.commn.dao.EFormDocsDao;
import ca.openosp.openo.commn.dao.TicklerDocsDao;
import ca.openosp.openo.commn.model.ConsultDocs;
import ca.openosp.openo.commn.model.EFormDocs;
import ca.openosp.openo.commn.model.TicklerDocs;
import ca.openosp.openo.commn.model.enumerator.DocumentType;
import ca.openosp.openo.utility.SpringUtils;

import ca.openosp.openo.encounter.oceanEReferal.pageUtil.OceanEReferralAttachmentUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocumentAttach {
    private final ConsultDocsDao consultDocsDao = SpringUtils.getBean(ConsultDocsDao.class);
    private final EFormDocsDao eFormDocsDao = SpringUtils.getBean(EFormDocsDao.class);
    private final TicklerDocsDao ticklerDocsDao = SpringUtils.getBean(TicklerDocsDao.class);

    /*
     * When editOnOcean is set to false, it signifies a normal consult request, performing just attach or detach operations on the consult request form.
     * When editOnOcean is set to true, it signifies that the attach or detach operation is being performed on a consult request created by OceanMD.
     * In this case, it will do two things:
     * 1. Attach or detach attachments from the consult request.
     * 2. Add those new attachments to the 'EreferAttachment' table, so Oscar can sent those attachment to OceanMD.
     * By doing this, the user will not have to manually upload new attachments to e-refer. They will be automatically fetched.
     */
    private Boolean editOnOcean = false;

    private Integer demographicNo;

    public DocumentAttach() {
    }

    public DocumentAttach(Integer demographicNo, Boolean editOnOcean) {
        this.demographicNo = demographicNo;
        this.editOnOcean = editOnOcean;
    }

    public void attachToConsult(String[] attachments, DocumentType documentType, String providerNo, Integer requestId) {
        List<String> currentList = new ArrayList<>(Arrays.asList(attachments));
        List<ConsultDocs> consultDocsList = consultDocsDao.findByRequestIdDocType(requestId, documentType.getType());
        List<String> oldList = new ArrayList<>();
        for (ConsultDocs consultDoc : consultDocsList) {
            oldList.add(Integer.toString(consultDoc.getDocumentNo()));
        }
        detachFromConsult(currentList, oldList, documentType, requestId);
        attachToConsult(currentList, oldList, documentType, providerNo, requestId);
    }

    private void attachToConsult(List<String> currentList, List<String> oldList, DocumentType documentType, String providerNo, Integer requestId) {
        for (String docId : currentList) {
            if (oldList.contains(docId)) {
                continue;
            }
            ConsultDocs consultDoc = new ConsultDocs(requestId, Integer.parseInt(docId), documentType.getType(), providerNo);
            consultDocsDao.persist(consultDoc);

            if (editOnOcean) {
                OceanEReferralAttachmentUtil.attachOceanEReferralConsult(docId, demographicNo, documentType.getType());
            }
        }
    }

    private void detachFromConsult(List<String> currentList, List<String> oldList, DocumentType documentType, Integer requestId) {
        for (String docId : oldList) {
            if (currentList.contains(docId)) {
                continue;
            }
            List<ConsultDocs> detachList = consultDocsDao.findByRequestIdDocNoDocType(requestId, Integer.valueOf(docId), documentType.getType());
            for (ConsultDocs consultDoc : detachList) {
                consultDoc.setDeleted("Y");
                consultDocsDao.merge(consultDoc);
            }

            if (editOnOcean) {
                OceanEReferralAttachmentUtil.detachOceanEReferralConsult(docId, documentType.getType());
            }
        }
    }

    public void attachToEForm(String[] attachments, DocumentType documentType, String providerNo, Integer fdid) {
        List<String> currentList = new ArrayList<>(Arrays.asList(attachments));
        List<EFormDocs> eFormDocsList = eFormDocsDao.findByFdidIdDocType(fdid, documentType.getType());
        List<String> oldList = new ArrayList<>();
        for (EFormDocs eFormDoc : eFormDocsList) {
            oldList.add(Integer.toString(eFormDoc.getDocumentNo()));
        }
        detachFromEForm(currentList, oldList, documentType, fdid);
        attachToEForm(currentList, oldList, documentType, providerNo, fdid);
    }

    private void attachToEForm(List<String> currentList, List<String> oldList, DocumentType documentType, String providerNo, Integer fdid) {
        for (String docId : currentList) {
            if (oldList.contains(docId)) {
                continue;
            }
            EFormDocs eFormDocs = new EFormDocs(fdid, Integer.parseInt(docId), documentType.getType(), providerNo);
            eFormDocsDao.persist(eFormDocs);
        }
    }

    private void detachFromEForm(List<String> currentList, List<String> oldList, DocumentType documentType, Integer fdid) {
        for (String docId : oldList) {
            if (currentList.contains(docId)) {
                continue;
            }
            List<EFormDocs> detachList = eFormDocsDao.findByFdidIdDocNoDocType(fdid, Integer.valueOf(docId), documentType.getType());
            for (EFormDocs eFormDoc : detachList) {
                eFormDoc.setDeleted("Y");
                eFormDocsDao.merge(eFormDoc);
            }
        }
    }

    public void attachToTickler(String[] attachments, DocumentType documentType, String providerNo, Integer ticklerId) {
        List<String> currentList = new ArrayList<>(Arrays.asList(attachments));
        List<TicklerDocs> ticklerDocsList = ticklerDocsDao.findByTicklerIdDocType(ticklerId, documentType.getType());
        List<String> oldList = new ArrayList<>();
        for (TicklerDocs ticklerDoc : ticklerDocsList) {
            oldList.add(Integer.toString(ticklerDoc.getDocumentNo()));
        }
        detachFromTickler(currentList, oldList, documentType, ticklerId);
        attachToTickler(currentList, oldList, documentType, providerNo, ticklerId);
    }

    private void attachToTickler(List<String> currentList, List<String> oldList, DocumentType documentType, String providerNo, Integer ticklerId) {
        for (String docId : currentList) {
            if (oldList.contains(docId)) {
                continue;
            }
            TicklerDocs ticklerDocs = new TicklerDocs(ticklerId, Integer.parseInt(docId), documentType.getType(), providerNo);
            ticklerDocsDao.persist(ticklerDocs);
        }
    }

    private void detachFromTickler(List<String> currentList, List<String> oldList, DocumentType documentType, Integer ticklerId) {
        for (String docId : oldList) {
            if (currentList.contains(docId)) {
                continue;
            }
            List<TicklerDocs> detachList = ticklerDocsDao.findByTicklerIdDocNoDocType(ticklerId, Integer.valueOf(docId), documentType.getType());
            for (TicklerDocs ticklerDoc : detachList) {
                ticklerDoc.setDeleted(TicklerDocs.DELETED);
                ticklerDocsDao.merge(ticklerDoc);
            }
        }
    }
}
