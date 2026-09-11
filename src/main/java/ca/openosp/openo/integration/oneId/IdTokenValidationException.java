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
package ca.openosp.openo.integration.oneId;

/**
 * Raised when an OpenID Connect id_token fails verification (signature, issuer, audience, expiry,
 * not-before, nonce, or a missing configuration/subject). The message is safe to log and never
 * contains the token or its claims.
 *
 * @since 2026-07-02
 */
public class IdTokenValidationException extends Exception {

    public IdTokenValidationException(String message) {
        super(message);
    }

    public IdTokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
