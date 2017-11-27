package com.example.winnipegbusschedules;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class StopActivity extends AppCompatActivity
{
  public static final String MAIN_PREFS = "Project Settings";
  public static final String ORDER_BY_TIME_PREF = "orderByTime";

  private boolean orderByTime;
  private String requestUrl;
  private String clickedStopNumber;
  private Transit.Stop stop;
  private ArrayList<Transit.Bus> busItems;
  private DBHelper dbHelper;
  private SharedPreferences sharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stop);

    sharedPreferences = getSharedPreferences(MAIN_PREFS, MODE_PRIVATE);
    orderByTime = sharedPreferences.getBoolean(ORDER_BY_TIME_PREF, true);

    dbHelper = new DBHelper(this);
  }

  @Override
  protected void onStart()
  {
    super.onStart();

    dbHelper.deleteOldRoutes();

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

  // Create the menu options with the menu_layout.xml file
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_layout, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId())
    {
      case R.id.miRefresh:
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        processRequest();
        break;

      case R.id.miSave:
        if (Helper.isNetworkAvailable(this))
        {
          Toast.makeText(this, "Saving Stop for offline use...", Toast.LENGTH_SHORT).show();

          Date dt = new Date();
          Calendar c = Calendar.getInstance();
          c.setTime(dt);
          c.add(Calendar.DATE, 1);
          dt = c.getTime();
          String endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA).format(dt);
          endDate = endDate.replace(' ', 'T');

          requestUrl = MapsActivity.BEGIN_URL
                  + MapsActivity.STOP_SCHEDULE_REQUEST_BEGIN
                  + "/" + clickedStopNumber
                  + MapsActivity.STOP_SCHEDULE_REQUEST_END
                  + MapsActivity.JSON_APPEND + "?"
                  + "end=" + endDate + "&"
                  + MapsActivity.API_KEY;

          processRequest();


          dbHelper.insertStopValues(stop.name, stop.key, stop.number,
                                    stop.latitude, stop.longitude);

          for (int i = 0; i < busItems.size(); i++)
          {
            Transit.Bus bus = busItems.get(i);
            dbHelper.insertRoutesValues(bus.variantName, bus.number, bus.key, bus.scheduledTime,
                                        bus.estimatedTime, bus.stopId);
          }
        }
        else
        {
          Toast.makeText(this, "It is not possible to save stop when offline.", Toast.LENGTH_SHORT).show();
        }

        break;

      case R.id.miDelete:

        Toast.makeText(this, "Deleting Stop for offline use...", Toast.LENGTH_SHORT).show();
        dbHelper.deleteStop(clickedStopNumber);

        break;

      case R.id.miOrderByTime:
        Toast.makeText(this, "Ordering by Time...", Toast.LENGTH_SHORT).show();
        orderByTime = true;
        setRoutesListView();
        break;

      case R.id.miOrderByRoute:
        Toast.makeText(this, "Ordering by Route...", Toast.LENGTH_SHORT).show();
        orderByTime = false;
        setRoutesListView();
        break;

//      case R.id.miTest:
//        Toast.makeText(this, "Ordering by Route...", Toast.LENGTH_SHORT).show();
//        dbHelper.deleteOldRoutes();
//        break;
    }

    return true;
  }

  private void setRoutesListView()
  {

    Collections.sort(busItems, new Comparator<Transit.Bus>()
    {
      @Override
      public int compare(Transit.Bus bus, Transit.Bus t1)
      {
        try
        {
          if (orderByTime)
          {
            return bus.getEstimatedTimeAsDate().compareTo(t1.getEstimatedTimeAsDate());
          }
          else
          {
            return bus.number.compareTo(t1.number);
          }
        }
        catch (ParseException e)
        {
          e.printStackTrace();
        }

        return 0;
      }
    });

    // create the adapter to populate the list view
    FeedAdapter feedAdapter = new FeedAdapter(StopActivity.this, R.layout.routes_list_item, busItems);

    ListView lvBuses = findViewById(R.id.lvBuses);
    lvBuses.setAdapter(feedAdapter);
  }

  @Override
  protected void onPause()
  {
    super.onPause();

    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(ORDER_BY_TIME_PREF, orderByTime);
    editor.apply();
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

      stop = dbHelper.loadDataStop(clickedStopNumber);

      TextView tvStop = findViewById(R.id.tvStop);
      String text = "Stop " + stop.number + " " + stop.name;
      tvStop.setText(text);

      busItems = dbHelper.loadDataRoutes(clickedStopNumber);
      // create the adapter to populate the list view
      FeedAdapter feedAdapter = new FeedAdapter(StopActivity.this, R.layout.routes_list_item, busItems);

      ListView lvBuses = findViewById(R.id.lvBuses);
      lvBuses.setAdapter(feedAdapter);
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

    // Get Stop Information
    stop = Helper.extractStopInfo(object.getJSONObject("stop"));

    String text = "Stop " + stop.number + " " + stop.name;
    tvStop.setText(text);

    // Get route schedules
    JSONArray routeSchedulesArray = object.getJSONArray("route-schedules");

    busItems = new ArrayList<>();

    for (int i = 0; i < routeSchedulesArray.length(); i++)
    {
      JSONObject routeScheduleObj = routeSchedulesArray.getJSONObject(i);

      // Get route description
      JSONObject routeObj = routeScheduleObj.getJSONObject("route");

      // Get schedule and estimated times
      JSONArray scheduledArray = routeScheduleObj.getJSONArray("scheduled-stops");

      for (int j = 0; j < scheduledArray.length(); j++)
      {
        Transit.Bus busItem = Helper.extractBusInfo(stop.key, routeObj, scheduledArray.getJSONObject(j));
        busItems.add(busItem);
      }
    }

    setRoutesListView();
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
          String text = o.number + " " + o.variantName;
          tvRoute.setText(text);
          //tvRoute.setTypeface(typeface, typefaceStyle);
        }

        if (tvTimes != null)
        {
          String text = "Scheduled: " + Helper.extractHourMinute(o.scheduledTime)
                        + " | Estimated: " + Helper.extractHourMinute(o.estimatedTime)
                        + " | Date: " + Helper.extractDate(o.estimatedTime);
          tvTimes.setText(text);
          //bt.setTypeface(typeface, typefaceStyle);
        }
      }

      return v;
    }
  }



}
