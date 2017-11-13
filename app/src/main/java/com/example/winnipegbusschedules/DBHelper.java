package com.example.winnipegbusschedules;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final String STOP_COL_LAT = "latitude";
  private static final String STOP_COL_LON = "longitude";

  //Create constants defining your routes table column names
  private static final String ROUTE_COL_NUMBER = "number";
  private static final String ROUTE_COL_NAME = "name";
  private static final String ROUTE_COL_SCHEDULED_TIME = "scheduled_time";
  private static final String ROUTE_COL_ESTIMATED_TIME = "estimated_time";
  private static final String ROUTE_COL_STOP_ID = "stop_id";

  //Define the database version
  private static final int DB_VERSION = 2;

  //Define your create statement in typical sql format
  //CREATE TABLE {Tablename} (
  //Colname coltype
  //)
  private static final String TABLE_STOPS_CREATE = "CREATE TABLE " + TABLE_STOPS + " ("
                                                + STOP_COL_ID + " INTEGER PRIMARY KEY, "
                                                + STOP_COL_NAME + " TEXT NOT NULL, "
                                                + STOP_COL_LAT + " VARCHAR NOT NULL, "
                                                + STOP_COL_LON + " VARCHAR NOT NULL" + ");";

  private static final String TABLE_ROUTES_CREATE = "CREATE TABLE " + TABLE_ROUTES + " ("
                                                + ROUTE_COL_NUMBER + " INTEGER, "
                                                + ROUTE_COL_NAME + " TEXT NOT NULL, "
                                                + ROUTE_COL_SCHEDULED_TIME + " DATETIME NOT NULL, "
                                                + ROUTE_COL_ESTIMATED_TIME + " DATETIME, "
                                                + ROUTE_COL_STOP_ID + " INTEGER, "
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
  public void insertStopValues(String name, int id, double lat, double lon)
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
    insertValues.put(STOP_COL_LAT, lat);
    insertValues.put(STOP_COL_LON, lon);

    //insert the values into the table
    db.insert(TABLE_STOPS, null, insertValues);

    //close the database
    db.close();
  }

  // Insert values using content values
  public void insertRoutesValues(String name, int number, String scheduled_time,
                                 String estimated_time, int stopId)
  {
    //get an instance of a writable database
    SQLiteDatabase db = this.getWritableDatabase();

    //create an instance of ContentValues to add to the database
    //the ContentValues class is used to store sets of values that
    //are easier to process
    ContentValues insertValues = new ContentValues();

    //Add values to the ContentValues:
    //insertValues.put(ColumnName, value);
    insertValues.put(ROUTE_COL_NAME, name);
    insertValues.put(ROUTE_COL_NUMBER, number);
    insertValues.put(ROUTE_COL_SCHEDULED_TIME, scheduled_time);
    insertValues.put(ROUTE_COL_ESTIMATED_TIME, estimated_time);
    insertValues.put(ROUTE_COL_STOP_ID, stopId);

    //insert the values into the table
    db.insert(TABLE_ROUTES, null, insertValues);

    //close the database
    db.close();
  }

//  public void saveExec(String name, int age)
//  {
//    //Open your writable database
//    SQLiteDatabase db = this.getWritableDatabase();
//
//    //Formulate your statement
//    String insertStatement = "INSERT INTO 'People' VALUES('" + name +"'," + age + ");";
//
//    //Execute your statement
//    db.execSQL(insertStatement);
//
//    db.close();
//  }

  // Load the data in the table
  public ArrayList<Transit.Stop> loadDataStops(){

    ArrayList<Transit.Stop> stopArrayList = new ArrayList<>();
    //open the readable database
    SQLiteDatabase db = this.getReadableDatabase();
    //create an array of the table names
    String[] selection = {STOP_COL_NAME, STOP_COL_ID, STOP_COL_LAT, STOP_COL_LON};
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
  public ArrayList<Transit.Bus> loadDataRoutes(int stopId)
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
                        new String[]{Integer.toString(stopId)}, //The values for the where clause
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

  //This method is used to load the data from the table into a hash map
  //this enables the use of multiple textviews in the listview
  public List<Map<String,String>> loadData2()
  {
    List<Map<String,String>> lm = new ArrayList<Map<String,String>>();

    //open the readable database
    SQLiteDatabase db = this.getReadableDatabase();
    //create an array of the table names
    String[] selection = {STOP_COL_NAME, STOP_COL_ID, STOP_COL_LAT, STOP_COL_LON};
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
      Map<String,String> map = new HashMap<String,String>();
      //assign the value to the corresponding array
      map.put("Name", c.getString(0));
      map.put("Age", String.valueOf(c.getInt(1)));

      lm.add(map);
      c.moveToNext();
    }

    //close the cursor
    c.close();
    //close the database
    db.close();
    return lm;

  }
}
