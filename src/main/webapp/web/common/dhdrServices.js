/*

    Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
    This software is published under the GPL GNU General Public License.
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    This software was written for the
    Department of Family Medicine
    McMaster University
    Hamilton
    Ontario, Canada

*/
/*
 * DHDR client service. Trimmed to the endpoints the DHDR viewer consumes; the gateway-log, UAO and
 * token-expiry methods from the oscarPro original are intentionally omitted because they belong to
 * the EHR-connectivity admin surface (see DHDRService.java, which excludes the matching endpoints).
 */
angular.module("dhdrServices", [])
    .service("dhdrService", function ($http, $q, $log) {
        return {
            apiPath: '../ws/rs',
            // A header name/value map, not a whole $http config. It is spread into config.headers
            // below, and the extra {headers: ...} wrapper this used to carry meant every DHDR
            // request went out with a literal header named "headers" whose value stringified to
            // [object Object]. Angular's own defaults still supplied Content-Type, so the requests
            // worked and the junk header simply rode along.
            configHeaders: {"Content-Type": "application/json", "Accept": "application/json"},

            searchByDemographicNo2: function (demographicNo, searchConfig) {
                var deferred = $q.defer();
                $http({
                    url: this.apiPath + '/dhdr/searchByDemographicNo2?demographicNo='
                        + encodeURIComponent(demographicNo),
                    method: "POST",
                    data: searchConfig,
                    headers: this.configHeaders,
                }).then(function (response) {
                    deferred.resolve(response.data);
                }, function (response) {
                    // DHDR14.01: reject with the notice the viewer renders, never a bare string. The
                    // server reports a reachable-but-failing DHDR EHR Service on the resolve path;
                    // landing here means the EMR's own endpoint failed. Angular reports no response
                    // at all (network failure, timeout) as status <= 0.
                    var noResponse = !response || response.status <= 0;
                    deferred.reject({
                        httpCode: noResponse ? 503 : response.status,
                        httpMessage: noResponse
                            ? "The DHDR EHR Service did not respond."
                            : "Drug and pharmacy service information could not be retrieved.",
                        severity: "error",
                        // DHDR03.06: a Date, for the same reason the two notices raised in
                        // index.jsp are. The viewer renders this through date:'medium'; Angular's
                        // date filter returns a non-ISO string untouched, so toLocaleString() here
                        // both skipped the shared format and followed the workstation locale,
                        // leaving one ambiguous timestamp beside notices that were already fixed.
                        dateTime: new Date(),
                        moreInformation: "Retry the search; if the problem persists, contact your EMR support desk."
                    });
                });
                return deferred.promise;
            },
            // Calls the ported /dhdr/getConsentOveride endpoint, which returns the PCOI viewlet URL
            // (in referenceURL) plus a correlation token (uuid). Replaces oscarPro's closed /kaiemr
            // viewlet launch; the override target is fixed server-side, so key is no longer sent.
            getConsentOveride: function (demographicNo, key) {
                var deferred = $q.defer();
                $http({
                    url: this.apiPath + '/dhdr/getConsentOveride?demographicNo='
                        + encodeURIComponent(demographicNo),
                    method: "GET",
                    headers: this.configHeaders,
                }).then(function (response) {
                    deferred.resolve(response);
                }, function () {
                    // The response may carry PHI, so it is not written to the browser console; the
                    // server records the failure in the gateway audit log.
                    deferred.reject("The consent override request could not be completed.");
                });
                return deferred.promise;
            },
            logConsentOverride: function (demographicNo, uniqueToken, dataReceived, status) {
                var deferred = $q.defer();
                $http({
                    // Encoded per segment: these are a number and a server-generated UUID today, so
                    // nothing here needs escaping, but a path segment that ever carries a / or ?
                    // would otherwise reshape the request rather than be rejected by it.
                    url: this.apiPath + '/dhdr/logConsentOverride/' + encodeURIComponent(demographicNo)
                        + '/' + encodeURIComponent(uniqueToken)
                        + '?status=' + encodeURIComponent(status),
                    method: "POST",
                    data: dataReceived,
                    headers: this.configHeaders,
                }).then(function (response) {
                    deferred.resolve(response);
                }, function () {
                    // The response may carry PHI, so it is not written to the browser console; the
                    // server records the failure in the gateway audit log.
                    deferred.reject("The consent override could not be logged.");
                });
                return deferred.promise;
            }
        };
    });
