package com.pangea;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;

/**
 * This class echoes a string called from JavaScript.
 */
public class BgGeoLocBroadcastReceiver extends CordovaPlugin {
  public static final String PREFS_NAME = "NativeStorage";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("storeBgParams")) {
      try {
        this.storeBgParams(args.getString(0), args.getString(1), callbackContext);
      } catch (Exception ex){
        ex.printStackTrace();
        return false;
      }
      return true;
    }
    return false;
  }

  private void storeBgParams(String key, String value, CallbackContext callbackContext) {
    try {
      Context context=this.cordova.getActivity().getApplicationContext();
      SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).edit();
      editor.putString(key, value);
      editor.apply();
      callbackContext.success("Params stored!");
    } catch (Exception e) {
      callbackContext.error("Failed to store params: " + e.toString());
    }
  }
}
