package com.example.winnipegbusschedules;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Denis on 06/11/2017.
 * Class that contains methods used in many places
 */
public class Helper
{
  public static boolean isNetworkAvailable(Context context)
  {
    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = null;

    if (connectivityManager != null)
    {
      activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    }

    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  // The hour and minute need to be extracted because the string is date and time combined
  public static String extractHourMinute(String dateTime)
  {
    String[] aux = dateTime.split("T");
    String[] auxTime = aux[1].split(":");

    return auxTime[0] + ":" + auxTime[1];
  }

  public static String extractDate(String dateTime)
  {
    String[] aux = dateTime.split("T");

    return aux[0];
  }

  public static Transit.Stop extractStopInfo(JSONObject stopObj) throws JSONException
  {
    Transit transit = new Transit();
    Transit.Stop stop = transit.new Stop();

    stop.key = stopObj.getString("key");
    stop.name = stopObj.getString("name");
    stop.number = stopObj.getString("number");

    JSONObject geographicObj = stopObj.getJSONObject("centre").getJSONObject("geographic");
    stop.latitude = geographicObj.getDouble("latitude");
    stop.longitude = geographicObj.getDouble("longitude");

    return stop;
  }

  public static Transit.Bus extractBusInfo(String stopKey, JSONObject routeObj, JSONObject scheduledObj) throws JSONException
  {
    Transit transit = new Transit();
    Transit.Bus busItem = transit.new Bus();

    busItem.number = routeObj.getString("number");
    busItem.key = scheduledObj.getString("key");

    JSONObject variantObj = scheduledObj.getJSONObject("variant");
    busItem.variantName = variantObj.getString("name");

    JSONObject arrivalObj = scheduledObj.getJSONObject("times").getJSONObject("arrival");
    busItem.scheduledTime = arrivalObj.getString("scheduled");
    busItem.estimatedTime = arrivalObj.getString("estimated");

    busItem.stopId = stopKey;

    return busItem;
  }

  // Checks if the stop is saved in the database
  public static boolean isSaved(Context context, String stopKey)
  {
    boolean result = false;

    DBHelper dbHelper = new DBHelper(context);
    Transit.Stop tempStop = dbHelper.loadDataStop(stopKey);

    if (tempStop != null)
    {
      result = true;
    }

    return result;
  }
}
