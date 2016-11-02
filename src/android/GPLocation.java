package cordova.plugins;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.Manifest;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.GoogleApiAvailability;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gregavola on 11/1/16.
 */

public class GPLocation extends CordovaPlugin {
    public CallbackContext gpsCallBack;
    public GoogleApiClient mGoogleApiClient;
    public FetchGoogleCoordinates fetchGoogleCordinates = new FetchGoogleCoordinates();
    public String TAG = "CORDOVA-GPS";
    String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.gpsCallBack = callbackContext;

        if(action.equals("getGPLocation")) {

            Log.e(TAG, "startGPService");

            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);

            this.gpsCallBack.sendPluginResult(pluginResult);

            startGPSerivce();


            return true;
        }
        else if(action.equals("cancelGPUpdates")) {

            Log.e(TAG, "cancelGPUpdates");

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            this.gpsCallBack.sendPluginResult(pluginResult);

            stopGPService();

            return true;
        }
        else{
            gpsCallBack.error("Invalid action : "+action+" passed");
        }

        return false;
    }

    public boolean startGPSerivce() {
        try {
            fetchGoogleCordinates.startGPLogging();
            return true;
        } catch (Exception error) {
            Log.e(TAG, error.toString());
            gpsCallBack.error("Unknown error occurred with GP Location.");
            return false;
        }
    }

    public boolean stopGPService() {
        Log.e(TAG, "We cancelled the GP service");
        fetchGoogleCordinates.stopLocationUpdates(false);
        return true;
    }

    public void requestPermission() {
        PermissionHelper.requestPermissions(this, 0, permissions);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        PluginResult result;
        //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
        if(this.cordova != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission Denied!");
                    gpsCallBack.error("Location Permission Deined");
                    return;
                }

            }

            //start the service
            Log.d(TAG, "Permission accepted, starting GP Lookup");
            this.startGPSerivce();
        }
    }

    public boolean hasPermisssion() {
        for(String p : permissions)
        {
            if(!PermissionHelper.hasPermission(this, p))
            {
                return false;
            }
        }
        return true;
    }

    public class FetchGoogleCoordinates implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


        LocationRequest locationRequest;
        private double lati = 0.0;
        private double longi = 0.0;
        private int accuray = 0;
        private String info = "";
        private boolean didTimeout = true;
        private boolean isConnecting = false;
        public GPLocation myLocation = new GPLocation();
        public String TAG = "CORDOVA-GPS";



        protected void startGPLogging() {

            LocationManager manager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);

            mGoogleApiClient = new GoogleApiClient.Builder(cordova.getActivity())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                gpsCallBack.error("Your GPS/Network Location has been disabled in your Settings. Please turn on Location Services to enable this feature.");
            }
            else {

                GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                int resultCode = apiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());

                if(resultCode == ConnectionResult.SUCCESS) {

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (didTimeout) {
                                Log.e(TAG, "Timeout after 10s");
                                stopLocationUpdates(true);
                            }
                        }
                    }, 10000);

                    if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting() && !isConnecting) {
                        isConnecting = true;
                        mGoogleApiClient.connect();
                    }

                } else {
                    gpsCallBack.error("Google Play Services is out of Date. Please update it via Google Play to use Location.");
                }
            }
        }

        public void stopLocationUpdates(Boolean isTimeout) {
            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (LocationListener) this);


                    Log.e(TAG, "Removed Location Listeners");

                    if (mGoogleApiClient.isConnected()) {
                        isConnecting = false;
                        mGoogleApiClient.disconnect();
                    }

                    if (isTimeout) {
                        isConnecting = false;
                        gpsCallBack.error("A request for location has timed out after 10 seconds.");
                    }
                } else {
                    try {
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (LocationListener) this);
                    } catch (Exception ex) {
                        Log.e(TAG, "Ex => " + ex.toString());
                    }


                    Log.e(TAG, "We can't cancel the updates because no updates have been made");
                }
            } else {
                Log.e(TAG, "Oh no locationClient was null");
            }
        }




        public void onConnected(Bundle arg0) {
            locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000); // milliseconds
            locationRequest.setFastestInterval(5000); // the fastest rate in milliseconds at which your app can handle location updates
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if (myLocation.hasPermisssion()) {
                try {
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, (LocationListener) this);
                } catch (SecurityException ex) {
                    Log.e(TAG, "GPS exception => " + ex.toString());
                    gpsCallBack.error("Unable to obtain location.");
                }
            }


        }

        public void onLocationChanged(android.location.Location location) {

            Log.e(TAG, "onLoacationCalled");

            didTimeout = false;

            if (location != null) {

                JSONObject returnVal = new JSONObject();

                lati = location.getLatitude();
                longi = location.getLongitude();
                accuray = (int) location.getAccuracy();
                info = location.getProvider();

                try {

                    stopLocationUpdates(false);

                    returnVal.put("lat", lati);
                    returnVal.put("lng", longi);
                    returnVal.put("provider", info);
                    returnVal.put("accurary", accuray);
                    returnVal.put("type", "GooglePlay");

                    // For Debugging
                    Log.e(TAG, returnVal.toString());

                    PluginResult result = new PluginResult(PluginResult.Status.OK, returnVal);
                    result.setKeepCallback(false);

                    gpsCallBack.sendPluginResult(result);

                    gpsCallBack.success(returnVal);

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    gpsCallBack.error("Unable to get Location (JSON)");
                }

            } else {
                gpsCallBack.error("A request for location has been unsuccessful. Please try again.");
            }
        }

        public void onConnectionSuspended(int arg0) {
            // TODO Auto-generated method stub

        }

        public void onConnectionFailed(ConnectionResult arg0) {
            // TODO Auto-generated method stub
            gpsCallBack.error("Unable to get Location");
        }


        public void onDisconnected() {
            // TODO Auto-generated method stub
            Log.e(TAG, "Client was Disconnected");
        }


    }
}

