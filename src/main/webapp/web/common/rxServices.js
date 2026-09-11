/*
 * DHDR-scoped shim for the AngularJS "rxServices" module.
 *
 * The oscarPro DHDR viewer depends on a full rxServices module that does not exist on this
 * codebase. The comparative view only calls rxService.getMedications(), so this shim provides
 * exactly that one function, wrapping the existing RxWebService REST endpoint
 * (GET /ws/rs/rx/drugs?demographicNo=). Extend this module rather than porting the full
 * (549-line) oscarPro version, which is shared Rx infrastructure unrelated to DHDR.
 */
angular.module("rxServices", [])
    .service("rxService", function ($http, $q, $log) {
        return {
            apiPath: '../ws/rs/rx',

            getMedications: function (demographicNo, status) {
                var deferred = $q.defer();
                var queryPath = this.apiPath + "/drugs";
                if (status !== "") {
                    queryPath = queryPath + '/' + status;
                }
                queryPath = queryPath + '?demographicNo=' + demographicNo;
                $http.get(queryPath).then(function (data) {
                    deferred.resolve(data);
                }, function () {
                    deferred.reject("An error occurred while fetching medications");
                });
                return deferred.promise;
            }
        };
    });
