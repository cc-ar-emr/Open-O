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

package oscar.oscarBilling.ca.bc.pageUtil;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import oscar.entities.PaymentType;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class ViewReceivePayment2Action
        extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    public String execute() {
        BillingViewBean bean = new BillingViewBean();
        String billingMasterNo = request.getParameter("lineNo");
        String billNo = request.getParameter("billNo");
        List paymentTypes = bean.getPaymentTypes();
        for (int i = 0; i < paymentTypes.size(); i++) {
            PaymentType tp = (PaymentType) paymentTypes.get(i);
            if ("ELECTRONIC".equals(tp.getPaymentType())) {
                paymentTypes.remove(i);
            }
        }
        this.setPaymentMethodList(paymentTypes);
        this.setBillingmasterNo(billingMasterNo);
        this.setBillNo(billNo);
        return SUCCESS;
    }

    private String amountReceived;
    private String payment;
    private String paymentMethod;
    private List paymentMethodList;
    private String billingmasterNo;
    private String billNo;
    private boolean paymentReceived;
    private String isRefund;
    private String payeeProviderNo;

    public String getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(String amountReceived) {
        this.amountReceived = amountReceived;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    public void setPaymentMethodList(List paymentMethodList) {
        this.paymentMethodList = paymentMethodList;
    }

    public void setBillingmasterNo(String billingmasterNo) {
        this.billingmasterNo = billingmasterNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public void setPaymentReceived(boolean paymentReceived) {
        this.paymentReceived = paymentReceived;
    }

    public void setIsRefund(String isRefund) {

        this.isRefund = isRefund;
    }

    public void setPayeeProviderNo(String payeeProviderNo) {
        this.payeeProviderNo = payeeProviderNo;
    }

    public String getPayment() {
        return payment;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public List getPaymentMethodList() {
        return paymentMethodList;
    }

    public String getBillingmasterNo() {
        return billingmasterNo;
    }

    public String getBillNo() {
        return billNo;
    }

    public boolean isPaymentReceived() {
        return paymentReceived;
    }

    public String getIsRefund() {
        return isRefund;
    }

    public String getPayeeProviderNo() {
        return payeeProviderNo;
    }
}
