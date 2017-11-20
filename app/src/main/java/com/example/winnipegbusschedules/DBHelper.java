package com.example.winnipegbusschedules;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Denis on 12/11/2017.
 * Class to facilitate Database interaction
 */

public class DBHelper extends SQLiteOpenHelper
{
  //Define your database name
  private static final String DB_NAME = "winnipegtransit";

  //Define your table name
  private static final String TABLE_STOPS = "stops";
  private static final String TABLE_ROUTES = "routes";

  //Create constants defining your stops table column names
  private static final String STOP_COL_NAME = "name";
  private static final String STOP_COL_ID = "id";
  private static final String STOP_COL_NUMBER = "number";
  private static final String STOP_COL_LAT = "latitude";
  private static final String STOP_COL_LON = "longitude";

  //Create constants defining your routes table column names
  private static final String ROUTE_COL_ID = "id";
  private static final String ROUTE_COL_NUMBER = "number";
  private static final String ROUTE_COL_NAME = "name";
  private static final String ROUTE_COL_SCHEDULED_TIME = "scheduled_time";
  private static final String ROUTE_COL_ESTIMATED_TIME = "estimated_time";
  private static final String ROUTE_COL_STOP_ID = "stop_id";

  //Define the database version
  private static final int DB_VERSION = 3;

  //Define your create statement in typical sql format
  //CREATE TABLE {Tablename} (
  //Colname coltype
  //)
  private static final String TABLE_STOPS_CREATE = "CREATE TABLE " + TABLE_STOPS + " ("
                                                + STOP_COL_ID + " VARCHAR PRIMARY KEY, "
                                                + STOP_COL_NAME + " TEXT NOT NULL, "
                                                + STOP_COL_NUMBER + " VARCHAR, "
                                                + STOP_COL_LAT + " VARCHAR NOT NULL, "
                                                + STOP_COL_LON + " VARCHAR NOT NULL" + ");";

  private static final String TABLE_ROUTES_CREATE = "CREATE TABLE " + TABLE_ROUTES + " ("
                                                + ROUTE_COL_ID + " VARCHAR PRIMARY KEY, "
                                                + ROUTE_COL_NUMBER + " VARCHAR, "
                                                + ROUTE_COL_NAME + " TEXT NOT NULL, "
                                                + ROUTE_COL_SCHEDULED_TIME + " DATETIME NOT NULL, "
                                                + ROUTE_COL_ESTIMATED_TIME + " DATETIME, "
                                                + ROUTE_COL_STOP_ID + " VARCHAR, "
                                                + "FOREIGN KEY(" + ROUTE_COL_STOP_ID
                                                  + ") REFERENCES " + TABLE_STOPS + "("
                                                  + STOP_COL_ID + ")"
                                                + ");";

  //Drop table statement
  private static final String DROP_TABLE_STOPS = "DROP TABLE IF EXISTS " + TABLE_STOPS;
  private static final String DROP_TABLE_ROUTES = "DROP TABLE IF EXISTS " + TABLE_ROUTES;

  //constructor
  public DBHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
    // TODO Auto-generated constructor stub
  }

  //when you create the class, create the table
  @Override
  public void onCreate(SQLiteDatabase db)
  {
    // execute the create table code
    db.execSQL(TABLE_STOPS_CREATE);
    db.execSQL(TABLE_ROUTES_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
  {
    //drop the table and recreate it
    db.execSQL(DROP_TABLE_STOPS);
    db.execSQL(DROP_TABLE_ROUTES);
    onCreate(db);
  }

  // Insert values using content values
  public void insertStopValues(String name, String id, String number, double lat, double lon)
  {
    //get an instance of a writable database
    SQLiteDatabase db = this.getWritableDatabase();

    //create an instance of ContentValues to add to the database
    //the ContentValues class is used to store sets of values that
    //are easier to process
    ContentValues insertValues = new ContentValues();

    //Add values to the ContentValues:
    //insertValues.put(ColumnName, value);
    insertValues.put(STOP_COL_NAME, name);
    insertValues.put(STOP_COL_ID, id);
    insertValues.put(STOP_COL_NUMBER, number);
    insertValues.put(STOP_COL_LAT, lat);
    insertValues.put(STOP_COL_LON, lon);

    //insert the values into the table
    db.insert(TABLE_STOPS, null, insertValues);

    //close the database
    db.close();
  }

  // Insert values using content values
  public void insertRoutesValues(String name, String number,
                                 String id, String scheduled_time,
                                 String estimated_time, String stopId)
  {
    //get an instance of a writable database
    SQLiteDatabase db = this.getWritableDatabase();

    //create an instance of ContentValues to add to the database
    //the ContentValues class is used to store sets of values that
    //are easier to process
    ContentValues insertValues = new ContentValues();

    //Add values to the ContentValues:
    //insertValues.put(ColumnName, value);
    insertValues.put(ROUTE_COL_ID, id);
    insertValues.put(ROUTE_COL_NAME, name);
    insertValues.put(ROUTE_COL_NUMBER, number);
    insertValues.put(ROUTE_COL_SCHEDULED_TIME, scheduled_time);
    insertValues.put(ROUTE_COL_ESTIMATED_TIME, estimated_time);
    insertValues.put(ROUTE_COL_STOP_ID, stopId);

    //insert the values into the table
    db.insert(TABLE_ROUTES, null, insertValues);

    //db.execSQL("UPDATE routes SET estimated_time = datetime('now', 'locale') WHERE stop_id = 10409");

    //close the database
    db.close();
  }

  // Load the data in the table
  public ArrayList<Transit.Stop> loadDataStops()
  {
    ArrayList<Transit.Stop> stopArrayList = new ArrayList<>();
    //open the readable database
    SQLiteDatabase db = this.getReadableDatabase();
    //create an array of the table names
    String[] selection = {STOP_COL_NAME, STOP_COL_NUMBER, STOP_COL_LAT, STOP_COL_LON};
    //Create a cursor item for querying the database
    Cursor c = db.query(TABLE_STOPS,	//The name of the table to query
            selection,				//The columns to return
            null,					//The columns for the where clause
            null,					//The values for the where clause
            null,					//Group the rows
            null,					//Filter the row groups
            null);					//The sort order

    //Move to the first row
    c.moveToFirst();

    //For each row that was retrieved
    for(int i=0; i < c.getCount(); i++)
    {
      //assign the value to the corresponding array
      Transit transit = new Transit();
      Transit.Stop stop = transit.new Stop();
      stop.name = c.getString(0);
      stop.number = c.getString(1);
      stop.latitude = c.getDouble(2);
      stop.longitude = c.getDouble(3);

      stopArrayList.add(stop);

      c.moveToNext();
    }

    //close the cursor
    c.close();

    //close the database
    db.close();

    return stopArrayList;
  }

  // Load the data in the table
  public Transit.Stop loadDataStop(String stopId)
  {
    //open the readable database
    SQLiteDatabase db = this.getReadableDatabase();
    //create an array of the table names
    String[] selection = {STOP_COL_NAME, STOP_COL_NUMBER, STOP_COL_LAT, STOP_COL_LON};
    //Create a cursor item for querying the database
    Cursor c = db.query(TABLE_STOPS,	//The name of the table to query
            selection,				//The columns to return
            STOP_COL_ID + " =?",					//The columns for the where clause
            new String[]{stopId},					//The values for the where clause
            null,					//Group the rows
            null,					//Filter the row groups
            null);					//The sort order

    //Move to the first row
    c.moveToFirst();

    //assign the value to the corresponding array
    Transit transit = new Transit();
    Transit.Stop stop = transit.new Stop();
    stop.name = c.getString(0);
    stop.number = c.getString(1);
    stop.latitude = c.getDouble(2);
    stop.longitude = c.getDouble(3);

    //close the cursor
    c.close();

    //close the database
    db.close();

    return stop;
  }

  // Load the data in the table
  public ArrayList<Transit.Bus> loadDataRoutes(String stopId)
  {
    ArrayList<Transit.Bus> routeArrayList = new ArrayList<>();
    //open the readable database
    SQLiteDatabase db = this.getReadableDatabase();
    //create an array of the table names
    String[] selection = {ROUTE_COL_NAME, ROUTE_COL_NUMBER,
                          ROUTE_COL_SCHEDULED_TIME, ROUTE_COL_SCHEDULED_TIME};
    //Create a cursor item for querying the database
    Cursor c = db.query(TABLE_ROUTES,	//The name of the table to query
                        selection,				        //The columns to return
                        ROUTE_COL_STOP_ID + " =?",		    //The columns for the where clause
                        new String[]{stopId}, //The values for the where clause
                        null,					    //Group the rows
                        null,					    //Filter the row groups
                        null);					  //The sort order

    //Move to the first row
    c.moveToFirst();

    //For each row that was retrieved
    for(int i=0; i < c.getCount(); i++)
    {
      //assign the value to the corresponding array
      Transit transit = new Transit();
      Transit.Bus bus = transit.new Bus();
      bus.variantName = c.getString(0);
      bus.number = c.getString(1);
      bus.scheduledTime = c.getString(2);
      bus.estimatedTime = c.getString(3);

      routeArrayList.add(bus);

      c.moveToNext();
    }

    //close the cursor
    c.close();

    //close the database
    db.close();

    return routeArrayList;
  }

  public void deleteStop(String stopId)
  {
    //get an instance of a writable database
    SQLiteDatabase db = this.getWritableDatabase();

    db.delete(TABLE_STOPS, STOP_COL_ID + " = " + stopId, null);
  }

  public void deleteOldRoutes(String stopId)
  {
    // Get current time
    String current_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA).format(new Date());
    Log.d("Test", current_time);

    //get an instance of a writable database
    SQLiteDatabase db = this.getWritableDatabase();

    int result = db.delete(TABLE_ROUTES,
              ROUTE_COL_STOP_ID + " = " + stopId + " AND "
                 + ROUTE_COL_ESTIMATED_TIME + " < Datetime('" + current_time + "')", null);

    Log.d("Test", "Delete result: " + result);
  }
}
