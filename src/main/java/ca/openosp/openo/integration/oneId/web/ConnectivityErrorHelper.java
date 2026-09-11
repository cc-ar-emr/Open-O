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
package ca.openosp.openo.integration.oneId.web;

import ca.openosp.openo.integration.ohcms.CMSException;
import ca.openosp.openo.integration.oneId.TokenExpiredException;
import org.owasp.encoder.Encode;

import javax.ws.rs.WebApplicationException;

/**
 * Maps ONE ID gateway and context-service failures to a short, user-safe message, a suggested next
 * step, and a stable log code. Messages are OWASP-encoded and never carry stack traces, tokens, or
 * PHI; the underlying error is recorded separately in the gateway transaction log.
 *
 * @since 2026-07-02
 */
public final class ConnectivityErrorHelper {

    private ConnectivityErrorHelper() {
    }

    /**
     * A user-facing rendering of a connectivity failure: an encoded message, a suggested next step,
     * and a stable code for the audit log.
     */
    public static final class ConnectivityError {
        private final String userMessage;
        private final String nextStep;
        private final String logCode;

        public ConnectivityError(String userMessage, String nextStep, String logCode) {
            this.userMessage = userMessage;
            this.nextStep = nextStep;
            this.logCode = logCode;
        }

        public String getUserMessage() {
            return userMessage;
        }

        public String getNextStep() {
            return nextStep;
        }

        public String getLogCode() {
            return logCode;
        }
    }

    /**
     * Maps a failure to a user-safe message, next step, and log code.
     *
     * @param t Throwable the failure raised by a gateway or context-service call
     * @return ConnectivityError the encoded message, next step, and log code
     */
    public static ConnectivityError map(Throwable t) {
        if (t instanceof TokenExpiredException) {
            return error("Your ONE ID session has expired.",
                    "Please sign in again to continue.", "TOKEN_EXPIRED");
        }
        if (t instanceof CMSException) {
            return error("The Ontario Health context service could not complete your request.",
                    "Please try again in a moment.", "CMS_ERROR");
        }
        if (t instanceof WebApplicationException) {
            return mapStatus(((WebApplicationException) t).getResponse().getStatus());
        }
        return error("Could not connect to Ontario Health.",
                "Please try again. If the problem continues, contact your administrator.",
                "GATEWAY_UNAVAILABLE");
    }

    /**
     * Maps a non-200 gateway response status to a user-safe message, next step, and log code.
     *
     * @param httpStatus int the HTTP status returned by the gateway or context service
     * @return ConnectivityError the encoded message, next step, and log code
     */
    public static ConnectivityError mapStatus(int httpStatus) {
        switch (httpStatus) {
            case 400:
                return error("Ontario Health rejected the request.",
                        "Please try again or contact your administrator.", "HTTP_400");
            case 401:
                return error("Your ONE ID session is no longer authorized.",
                        "Please sign in again to continue.", "HTTP_401");
            case 403:
                return error("You do not have access to this Ontario Health service.",
                        "Contact your administrator if you need access.", "HTTP_403");
            default:
                if (httpStatus >= 500) {
                    return error("Ontario Health is temporarily unavailable.",
                            "Please try again in a few minutes.", "HTTP_5XX");
                }
                return error("Could not connect to Ontario Health.",
                        "Please try again. If the problem continues, contact your administrator.",
                        "GATEWAY_ERROR");
        }
    }

    private static ConnectivityError error(String userMessage, String nextStep, String logCode) {
        return new ConnectivityError(Encode.forHtml(userMessage), Encode.forHtml(nextStep), logCode);
    }
}
