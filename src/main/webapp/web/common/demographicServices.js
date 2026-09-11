/*
 * DHDR-scoped shim for the AngularJS "demographicServices" module.
 *
 * The oscarPro DHDR viewer depends on a full demographicServices module that does not exist on
 * this codebase. The viewer only calls demographicService.getDemographic(), so this shim provides
 * exactly that one function, wrapping the existing DemographicService REST endpoint
 * (GET /ws/rs/demographics/{id}). Extend this module rather than porting the full oscarPro version.
 */
angular.module("demographicServices", [])
    .service("demographicService", function ($http, $q, $log) {
        return {
            apiPath: '../ws/rs/',
            configHeadersWithCache: {headers: {"Content-Type": "application/json", "Accept": "application/json"}, cache: true},

            getDemographic: function (demographicNo) {
                var deferred = $q.defer();
                $http.get(this.apiPath + 'demographics/' + demographicNo, this.configHeadersWithCache).then(function (response) {
                    // A patient the endpoint cannot return comes back as 200 with an empty body
                    // rather than a 404 - DemographicService answers null - so resolving whatever
                    // arrived handed the viewer an empty patient and let it search on regardless.
                    // The caller's load-error notice (DHDR02.02) only runs on a rejection, so it
                    // could never appear for the case it was written for.
                    if (!response || !response.data) {
                        deferred.reject("The patient record was not returned");
                        return;
                    }
                    deferred.resolve(response.data);
                }, function () {
                    deferred.reject("An error occurred while fetching demographic");
                });
                return deferred.promise;
            }
        };
    });
