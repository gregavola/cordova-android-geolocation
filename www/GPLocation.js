/*global cordova, module*/

var exec = require('cordova/exec');

exports.getGPLocation = function (options) {
    exec(options.success, options.error, "GPLocation", "getGPLocation", []);
};

exports.cancelGPUpdates = function (options) {
    exec(options.success, options.error, "GPLocation", "cancelGPUpdates", []);
};
