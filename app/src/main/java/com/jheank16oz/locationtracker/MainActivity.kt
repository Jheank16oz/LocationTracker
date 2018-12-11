package com.jheank16oz.locationtracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {


    /**
     * Code used in requesting runtime permissions.
     */
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    /**
     * Constant used in the location settings dialog.
     */
    private val REQUEST_CHECK_SETTINGS = 0x1

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000


    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient: SettingsClient? = null

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private var mLocationRequest: LocationRequest? = null

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    /**
     * Callback for Location events.
     */
    private var mLocationCallback: LocationCallback? = null

    /**
     * Represents a geographical location.
     */
    private var mCurrentLocation: Location? = null

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private var mRequestingLocationUpdates: Boolean? = null

    /**
     * Time when the location was updated represented as a String.
     */
    private var mTimer: CountDownTimer? = null


    private var mDocRef:CollectionReference? = null
    private lateinit var db:AppDatabase
    private var started =  false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            if (!started) {
                started = true
                startLocationUpdates()
                Toast.makeText(this,"start",Toast.LENGTH_SHORT).show()
                fab.setImageDrawable(resources.getDrawable(android.R.drawable.ic_media_pause))
                mTimer?.start()
            }else{
               stopUpdates()

            }
        }
        db = Room.databaseBuilder(
                this,
                AppDatabase::class.java, "location-database"
        ).build()

        load.setOnClickListener {
            Thread(Runnable {
                val list:List<com.jheank16oz.locationtracker.Location> = db.userDao().getAll()
                runOnUiThread {
                    locationText.text = list.toString()
                }


            }).start()
        }

        map.setOnClickListener { _ ->
            Thread(Runnable {
                val list = db.userDao().getAll()
                if (list.isNotEmpty()) {
                    val i = Intent(this, MapsActivity::class.java).also {
                        it.putParcelableArrayListExtra(MapsActivity.LOCATIONS_KEY, ArrayList(list))
                    }
                    startActivity(i)
                }else{
                    runOnUiThread {
                        Toast.makeText(baseContext,"No se tienen ubicaciones",Toast.LENGTH_SHORT).show()

                    }
                }


            }).start()

        }

        delete.setOnClickListener{ it ->

            AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("¿Eliminar todo?")
                    .setMessage("Esta apunto de eliminar todo")
                    .setPositiveButton("eliminar") { p0, p1 ->
                        Thread(Runnable {
                            db.userDao().clearAll()
                            runOnUiThread {
                                locationText.text = ""
                                Toast.makeText(baseContext,"Eliminado.",Toast.LENGTH_SHORT).show()
                            }
                        }).start()
                    }.setNegativeButton("cancelar") { _, _ ->
                        // ignored
                    }.show()

        }

        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = settings
        mDocRef = firestore.collection("miercoles")

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()

        load.callOnClick()
    }

    private fun stopUpdates() {
        started = false
        stopForegroundService()
        stopLocationUpdates()
        mTimer?.cancel()

        fab.setImageDrawable(resources.getDrawable(android.R.drawable.ic_media_play))

        Toast.makeText(this,"stop",Toast.LENGTH_SHORT).show()

    }


    /**
     * Sets up the location request. Android has two location request settings:
     * `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     *
     *
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     *
     *
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest?.fastestInterval = 0
        mLocationRequest?.smallestDisplacement = 5.0f

        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                mCurrentLocation = locationResult?.lastLocation
                updateLocationUI()
            }
        }
    }


    /**
     * Uses a [com.google.android.gms.location.LocationSettingsRequest.Builder] to build
     * a [com.google.android.gms.location.LocationSettingsRequest] that is used for checking
     * if a device has the needed location settings.
     */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i("this ", "User agreed to make required location settings changes.")
                Activity.RESULT_CANCELED -> {
                    Log.i("this ", "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                    updateUI()
                }
            }// Nothing to do. startLocationupdates() gets called in onResume again.
        }
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient?.checkLocationSettings(mLocationSettingsRequest)
                ?.addOnSuccessListener(this) {
                    Log.i("this ", "All location settings are satisfied.")

                    mFusedLocationClient?.requestLocationUpdates(mLocationRequest,
                            mLocationCallback, Looper.myLooper())

                    updateUI()
                }
                ?.addOnFailureListener(this) { e ->
                    val statusCode = (e as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i(
                                    "this ", "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i("this ", "PendingIntent unable to execute request.")
                            }

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                            Log.e("this ", errorMessage)
                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                            mRequestingLocationUpdates = false
                        }
                    }

                    updateUI()
                }


        createChronometer(20000)
        startNotification()

    }

    private fun startNotification() {
        val i = Intent(this, MyForeGroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i)
        } else {
            startService(i)
        }
    }

    private fun stopForegroundService(){
        val stopIntent = Intent(this@MainActivity, MyForeGroundService::class.java)
        stopIntent.action = MyForeGroundService.STOPFOREGROUND_ACTION
        startService(stopIntent)

    }

    /**
     * Updates all UI fields.
     */
    private fun updateUI() {
        updateLocationUI()
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private fun updateLocationUI() {
        if (mCurrentLocation != null && mRequestingLocationUpdates == true) {



            save(Location(mCurrentLocation?.latitude.toString(), mCurrentLocation?.longitude.toString(),
                    Date().toString(), "false"))
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if (mRequestingLocationUpdates != true) {
            Log.d("this", "stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
                ?.addOnCompleteListener(this) {
                    mRequestingLocationUpdates = false
                }

    }

    public override fun onResume() {
        super.onResume()
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.

        if ( mRequestingLocationUpdates == true && checkPermissions()) {
            startLocationUpdates()
        } else if (!checkPermissions()) {
            requestPermissions()
        }

        updateUI()
    }

    override fun onPause() {
        super.onPause()

        // Remove location updates to save battery.
        // stopLocationUpdates()
    }



    /**
     * Shows a [Snackbar].
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {
        Snackbar.make(
                findViewById<View>(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("this ", "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, View.OnClickListener {
                // Request permission
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        } else {
            Log.i("this ", "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdates()

    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i("this ", "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("this ", "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates == true) {
                    Log.i("this ", "Permission granted, updates requested, starting location updates")
                    startLocationUpdates()
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, View.OnClickListener {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                })
            }
        }
    }

    private fun createChronometer(time: Long) {

        if(mTimer == null) {
            Log.e("created", "****")
            mTimer = object : CountDownTimer(time, 1000) {

                override fun onTick(millisUntilFinished: Long) {
                    Log.e("tick ", (millisUntilFinished / 1000).toString() + "")
                }

                override fun onFinish() {
                    if (ActivityCompat.checkSelfPermission(applicationContext,
                                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }


                    mFusedLocationClient?.lastLocation
                            ?.addOnSuccessListener { location: Location? ->
                                save(Location(location?.latitude.toString(), location?.longitude.toString(),
                                        Date().toString(), "true"))
                            }


                }
            }
        }

    }

    fun save(location: com.jheank16oz.locationtracker.Location?){
        mTimer?.cancel()
        mTimer?.start()

        if (location == null){
            validateGps()
            return
        }
        Thread(Runnable {
            db.userDao().insert(location)
            runOnUiThread {
                locationText?.let {
                    val text = location.toString() + (it.text.toString())
                    it.text = text
                }
            }
        }).start()
    }

    private fun validateGps(){
        val manager: LocationManager =  getSystemService( Context.LOCATION_SERVICE ) as LocationManager

        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Toast.makeText(this,"Encienda el gps", Toast.LENGTH_SHORT).show()
        }
    }


}
