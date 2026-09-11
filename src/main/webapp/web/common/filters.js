/*
 * "oscarFilters" AngularJS module.
 *
 * The DHDR viewer uses AngularJS built-in filters (date, filter, orderBy, json) plus the custom
 * "age" filter (patient header). Only "age" is ported here from the oscarPro oscarFilters module;
 * add other filters if a view later needs one.
 */
angular.module('oscarFilters', [])
    .filter('age', function () {
        return function (input) {
            if (input != null && input.years != null) {
                // No unit spacing: the banner reads "(6m)" beside the date of birth, and the years
                // branch used to render "42 y" while the other two rendered "10d" and "6m".
                if (input.years < 1 && input.months < 1) {
                    return input.days + "d";
                }
                if (input.years < 2) {
                    // years holds whole years and months only the remainder, so the year has to be
                    // added back in here. Printing months alone rendered a one-year-old as "0m" and
                    // an eighteen-month-old as "6m", each about a year younger than they are.
                    return (input.years * 12 + input.months) + "m";
                }
                return input.years + "y";
            }
            return "";
        };
    });
