package se.torsteneriksson.timetogo;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import se.torsteneriksson.timetogo.utilities.Utilities;

/**
 * Created by torsten on 10/28/2016.
 * Check this website:
 * https://developers.google.com/maps/documentation/distance-matrix/intro#RequestParameters
 *
 */

class TravelTimeChecker {
    private static final String API_KEY = "AIzaSyDaes5SLuSNR9WWx6ceiQvFdw28L0vNPGk";
    private static final String DISTANCEMATRIX_SEARCH_URL = "https://maps.googleapis.com/maps/api/distancematrix/json?";
    private static final String TAG = "TravelTimeChecker" ;
    private Context mContext = null;
    //https://maps.googleapis.com/maps/api/distancematrix/json?units=metrics&origins=59.301684,18.052251&destinations=59.402786,17.954865&key=AIzaSyDaes5SLuSNR9WWx6ceiQvFdw28L0vNPGk

    TravelTimeChecker(Context context){
        //SettingsActivity settings = new SettingsActivity(context);
        mContext = context;
    }

    public String[] searchPlaces(String origin, String destination, String avoid, String mode) {
        String url;
        if(!avoid.equals("")) {
            url = DISTANCEMATRIX_SEARCH_URL +
                    "units=metrics" +
                    "&origins=" + origin +
                    "&destinations=" + destination +
                    //"&departure_time=1477983600" +
                    "&departure_time=now" +
                    "&traffic_model=best_guess" +
                    "&avoid="+avoid +
                    "&mode=" + mode +
                    "&key=" + API_KEY;
        } else {
            url = DISTANCEMATRIX_SEARCH_URL +
                    "units=metrics" +
                    "&origins=" + origin +
                    "&destinations=" + destination +
                    //"&departure_time=1477983600" +
                    "&departure_time=now" +
                    "&traffic_model=best_guess" +
                    "&mode=" + mode +
                    "&key=" + API_KEY;
        }
        Log.d(TAG,"URL:" + url);
        String[] result ={};

        try{
            JSONObject distanceMatrix = getJSONObjectFromURL(url);
            result = parseDistanceMatrixJson(distanceMatrix);
        } catch (IOException e) {
            Log.d(TAG, "IOException");
            e.printStackTrace();
            Toast.makeText(mContext, mContext.getString(R.string.lookup_failed), Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Log.d(TAG,"JSONException");
            e.printStackTrace();
        }
        return result;
    }

    public String[] searchPlaces(Location origin, Location destination, String avoid, String mode) {
        String url;
        if(!avoid.equals("")) {
            url = DISTANCEMATRIX_SEARCH_URL +
                    "units=metrics" +
                    "&origins=" + String.valueOf(origin.getLatitude()) + "," + String.valueOf(origin.getLongitude()) +
                    "&destinations=" + String.valueOf(destination.getLatitude()) + "," + String.valueOf(destination.getLongitude()) +
                    //"&departure_time=1477983600" +
                    "&departure_time=now" +
                    "&traffic_model=best_guess" +
                    "&avoid="+avoid +
                    "&mode=" + mode +
                    "&key=" + API_KEY;
        } else {
            url = DISTANCEMATRIX_SEARCH_URL +
                    "units=metrics" +
                    "&origins=" + String.valueOf(origin.getLatitude()) + "," + String.valueOf(origin.getLongitude()) +
                    "&destinations=" + String.valueOf(destination.getLatitude()) + "," + String.valueOf(destination.getLongitude()) +
                    //"&departure_time=1477983600" +
                    "&departure_time=now" +
                    "&traffic_model=best_guess" +
                    "&mode=" + mode +
                    "&key=" + API_KEY;
        }
        Log.d(TAG,"URL:" + url);
        String[] result ={};

        try{
            JSONObject distanceMatrix = getJSONObjectFromURL(url);
            result = parseDistanceMatrixJson(distanceMatrix);
        } catch (IOException e) {
            Log.d(TAG, "IOException");
            e.printStackTrace();
            String[] temp = new String[1];
            temp[0] = e.getMessage().toString();
            result = temp;
        } catch (JSONException e) {
            Log.d(TAG,"JSONException");
            e.printStackTrace();
            String[] temp = new String[1];
            temp[0] = e.getMessage().toString();
            result = temp;
        }
        return result;
    }

    /**
     * Fetches a JSONObject from the url provided
     * @param urlString
     * @return The found JSON structure.
     * @throws IOException
     * @throws JSONException
     */
    private JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection;
        Log.d(TAG,"urlString: " + urlString);
        URL urlTemp= new URL(urlString);
        URL url = null;
        String urlStr;
        try {
            URI uri = new URI(urlTemp.getProtocol(), urlTemp.getUserInfo(), urlTemp.getHost(), urlTemp.getPort(), urlTemp.getPath(), urlTemp.getQuery(), urlTemp.getRef());
            urlStr = uri.toASCIIString();
            url = new URL(urlStr);
        } catch (Exception e) {
            Log.d("URIparse failed",e.toString());
            return null;
        }

        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.connect();
        Log.d(TAG,"response code:"+urlConnection.getResponseCode());
        if(urlConnection.getResponseCode() >= 400) {
            BufferedReader brError = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
            String line;
            while ((line = brError.readLine()) != null) {
                Log.d(TAG,line);
            }
            throw new IOException("Lookup failed:" + line);
        }
        BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));
        String jsonString;
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);
        return new JSONObject(jsonString);
    }



    /**
     * Parse the JSON object to find distance and duration_in_traffic
     * @param json the JSON structure as returned from GOOGLE distance matrix
     * @return Duration in traffic
     * @throws JSONException
     */
    private String[] parseDistanceMatrixJson(JSONObject json) throws JSONException{
        if(json == null) {
            throw new JSONException("No valid JSON object found!");
        }
        String status = json.getString("status");
        Log.d(TAG,"status:" + status);
        if(!status.equals("OK")) {
            Log.d(TAG,"Status:" + status);
            String[] errorMsg = {"Failed" +  status};
            return errorMsg;
        }

        JSONArray rows = json.getJSONArray("rows");
        Log.d(TAG,"rows:" + rows.toString());
        JSONObject first = rows.getJSONObject(0);
        Log.d(TAG,"first:" + first.toString());
        JSONArray elements = first.getJSONArray("elements");
        JSONObject second = elements.getJSONObject(0);
        String innerStatus = second.getString("status");
        if(!innerStatus.equals("OK")){
            Log.d(TAG,"Status:" + innerStatus);
            String[] errorMsg = {"Failed:" +  innerStatus};
            return errorMsg;
        }

        JSONObject distanceObject = second.getJSONObject("distance");
        String distance = distanceObject.getString("value");
        Log.d(TAG,"Distance:" + distance);
        JSONObject durationNoTrafficObject = second.getJSONObject("duration");
        String durationNoTraffic = durationNoTrafficObject.getString("value");
        Log.d(TAG,"Duration no traffic:" + durationNoTraffic);
        JSONObject durationInTrafficObject = second.getJSONObject("duration_in_traffic");
        String durationInTraffic = durationInTrafficObject.getString("value");
        Log.d(TAG,"Duration in traffic:" + durationInTraffic);
        String[] result = {durationInTraffic,durationNoTraffic,distance};
        return result;
        //return "Distance:" + distance + " Duration:" + duration;
    }
}