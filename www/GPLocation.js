/*global cordova, module*/

module.exports = {
    getGPLocation: function (options) {
        cordova.exec(options.success, options.error, "GPLocation", "getGPLocation", []);
    }

    cancelGPUpdates: function (options) {
        cordova.exec(options.success, options.error, "GPLocation", "cancelGPUpdates", []);
    }
};
