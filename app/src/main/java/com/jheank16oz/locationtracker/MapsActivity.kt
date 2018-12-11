package com.jheank16oz.locationtracker

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var locations:ArrayList<Location>? = null
    private var markers = ArrayList<MarkerOptions>()
    private var progress:ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locations = intent.getParcelableArrayListExtra(LOCATIONS_KEY)

        loading(true)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val latlngs = LatLngBounds.Builder()


        mMap.setOnMapLoadedCallback {
            //Your code where exception occurs goes here...
            locations?.let {
                it.forEach { location ->
                    val icon = bitmapDescriptorFromVector(if (location.type == "true") R.drawable.ic_circle_warning else R.drawable.ic_circle_success)
                    try {
                        val latlng = LatLng(location.latitude?.toDouble()!!, location.longitude?.toDouble()!!)
                        val marker = MarkerOptions().position(latlng).icon(icon)
                        markers.add(marker)
                        mMap.addMarker(marker)
                        latlngs.include(latlng)
                    }catch (e:NumberFormatException){
                        e.printStackTrace()
                    }
                }


            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latlngs.build(),10))
            loading(false)

        }

    }

    private fun bitmapDescriptorFromVector( vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = getDrawable(vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth/4, vectorDrawable.intrinsicHeight/4)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth/4, vectorDrawable.intrinsicHeight/4, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    companion object {
        const val LOCATIONS_KEY = "locations"
    }


    private fun loading(loader: Boolean){
        if (progress == null){
            progress = ProgressDialog(this)
            progress?.setCancelable(false)
            progress?.isIndeterminate = true
            progress?.setMessage("Cargando marcadores ... ")
        }

        if (loader){
            progress?.show()
        }else{
            progress?.dismiss()
        }
    }
}
