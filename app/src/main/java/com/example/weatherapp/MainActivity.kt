package com.example.weatherapp

import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private var locationByGps: Location? = null
    private var locationByNetwork: Location? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var addresses: List<Address>? = null
    private var isLocationSearched: Boolean = false
    private var units: String = "imperial"
    private lateinit var city: String
    private val API: String = "ab994ab9dfeb6900f29b66b59b58f209"
    private var localTimeZone: Long = 0
    private var searchedTimeZone: Long = 0
    private var timeZone: Long = 0

    private val imperialUnits = object {
        val degreeUnits = "°F"
        val speedUnits = "mil/h"
    }
    private val metricUnits = object {
        val degreeUnits = "°C"
        val speedUnits = "met/s"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pullToRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pullToRefresh.setOnRefreshListener {
            weatherTask().execute()
            pullToRefresh.isRefreshing = false
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isLocationPermissionGranted()
        weatherTask().execute()

        val unitsButton = findViewById<Button>(R.id.units_button)
        unitsButton.text = "°C"

        unitsButton.setOnClickListener {
            if (units.equals("imperial")) {
                units = "metric"
                unitsButton.text = "°F"
                weatherTask().execute()
            } else {
                units = "imperial"
                unitsButton.text = "°C"
                weatherTask().execute()
            }
        }

        val currentLocationButton = findViewById<ImageButton>(R.id.current_location_button)
        currentLocationButton.setOnClickListener {
            isLocationPermissionGranted()
            weatherTask().execute()
        }

        val locationSearch: EditText = findViewById(R.id.location_search)
        locationSearch.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {

                    city = locationSearch.text.toString()
                    isLocationSearched = true
                    locationSearch.text = null

                    val searchKeyboard = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    searchKeyboard.hideSoftInputFromWindow(
                        locationSearch.applicationWindowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )

                    weatherTask().execute()
                    true
                }
                else -> false
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) ,1
            )
            Log.i("location_message", "Couldn't get location")
            city = "New York"
            false
        } else {
            val lastKnownLocationByGps =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocationByGps?.let {
                locationByGps = lastKnownLocationByGps
            }
            val lastKnownLocationByNetwork =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnownLocationByNetwork?.let {
                locationByNetwork = lastKnownLocationByNetwork
            }

            if (locationByGps == null && locationByNetwork != null) {
                getLocationByNetwork()

            } else if (locationByNetwork == null && locationByGps != null) {
                getLocationByGPS()

            } else if (locationByGps != null && locationByNetwork != null) {
                if (locationByGps!!.accuracy > locationByNetwork!!.accuracy) {
                    getLocationByGPS()
                } else {
                    getLocationByNetwork()
                }
            }
            true
        }
    }

    private fun getLocationByGPS() {
        try{
            currentLocation = locationByGps
            latitude = currentLocation?.latitude
            longitude = currentLocation?.longitude
            val geocoder = Geocoder(this, Locale.getDefault())
            addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
            city = addresses!![0].locality
            Log.i("location_message", "Could get location by GPS, city: $city, lat: $latitude, long: $longitude")
        } catch (e: Exception) {
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.search_layout).visibility = View.VISIBLE
            findViewById<TextView>(R.id.error_text).visibility = View.VISIBLE
        }
    }

    private fun getLocationByNetwork() {
        try {
            currentLocation = locationByNetwork
            latitude = currentLocation?.latitude
            longitude = currentLocation?.longitude
            val geocoder = Geocoder(this, Locale.getDefault())
            addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
            city = addresses!![0].locality
            Log.i("location_message", "Could get location by network, city: $city, lat: $latitude, long: $longitude")
        } catch (e: Exception) {
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.search_layout).visibility = View.VISIBLE
            findViewById<TextView>(R.id.error_text).visibility = View.VISIBLE
        }
    }

    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.search_layout).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.main_container).visibility = View.GONE
            findViewById<TextView>(R.id.error_text).visibility = View.GONE
        }

        override fun doInBackground(vararg p0: String?): String? {
            return try {
                if (isLocationSearched) {
                    URL("https://api.openweathermap.org/data/2.5/weather?q=$city&&units=$units&appid=$API")
                        .readText(Charsets.UTF_8)
                } else {
                    URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&&units=$units&appid=$API")
                        .readText(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                Log.i("location_message", "City: $city")
                val jsonObj = JSONObject(result!!)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt: Long = jsonObj.getLong("dt")
                val updatedAtText = "Updated at: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", 
                    Locale.ENGLISH).format(Date(updatedAt * 1000))}"
                val tempInt = main.getInt("temp")
                val temp = "$tempInt"
                val tempMinInt = main.getInt("temp_min")
                val tempMin = "Min Temp: $tempMinInt"
                val tempMaxInt = main.getInt("temp_max")
                val tempMax = "Max Temp: $tempMaxInt"
                val pressure = main.getString("pressure") + "hPa"
                val humidity = main.getString("humidity") + "%"
                if (isLocationSearched) {
                    searchedTimeZone = jsonObj.getLong("timezone")
                } else {
                    localTimeZone = jsonObj.getLong("timezone")
                }
                if (isLocationSearched) {
                    timeZone = localTimeZone - searchedTimeZone
                }
                val sunrise: Long = sys.getLong("sunrise") - timeZone
                val sunset: Long = sys.getLong("sunset") - timeZone
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name") + ", " + sys.getString("country")

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                val tempValue = findViewById<TextView>(R.id.temp)
                val tempMinValue = findViewById<TextView>(R.id.temp_min)
                val tempMaxValue = findViewById<TextView>(R.id.temp_max)
                if (units == "metric") {
                    tempValue.text = temp + metricUnits.degreeUnits
                    tempMinValue.text = tempMin + metricUnits.degreeUnits
                    tempMaxValue.text = tempMax + metricUnits.degreeUnits
                } else {
                    tempValue.text = temp + imperialUnits.degreeUnits
                    tempMinValue.text = tempMin + imperialUnits.degreeUnits
                    tempMaxValue.text = tempMax + imperialUnits.degreeUnits
                }

                findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a",
                    Locale.getDefault()).format(Date(sunrise * 1000))
                findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a",
                    Locale.getDefault()).format(Date(sunset * 1000))
                val windSpeedValue = findViewById<TextView>(R.id.wind)
                if (units == "metric") {
                    windSpeedValue.text = windSpeed + metricUnits.speedUnits
                } else {
                    windSpeedValue.text = windSpeed + imperialUnits.speedUnits
                }
                findViewById<TextView>(R.id.pressure).text = pressure
                findViewById<TextView>(R.id.humidity).text = humidity

                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<ConstraintLayout>(R.id.search_layout).visibility = View.VISIBLE
                findViewById<RelativeLayout>(R.id.main_container).visibility = View.VISIBLE
            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<ConstraintLayout>(R.id.search_layout).visibility = View.VISIBLE
                findViewById<TextView>(R.id.error_text).visibility = View.VISIBLE
            }
        }
    }
}