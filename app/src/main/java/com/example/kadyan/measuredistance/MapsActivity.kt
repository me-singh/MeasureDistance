package com.example.kadyan.measuredistance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_maps.*
import com.google.firebase.database.DataSnapshot
import kotlinx.android.synthetic.main.polyline_form_layout.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
        LocationListener,ValueEventListener{

    companion object {
        private const val TAG="MapsActivity"
        private const val FINE_LOCATION=Manifest.permission.ACCESS_FINE_LOCATION
        private const val LOCATION_PERMISSION_REQUEST_CODE=123
    }

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager:LocationManager

    private var buttonEnabled=1

    private var counts: Int = 0
    private var distance: Double = 0.0
    private var collectionsOfArrayLists = ArrayList< ArrayList<LatLong> >()
    private var locationsArrayList=ArrayList<LatLong>()
    private var isSaved=false

    private var polygonPoints=0
    private var polygonPointsList = ArrayList<LatLng>()
    private var collectionsOfPolygonArrayLists = ArrayList< ArrayList<LatLong> >()//TODO

    private lateinit var firebaseDatabase:FirebaseDatabase
    private lateinit var databaseReference:DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(toolbar)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.root
        checkPermissionGranted()

        pointButton.setOnClickListener({
            buttonEnabled=1
            mMap.clear()

            pointProperties.visibility=View.VISIBLE
            polyLinesProperties.visibility=View.GONE
            polygonProperties.visibility=View.GONE
        })
        lineButton.setOnClickListener({
            buttonEnabled=2
            mMap.clear()
            reInitialize()
            collectionsOfArrayLists.clear()

            pointProperties.visibility=View.GONE
            polyLinesProperties.visibility=View.VISIBLE
            polygonProperties.visibility=View.GONE
        })
        polygonButton.setOnClickListener({
            buttonEnabled=3
            mMap.clear()
            polygonPointsList.clear()
            polygonPoints=0
            collectionsOfPolygonArrayLists.clear()

            pointProperties.visibility=View.GONE
            polyLinesProperties.visibility=View.GONE
            polygonProperties.visibility=View.VISIBLE
        })
    }



    private fun checkPermissionGranted() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(applicationContext, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "FINE LOCATION PERMISSION IS GRANTED", Toast.LENGTH_SHORT).show()
            initMap()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION)) {
                Toast.makeText(this, "FINE LOCATION PERMISSION IS MANDATORY", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "PERMISSION IS DENIED", Toast.LENGTH_SHORT).show()
                    return
                }
                initMap()
            }
        }
    }



    private fun initMap() {
        Toast.makeText(this, "FINE LOCATION PERMISSION IS GRANTED", Toast.LENGTH_SHORT).show()
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.setOnMapClickListener(this)

        //TODO 2
        mMap.setOnPolylineClickListener { polyline ->
            Toast.makeText(baseContext,"POLYLINE CLICKED",Toast.LENGTH_SHORT).show()
            addPolyLinesProperties.setOnClickListener {
                //TODO open form
                showDialog(it)

            }
        }

//        mMap.setOnMarkerClickListener {
//            Toast.makeText(baseContext,"MARKER CLICKED",Toast.LENGTH_SHORT).show()
//            return@setOnMarkerClickListener true
//        }

        //TODO 3
        mMap.setOnPolygonClickListener { polygon ->
            Toast.makeText(baseContext,"POLYGON CLICKED",Toast.LENGTH_SHORT).show()
            addPolygonProperties.setOnClickListener{
                //TODO open form
                showDialog(it)
                if (it.tag !=null)
                    Toast.makeText(baseContext,""+it.tag,Toast.LENGTH_SHORT).show()
            }
        }

        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
            Toast.makeText(this,"WORKING",Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


//    private fun getMarkerIconFromDrawable(drawable: Drawable): BitmapDescriptor {
//        val canvas = Canvas()
//        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
//        canvas.setBitmap(bitmap)
//        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
//        drawable.draw(canvas)
//        return BitmapDescriptorFactory.fromBitmap(bitmap)
//    }


    override fun onMapClick(latLng: LatLng?) {
//        val camPos=CameraPosition.Builder().target(latLng).zoom(10f).build()
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos))
        if (!isSaved) {
            val latLong=LatLong(latLng!!.latitude,latLng.longitude)
            when (buttonEnabled) {
                1 -> addPoints(latLong)
                2 -> addDistanceAndLines(latLong)
                3 -> addPolygons(latLng)
            }
        }
    }




    private fun addPoints(latLong: LatLong) {
        mMap.addMarker(MarkerOptions()
                .position(LatLng(latLong.latitude,latLong.longitude))
                .title("Random"))
        val pointsReference=databaseReference.child("points")
        pointsReference.push().setValue(latLong)
        pointsReference.addValueEventListener(this)
    }


    private fun addDistanceAndLines(latLong:LatLong){
//        val circleDrawable = resources.getDrawable(R.drawable.ic_fiber_manual_record_black_24dp)
//        val markerIcon = getMarkerIconFromDrawable(circleDrawable)
        val result = FloatArray(1)
        if (counts==0) {
            locationsArrayList.add(latLong)
        }else{
            val previousLatLng = locationsArrayList[locationsArrayList.size-1]
            mMap.addPolyline(PolylineOptions().clickable(true).add(LatLng(latLong.latitude,latLong.longitude),
                    LatLng(previousLatLng.latitude,previousLatLng.longitude)))
            locationsArrayList.add(latLong)
            Location.distanceBetween(previousLatLng.latitude,previousLatLng.longitude,latLong.latitude,latLong.longitude,result)
        }
        distance+=result[0]/1609.344
        mMap.addMarker(MarkerOptions()
                .position(LatLng(latLong.latitude,latLong.longitude))
                .title("Distance : "+ String.format("%.2f",distance) + " mi"))
//                .setIcon(markerIcon)
        counts++
    }


    private fun addPolygons(latLng: LatLng) {
        if (polygonPoints==0){
            polygonPointsList.add(latLng)
        } else{
            mMap.clear()
//            val firstPoint=polygonPointsList[0]
//            val previousPoint=polygonPointsList[polygonPointsList.size-1]
            polygonPointsList.add(latLng)
//            Log.e(TAG,""+latLong.latitude+" "+latLong.longitude+"    "+
//                    previousPoint.latitude+" "+previousPoint.longitude+"    "+
//                    firstPoint.latitude+" "+firstPoint.longitude)
            mMap.addPolygon(PolygonOptions().clickable(true).addAll(polygonPointsList)).fillColor=Color.GRAY
        }
        mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title("Random"))
        polygonPoints++
        Log.e(TAG,""+polygonPoints)
    }





    private fun showDialog(it: View) {
        val builder = AlertDialog.Builder(this)
        val view = this.layoutInflater.inflate(R.layout.polyline_form_layout, null)
//        if(it.tag != null){
//            editTextFirstProperty.setText(it.tag.toString())
//        }
        builder.setView(view)
                .setTitle("Enter Properties:")
                .setPositiveButton("OK", { dialog, which ->
                    val first=findViewById<EditText>(R.id.editTextFirstProperty)
                    val second=findViewById<EditText>(R.id.editTextSecondProperty)
//                    val new1=first.text.toString()
//                    val tag=first.text.toString()+""+second.text.toString()
                    it.tag = "ABC"
//                    Toast.makeText(baseContext,""+tag,Toast.LENGTH_SHORT).show()
                })
                .setNegativeButton("Cancel", { dialog, which ->
                    Toast.makeText(baseContext,"Cancel CLICKED",Toast.LENGTH_SHORT).show()
                })
        val dialog = builder.create()
        dialog.show()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_undo -> {
                if(locationsArrayList.size!=0 && !isSaved){
                    doUndo()
                }
                true
            }
            R.id.action_save -> {
                Toast.makeText(this,"SAVED",Toast.LENGTH_SHORT).show()
                if(!isSaved){
                    if (buttonEnabled==2){
                        val temp=ArrayList<LatLong>()
                        temp.addAll(locationsArrayList)
                        collectionsOfArrayLists.add(temp)

                        val polylinesReference=databaseReference.child("polylines")
                        polylinesReference.push().setValue(locationsArrayList)
                        polylinesReference.addValueEventListener(this)
                        reInitialize()
                    }else if (buttonEnabled==3){
                        val polygonReference=databaseReference.child("polylines")
                        polygonReference.push().setValue(locationsArrayList)
                        polygonReference.addValueEventListener(this)
                        reInitialize()
                    }

                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun doUndo() {

        val newLocations=ArrayList<LatLong>()
        newLocations.addAll(locationsArrayList)
        val n=newLocations.size

        mMap.clear()
        for (arrayList in collectionsOfArrayLists){
            val temp=ArrayList<LatLong>()
            temp.addAll(arrayList)
            reInitialize()
            for (i in temp.indices){
                val latLong=temp[i]
                Log.e(TAG+"1",""+latLong.latitude+" "+latLong.longitude)
                addDistanceAndLines(LatLong(latLong.latitude,latLong.longitude))
            }
        }

        reInitialize()
        newLocations.removeAt(n-1)
        for (i in newLocations.indices){
            val latLong=newLocations[i]
            Log.e(TAG+"2",""+latLong.latitude+" "+latLong.longitude)
            addDistanceAndLines(LatLong(latLong.latitude,latLong.longitude))
        }
    }


    private fun reInitialize(){
        locationsArrayList.clear()
        counts=0
        distance=0.0
    }







    override fun onCancelled(error: DatabaseError) {
        // Failed to read value
        Log.w(TAG, "Failed to read value.", error.toException())
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        // This method is called once with the initial value and again
        // whenever data at this location is updated.

//        val genericTypeIndicator = object : GenericTypeIndicator<ArrayList<LatLong>>() {}
//
//        val arrayList = ArrayList< ArrayList<LatLong> >()
//        var cnt=0
//        for (child in dataSnapshot.children) {
//            arrayList.add(child.getValue(genericTypeIndicator)!!)
//            Log.e(TAG, "ARRAY LIST ADDED "+ arrayList[cnt].size)
//            cnt++
//        }
        Log.e(TAG, "ADDED TO DB ")
    }




    override fun onLocationChanged(location: Location?) {
        Toast.makeText(this, "Latitude: " + location?.latitude + "\n Longitude: " + location?.longitude, Toast.LENGTH_SHORT).show()
        val latLng = LatLng(location!!.latitude,location.longitude)
        val camPos = CameraPosition.Builder()
                .target(latLng)
                .zoom(9f)
                .bearing(location.bearing)
                .build()
        val camUpd3 = CameraUpdateFactory.newCameraPosition(camPos)
        mMap.animateCamera(camUpd3)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Toast.makeText(this,"CLICKED: "+ "onStatusChanged",Toast.LENGTH_SHORT).show()
    }

    override fun onProviderEnabled(provider: String?) {
        Toast.makeText(this,"CLICKED: "+ "onProviderEnabled",Toast.LENGTH_SHORT).show()
    }

    override fun onProviderDisabled(provider: String?) {
        Toast.makeText(this, "Please Enable GPS", Toast.LENGTH_SHORT).show()
    }

}
