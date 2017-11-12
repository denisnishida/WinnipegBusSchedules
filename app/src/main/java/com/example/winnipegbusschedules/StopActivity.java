package com.example.winnipegbusschedules;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class StopActivity extends AppCompatActivity
{
  String requestUrl;
  String clickedStopNumber;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stop);
  }

  @Override
  protected void onStart()
  {
    super.onStart();

    // Get the stop number in the intent
    Intent intent = getIntent();
    clickedStopNumber = intent.getStringExtra(MapsActivity.STOP_NUMBER_KEY);

    requestUrl = MapsActivity.BEGIN_URL
                + MapsActivity.STOP_SCHEDULE_REQUEST_BEGIN
                + "/" + clickedStopNumber
                + MapsActivity.STOP_SCHEDULE_REQUEST_END
                + MapsActivity.JSON_APPEND + "?"
                + MapsActivity.API_KEY;

    processRequest();
  }

  public void processRequest()
  {
    if (Helper.isNetworkAvailable(this))
    {
      // create and execute AsyncTask
      ProcessingTask task = new ProcessingTask();
      task.execute();
    }
    else
    {
      Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }
  }

  class ProcessingTask extends AsyncTask
  {
    @Override
    protected Object doInBackground(Object[] objects)
    {
      // Create URL object to RSS file
      URL url = null;

      try
      {
        url = new URL(requestUrl);
      }
      catch (MalformedURLException e)
      {
        e.printStackTrace();
      }

      // Create and open HTTP connection
      HttpURLConnection connection = null;
      try
      {
        if (url != null)
        {
          connection = (HttpURLConnection) url.openConnection();
        }
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }

      try
      {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null)
        {
          stringBuilder.append(line);
        }

        bufferedReader.close();

        return stringBuilder.toString();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      finally
      {
        if (connection != null)
        {
          connection.disconnect();
        }
      }

      return null;
    }

    // On the end of the thread
    @Override
    protected void onPostExecute(Object o)
    {
      if (o != null)
      {
        String response = o.toString();
        Log.i("INFO", o.toString());
        parseJSON(response);
      }
      else
      {
        Toast.makeText(StopActivity.this,
                "Sorry, it was not possible to get the schedule information",
                Toast.LENGTH_SHORT).show();
      }
    }

    // Verify the request and get the values
    private void parseJSON(String response)
    {
      try
      {
        // Using orj.json, get the file string and convert it to an object
        JSONObject object = (JSONObject) new JSONTokener(response).nextValue();

        // The Winnipeg Transit JSON results usually have nested values
        // We can identify the request by the first key of the first level

        // The method names() will retrieve an JSONArray with the key names
        JSONArray objectNames = object.names();

        // Retrieve the first key of the first level
        String firstKey = objectNames.getString(0);

        if (firstKey.equals("stop-schedule"))
        {
          parseStopSchedule(object.getJSONObject(firstKey));
        }
      }
      catch (JSONException e)
      {
        e.printStackTrace();
      }
    }
  }

  // Get the information from the stop schedule request
  private void parseStopSchedule(JSONObject object) throws JSONException
  {
    TextView tvStop = findViewById(R.id.tvStop);
    ListView lvBuses = findViewById(R.id.lvBuses);

    // Get Stop Information
    JSONObject stopObject = object.getJSONObject("stop");

    String text = "Stop " + stopObject.getString("key")
                  + ": " + stopObject.getString("name");
    tvStop.setText(text);

    ArrayList<Transit.Bus> busItems = new ArrayList<>();

    // Get route schedules
    JSONArray routeSchedulesArray = object.getJSONArray("route-schedules");

    for (int i = 0; i < routeSchedulesArray.length(); i++)
    {
      JSONObject routeScheduleObj = routeSchedulesArray.getJSONObject(i);

      // Get route description
      JSONObject routeObj = routeScheduleObj.getJSONObject("route");

      // Get schedule and estimated times
      JSONArray scheduledArray = routeScheduleObj.getJSONArray("scheduled-stops");

      Transit transit = new Transit();

      for (int j = 0; j < scheduledArray.length(); j++)
      {
        Transit.Bus busItem = transit.new Bus();

        busItem.number = routeObj.getString("number");

        JSONObject scheduledObj = scheduledArray.getJSONObject(j);

        JSONObject variantObj = scheduledObj.getJSONObject("variant");
        busItem.variantName = variantObj.getString("name");

        JSONObject arrivalObj = scheduledObj.getJSONObject("times").getJSONObject("arrival");
        busItem.scheduledTime = arrivalObj.getString("scheduled");
        busItem.estimatedTime = arrivalObj.getString("estimated");

        busItems.add(busItem);
      }
    }

    // create the adapter to populate the list view
    FeedAdapter feedAdapter = new FeedAdapter(StopActivity.this, R.layout.routes_list_item, busItems);
    lvBuses.setAdapter(feedAdapter);
  }

  // Custom ArrayAdapter for our ListView
  private class FeedAdapter extends ArrayAdapter<Transit.Bus>
  {
    private ArrayList<Transit.Bus> items;

    public FeedAdapter(Context context, int textViewResourceId, ArrayList<Transit.Bus> items)
    {
      super(context, textViewResourceId, items);
      this.items = items;
    }

    //This method is called once for every item in the ArrayList as the list is loaded.
    //It returns a View -- a list item in the ListView -- for each item in the ArrayList
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
      View v = convertView;

      if (v == null) {
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.routes_list_item, null);
      }

      Transit.Bus o = items.get(position);

      if (o != null)
      {
        TextView tvRoute = (TextView) v.findViewById(R.id.tvRoute);
        TextView tvTimes = (TextView) v.findViewById(R.id.tvTimes);

        if (tvRoute != null)
        {
          String text = o.number + " - " + o.variantName;
          tvRoute.setText(text);
          //tvRoute.setTypeface(typeface, typefaceStyle);
        }

        if (tvTimes != null)
        {
          String text = "Scheduled: " + Helper.extractHourMinute(o.scheduledTime)
                        + " | Estimated: " + Helper.extractHourMinute(o.estimatedTime);
          tvTimes.setText(text);
          //bt.setTypeface(typeface, typefaceStyle);
        }
      }

      return v;
    }
  }



}
