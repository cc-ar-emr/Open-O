/*
 * DHDR-scoped shim for the AngularJS "providerServices" module.
 *
 * The oscarPro DHDR viewer depends on a full providerServices module that does not exist on this
 * codebase. The viewer only calls providerService.getAllActiveProviders(), so this shim provides
 * exactly that one function, wrapping the existing ProviderService REST endpoint
 * (GET /ws/rs/providerService/providers_json). Extend this module rather than porting the full
 * oscarPro version.
 */
angular.module("providerServices", [])
    .service("providerService", function ($http, $q, $log) {
        return {
            apiPath: '../ws/rs/providerService',

            getAllActiveProviders: function () {
                var deferred = $q.defer();
                $http({
                    url: this.apiPath + '/providers_json',
                    method: "GET"
                }).then(function (data) {
                    deferred.resolve(data.data.content);
                }, function () {
                    deferred.reject("An error occured while fetching providers");
                });
                return deferred.promise;
            }
        };
    });
