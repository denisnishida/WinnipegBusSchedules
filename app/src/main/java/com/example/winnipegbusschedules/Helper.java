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

  public static Transit.Stop extractStopInfo(JSONObject stopObj) throws JSONException
  {
    Transit transit = new Transit();
    Transit.Stop stop = transit.new Stop();

    stop.name = stopObj.getString("name");
    stop.number = stopObj.getString("number");

    JSONObject geographicObj = stopObj.getJSONObject("centre").getJSONObject("geographic");
    stop.latitude = geographicObj.getDouble("latitude");
    stop.longitude = geographicObj.getDouble("longitude");

    return stop;
  }
}
