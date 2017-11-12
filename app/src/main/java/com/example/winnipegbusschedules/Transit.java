package com.example.winnipegbusschedules;

/**
 * Created by Denis on 12/11/2017.
 * Class that contains the classes related to the
 * Winnipeg Transit classes
 */

public class Transit
{
  // Class that represents a bus in the list
  public class Bus
  {
    public String number;
    public String variantName;
    public String scheduledTime;
    public String estimatedTime;

    public Bus()
    {
      number = variantName = scheduledTime = estimatedTime = "";
    }

    @Override
    public boolean equals(Object obj)
    {
      // To Do: comparison by time
      return super.equals(obj);
    }
  }
}
