var exec = require('cordova/exec');

module.exports = {
    /**
     * Gets one position straight from the GPS provider (not the WebView's fused provider).
     * @param {{timeout?: number, maxAge?: number}} options - timeout in ms (default 15000);
     *   maxAge in ms: a last known fix at most this old is returned right away (default 60000)
     * successCallback receives {lat, lon, accuracy, altitude, provider, ageMs}.
     * errorCallback receives {code, message}: 1 = no permission, 2 = GPS provider disabled
     * or unavailable, 3 = timeout without a fix.
     */
    getCurrentPosition: function(options, successCallback, errorCallback) {
        options = options || {};
        var timeout = options.timeout != null ? options.timeout : 15000;
        var maxAge = options.maxAge != null ? options.maxAge : 60000;
        exec(successCallback, errorCallback, 'LatinadGps', 'getCurrentPosition', [timeout, maxAge]);
    }
};
