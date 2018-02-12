package com.krabhi;

import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.logger.TSLog;
import com.truckstop.pangea.qa.R;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;


/**
 * This BroadcastReceiver receives broadcasted events from the BackgroundGeolocation plugin.
 * It's designed for you to customize in your own application, to handle events in the native
 * environment.  You can use this in cases where the user has terminated your foreground Activity
 * while the BackgroundGeolocation background Service continues to operate.
 *
 * You have full access to the BackgroundGeolocation API adapter.  You may execute any documented
 * API method, such as #start, #stop, #changePace, #getCurrentPosition, etc.
 *
 * @author chris scott, Transistor Software www.transistorsoft.com
 *
 * This BroadcastReceiver receives the following events:
 *
 * @event heartbeat         BackgroundGeolocation.EVENT_HEARTBEAT
 * @event motionchange      BackgroundGeolocation.EVENT_MOTIONCHANGE
 * @event location          BackgroundGeolocation.EVENT_LOCATION
 * @event geofence          BackgroundGeolocation.EVENT_GEOFENCE
 * @event http              BackgroundGeolocation.EVENT_HTTP
 * @event schedule          BackgroundGeolocation.EVENT_SCHEDULE
 * @event activitychange    BackgroundGeolocation.EVENT_ACTIVITYCHANGE
 * @event providerchange    BackgroundGeolocation.EVENT_PROVIDERCHANGE
 * @event geofenceschange   BackgroundGeolocation.EVENT_GEOFENCESCHANGE
 * @event heartbeat         BackgroundGeolocation.EVENT_BOOT
 *
 */
public class EventReceiver extends BroadcastReceiver {
    public static String TAG = "TSEventReceiver";
    private static final String PREFS_NAME = "NativeStorage";

    @Override
    public void onReceive(Context context, Intent intent) {
        String eventName = getEventName(intent.getAction());
        String message = TSLog.header("Truckstop: BackgroundGeolocation EventReceiver: " + eventName);
        TSLog.logger.info(message);

        // Decode event name
        if (BackgroundGeolocation.EVENT_HEARTBEAT.equalsIgnoreCase(eventName)) {
            onHeartbeat(context, intent);
        } else if (BackgroundGeolocation.EVENT_PROVIDERCHANGE.equalsIgnoreCase(eventName)) {
            onProviderChange(context, intent);
        }
    }

    /**
     * Fetch the last portion of the Intent action foo.bar.EVENT_NAME -> event_name
     * @param {String} action
     * @return {string} eventName
     */
    private String getEventName(String action) {
        String[] path = action.split("\\.");
        return path[path.length-1].toLowerCase();
    }

    /**
     * @event heartbeat
     * @param {Boolean} isMoving
     * @param {JSONObject} location
     */
    private void onHeartbeat(Context context, Intent intent) {
        try {
            JSONObject location = new JSONObject(intent.getStringExtra("location"));
            String data = location.toString(); ;
            data = "{\"locations\": [".concat(data).concat("]}");
            String url = getSharedPreferenceValue(context.getSharedPreferences(PREFS_NAME,
                    context.MODE_PRIVATE),"HeartbeatApiURL");
            if(!url.equals(null) && !url.equals("")){
                new TruckstopHttpApi().execute(url, data);
            }
        } catch (JSONException e) {
            TSLog.logger.error(TSLog.error(e.getMessage()));
        }
    }

    /**
     * @event providerchange
     * @param {String} activityName
     */
    private void onProviderChange(Context context, Intent intent) {
        try {
            JSONObject provider = new JSONObject(intent.getStringExtra("provider"));
            String data = provider.toString();
            Calendar c = Calendar.getInstance();
            String userId = getSharedPreferenceValue(context.getSharedPreferences(PREFS_NAME,
              context.MODE_PRIVATE),"userId");
            data = data.substring(0,data.length()-1)
                    .concat(",\"userAccount\":"+userId)
                    .concat(",\"deviceId\":"+getSharedPreferenceValue(context.getSharedPreferences(PREFS_NAME,
                            context.MODE_PRIVATE),"X-DeviceID"))
                    .concat(",\"deviceTimestamp\":\"\\/Date("+c.getTimeInMillis()+")\\/\"")
                    .concat("}");
            String url = getSharedPreferenceValue(context.getSharedPreferences(PREFS_NAME,
                    context.MODE_PRIVATE),"ProviderApiURL");
            if(!url.equals(null) && !url.equals("")){
              if(!userId.equals(null) && !userId.equals("")){
                new TruckstopHttpApi().execute(url, data);
              }
            }

            if(provider.getBoolean("network") || provider.getBoolean("gps")) {
                sendNotification(context, getSharedPreferenceValue(context.getSharedPreferences(PREFS_NAME,
                context.MODE_PRIVATE),"ProviderMessage"));
            }
        } catch (JSONException e) {
            TSLog.logger.error(TSLog.error(e.getMessage()));
        }
    }

    private void sendNotification(Context context, String notificationText) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.common_full_open_on_phone)
                        .setContentTitle("Truckstop Mobile")
                        .setContentText(notificationText);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(001, mBuilder.build());
    }

    public class TruckstopHttpApi extends AsyncTask<String, Void, Integer> {

        public TruckstopHttpApi(){
            //set context variables if required
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected Integer doInBackground(String... params) {
            String urlString = params[0]; // URL to call
            String data = params[1]; //data to post
            urlString = urlString.replaceAll("\"", "");
            TSLog.logger.debug("Truckstop Http Request: " + urlString + " | " + data);
            OutputStream out = null;
            int responseCode = 0;
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out, "UTF-8"));
                writer.write(data);
                writer.flush();
                writer.close();
                out.close();
                urlConnection.connect();
                responseCode =  urlConnection.getResponseCode();
                TSLog.logger.debug("Truckstop Http Response: " + responseCode);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                TSLog.logger.debug("Truckstop HTTP error:"+ e.getMessage());
            }
            return responseCode;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);
        }
    }

    private String getSharedPreferenceValue(SharedPreferences sharedPreferences, String id) {
        return sharedPreferences.getString(id, "");
    }
}