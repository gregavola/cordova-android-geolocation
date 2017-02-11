/*global cordova, module*/

var exec = require('cordova/exec');

exports.getGPLocation = function (success, error) {
    exec(success, error, "GPLocation", "getGPLocation", []);
};

exports.cancelGPUpdates = function (success, error) {
    exec(success, error, "GPLocation", "cancelGPUpdates", []);
};
