var exec = require('cordova/exec');

exports.storeBgParams = function (key, value, success, error) {
    exec(success, error, "BgGeoLocBroadcastReceiver", "storeBgParams", [key, value]);
};
