/*
********************************************************
Copyright @ WebOnWebOff.com, by D. Miller
You may reuse this script, on condition that: 
	- this copyright text is kept
	- report improvements/changes to contact(at)WebOnWebOff.com
www.WebOnWebOff.com
********************************************************
*/
var yLib = (function() {
    // Helper function to check if a string is valid and safe
    function isValidNamespace(sNameSpace) {
        return typeof sNameSpace === 'string' && 
               sNameSpace.trim() !== '' && 
               !sNameSpace.includes('__proto__') && 
               !sNameSpace.includes('constructor');
    }

    return {
        util: Object.create(null), // Create an object without a prototype
        widget: Object.create(null), // Create an object without a prototype
        namespace: function(sNameSpace) {
            if (!isValidNamespace(sNameSpace)) {
                console.error('Invalid or unsafe namespace:', sNameSpace);
                return null;
            }

            var levels = sNameSpace.split('.');
            var thisNameSpace = this;

            for (var i = (levels[0] === 'yLib' ? 1 : 0); i < levels.length; i++) {
                var level = levels[i];

                // Skip unsafe levels
                if (level === '__proto__' || level === 'constructor') {
                    console.error('Unsafe level detected:', level);
                    return null;
                }

                // Create a new level if it doesn't exist
                if (!thisNameSpace[level]) {
                    thisNameSpace[level] = Object.create(null); // Create an object without a prototype
                }

                thisNameSpace = thisNameSpace[level];
            }

            return thisNameSpace;
        }
    };
})();
