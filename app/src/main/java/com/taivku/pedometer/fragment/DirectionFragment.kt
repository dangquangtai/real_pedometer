package com.taivku.pedometer.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.BuildConfig
import com.google.maps.android.PolyUtil
import com.taivku.pedometer.map.DirectionResponses





import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_direction.*
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.ParticleSystem
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.KParameter
import com.taivku.pedometer.R
import com.taivku.pedometer.database.PersonalDatabase
import com.taivku.pedometer.database.PersonalDatabaseDAO
import com.taivku.pedometer.database.PersonalInfo


class DirectionFragment : Fragment() {
    private lateinit var btn_find: Button
    private lateinit var btn_start: Button
    private lateinit var edt_address: EditText
    private lateinit var textDistance: TextView
    private var lastKnownLocation: Location? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationUpdatesCallback: LocationCallback? = null
    private lateinit var locationRequest: LocationRequest
    private var isStart = false
    lateinit var map: GoogleMap
    private val listLocation = ArrayList<LatLng>()
    private var sumDistance = 0.0
    lateinit var chronometermap: Chronometer
    lateinit var displayDistance: TextView
    lateinit var displayTime: TextView
    lateinit var displaySpeed: TextView
    lateinit var displayCalo: TextView
    lateinit var btnShare: ImageView
    lateinit var btnMusic: ImageView
    private var runing = false
    var pauseOffset: Long = 0L
    lateinit var database1: PersonalDatabaseDAO
    var myPersonalInfo: PersonalInfo? = null
    var gender: Int = 1
    var age: Int = 1
    var height: Int = 0
    var weight: Int = 0
    lateinit var linearLayout: LinearLayout
    lateinit var konfettiView: KonfettiView


    @SuppressLint("MissingPermission", "UseRequireInsteadOfGet")
    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                // Set the map's camera position to the current location of the device.
                lastKnownLocation = task.result
                if (lastKnownLocation != null) {
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            ), 17F
                        )
                    )
                    googleMap?.isMyLocationEnabled = true
                    googleMap?.uiSettings?.isMyLocationButtonEnabled = true
                }
            }
        }
        val apiServices = RetrofitClient.apiServices()

        btn_find.setOnClickListener {
            val location = edt_address.text.toString()
            if (location.isNotEmpty()) {
                var addressList: List<Address> = ArrayList()
                val geoCoder = Geocoder(requireContext())
                try {
                    addressList = geoCoder.getFromLocationName(location, 1)
                } catch (e: Exception) {
                }
                if (addressList.isNotEmpty() && lastKnownLocation != null) {
                    val address = addressList[0]
                    val from =
                        lastKnownLocation!!.latitude.toString() + "," + lastKnownLocation!!.longitude.toString()
                    val end = address.latitude.toString() + "," + address.longitude.toString()
                    apiServices.getDirection(
                        from,
                        end,
                        "bike",
                        "A3Sh66gqzl8692hfcneIAXC6bS4YY7YxEr90bo7x"
                    )
                        .enqueue(object : Callback<DirectionResponses> {
                            override fun onResponse(
                                call: Call<DirectionResponses>,
                                response: Response<DirectionResponses>
                            ) {
                                drawPolyline(response, googleMap)
                                val marker = MarkerOptions().position(
                                    LatLng(
                                        address.latitude,
                                        address.longitude
                                    )
                                ).icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_AZURE
                                    )
                                )
                                map.addMarker(marker)

                            }

                            override fun onFailure(
                                call: Call<DirectionResponses>,
                                t: Throwable
                            ) {

                            }
                        })
                }
                else{
                    Toast.makeText(context,"Không tìm thấy địa điểm",Toast.LENGTH_SHORT).show()
                }
            }


        }
        btn_start.setOnClickListener {
            if (lastKnownLocation != null) {
                var calculateCalo: Float = 0f
                if (!isStart) {

                    googleMap.clear()
                    listLocation.clear()
                    btn_start.text = "Stop"
                    displayDistance.text = "0,0"
                    displayTime.text = "00:00"
                    displaySpeed.text = "0,0"
                    displayCalo.text = "0,00"
                    btnShare.isInvisible = true
                    sumDistance = 0.0
                    startChronometer()
                    isStart = true
                } else {
                    konfettiView.build()
                        .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                        .setDirection(0.0, 359.0)
                        .setSpeed(1f, 5f)
                        .setFadeOutEnabled(true)
                        .setTimeToLive(2000L)
                        .addShapes(Shape.Square, Shape.Circle)
                        .addSizes(Size(12))
                        .setPosition(-50f, konfettiView.width + 50f, -50f, -50f)
                        .streamFor(300, 5000L)
                    btn_start.text = "Start"
                    btnShare.visibility = view!!.visibility
                    pauseChronometer()
                    displayDistance.text = BigDecimal(sumDistance).setScale(
                        2,
                        RoundingMode.HALF_EVEN
                    ).toString()
                    displayTime.text = chronometermap.text.toString()
                    if (pauseOffset != 0L) {
                        val caculateSpeed =
                            ((sumDistance) / (pauseOffset / 1000f))


                        displaySpeed.text = BigDecimal(caculateSpeed).setScale(
                            2,
                            RoundingMode.HALF_EVEN
                        ).toString()
                    }
                    if (sumDistance == 0.0) {
                        calculateCalo = 0f
                    } else {

                        calculateCalo = calculateEnergyExpenditure(
                            height,
                            age,
                            weight,
                            gender,
                            pauseOffset,
                            sumDistance.toFloat()
                        )
                    }
                    displayCalo.text = BigDecimal(calculateCalo.toDouble()).setScale(
                        2,
                        RoundingMode.HALF_EVEN
                    ).toString()
                    textDistance.text = "Distance: 0 m"
                    chronometermap.setBase(SystemClock.elapsedRealtime())
                    isStart = false
                }
            } else {
                Toast.makeText(context, "Not known your location!", Toast.LENGTH_SHORT).show()
            }
        }
        btnShare.setOnClickListener {
            try {

                val bitmap = Bitmap.createBitmap(
                    linearLayout.width,
                    linearLayout.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                linearLayout.draw(canvas)

//                val bitmap: Bitmap = getBimapFromView(linearLayout)

                val file = File(context!!.externalCacheDir, "bac.png")
                val fOut = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fOut)
                fOut.flush()
                fOut.close()
                file.setReadable(true, false)
                val intent = Intent(Intent.ACTION_SEND)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val photoURI = FileProvider.getUriForFile(
                    context!!.applicationContext,
                    BuildConfig.APPLICATION_ID.toString() + ".provider",
                    file
                )

                intent.putExtra(Intent.EXTRA_STREAM, photoURI)

                intent.type = "image/png"
                intent.putExtra(Intent.EXTRA_SUBJECT, "subject here")
                startActivity(Intent.createChooser(intent, "Share using"))
            } catch (e: FileNotFoundException) {
                Log.i("error", e.message.toString())
            }


        }
        btnMusic.setOnClickListener {
            val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
            startActivity(intent)
        }



    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_direction, container, false)
        val application = requireNotNull(this.activity).application
        btn_find = root.findViewById(R.id.btn_find)
        btn_start = root.findViewById(R.id.btnStart)
        edt_address = root.findViewById(R.id.edt_address)
        textDistance = root.findViewById(R.id.textDistance)
        chronometermap = root.findViewById(R.id.chronometerMap)
        displayDistance = root.findViewById(R.id.distanceMap)
        displayTime = root.findViewById(R.id.timeMap)
        displaySpeed = root.findViewById(R.id.speedMap)
        displayCalo = root.findViewById(R.id.caloMap)
        btnShare = root.findViewById(R.id.btnShare)
        btnMusic = root.findViewById(R.id.btnMusic)
        linearLayout = root.findViewById(R.id.showLayout)
        konfettiView = root.findViewById(R.id.viewKonfetti)

        database1 = PersonalDatabase.getInstance(application).personalDatabaseDAO
        val personalInfo = database1.getListPersonalInfo()
        personalInfo.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    val thisPersonalInfo = it[0]
                    myPersonalInfo = it[0]
                    age = thisPersonalInfo.age.toInt()
                    weight = thisPersonalInfo.weight.toInt()
                    height = thisPersonalInfo.height.toInt()

                    if (thisPersonalInfo.sex.equals("female")) {
                        gender = 0
                    }

                } else {
                    myPersonalInfo = PersonalInfo()
                    viewLifecycleOwner.lifecycleScope.launch {
                        database1.insert(myPersonalInfo!!)
                    }
                    Toast.makeText(context, "null data", Toast.LENGTH_SHORT).show()
                }
            }
        })

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        setUpLocationRequest()
        setUpLocationUpdatesCallback()
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationUpdatesCallback,
            null
        )
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationUpdatesCallback)
        super.onDestroy()
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationUpdatesCallback)
        super.onStop()
    }

    private fun drawPolyline(response: Response<DirectionResponses>, map: GoogleMap) {
        if (response.body().routes?.size!! > 0) {
            val route = response.body().routes?.get(0)
            val shape = route?.overviewPolyline?.points
            val polyline = PolylineOptions()
                .addAll(PolyUtil.decode(shape))
                .width(9f)
                .color(Color.BLUE)
            map.addPolyline(polyline)
        }

    }

    private interface ApiServices {
        @GET(("Direction"))
        fun getDirection(
            @Query("origin") origin: String,
            @Query("destination") destination: String,
            @Query("vehicle") vehicle:String,
            @Query("api_key") apiKey: String
        ): Call<DirectionResponses>
    }

    private object RetrofitClient {
        fun apiServices(): DirectionFragment.ApiServices {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://rsapi.goong.io/")
                .build()
            return retrofit.create<DirectionFragment.ApiServices>(DirectionFragment.ApiServices::class.java)
        }
    }

    private fun setUpLocationUpdatesCallback() {
        locationUpdatesCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult != null) {
                    lastKnownLocation = locationResult.lastLocation
                    if (isStart) {

                        if (listLocation.size > 0) {
                            val lc = Location("")
                            lc.latitude = listLocation.last().latitude
                            lc.longitude = listLocation.last().longitude
                            val tempDistance = lc?.distanceTo(lastKnownLocation)
                            Log.i("tag", "distace $tempDistance")
                            if (tempDistance > 1.0) {
                                sumDistance += tempDistance
                                textDistance.text = "Distance: " + BigDecimal(sumDistance).setScale(
                                    2,
                                    RoundingMode.HALF_EVEN
                                ).toString() + "m"
                            }
                        }
                        listLocation.add(
                            LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            )
                        )
                        map.clear()
                        val polyline1 = map.addPolyline(
                            PolylineOptions()
                                .clickable(true)
                                .addAll(listLocation)
                        )
                        polyline1.color = Color.GREEN
                        polyline1.width = 8f

                    }
                    Log.i("tag", "update location")
                } else {
                    Log.i("tag", "Location null")
                }
            }
        }
    }

    private fun setUpLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun startChronometer() {
        if (!runing) {
            chronometerMap.setBase(SystemClock.elapsedRealtime())
            chronometerMap.start()
            runing = true
        }

    }

    private fun pauseChronometer() {
        if (runing) {

            chronometerMap.stop()
            pauseOffset = SystemClock.elapsedRealtime() - chronometerMap.base
            runing = false
        }

    }

    private fun harrisBenedictRmr(
        gender: Int,
        weightKg: Int,
        age: Int,
        heightCm: Int
    ): Float {
        return if (gender == 0) {
            655.0955f + 1.8496f * heightCm + 9.5634f * weightKg - 4.6756f * age
        } else {
            66.4730f + 5.0033f * heightCm + 13.7516f * weightKg - 6.7550f * age
        }
    }

    private fun convertKilocaloriesToMlKmin(kilocalories: Float, weightKgs: Float): Float {
        var kcalMin = kilocalories / 1440
        kcalMin /= 5f
        return kcalMin / weightKgs * 1000
    }

    private fun calculateEnergyExpenditure(
        height: Int,
        age: Int?,
        weight: Int,
        gender: Int,
        durationInSeconds: Long,
        distanceTravel: Float,

        ): Float {

        val harrisBenedictRmR = convertKilocaloriesToMlKmin(
            harrisBenedictRmr(gender, weight, age!!, height),
            weight.toFloat()
        )
        val kmTravelled: Float = distanceTravel / 1000f
        val hours: Float = (durationInSeconds / 1000f) / 3600f
        val speedInMph: Float = kmTravelled * 0.62137f
        val metValue: Float = getMetForActivity(speedInMph)
        val constant = 3.5f
        val correctedMets = metValue * (constant / harrisBenedictRmR)



        return correctedMets * hours * weight
    }

    private fun getMetForActivity(speedInMph: Float): Float {
        if (speedInMph < 2.0) {
            return 2.0f
        } else if (java.lang.Float.compare(speedInMph, 2.0f) == 0) {
            return 2.8f
        } else if (java.lang.Float.compare(speedInMph, 2.0f) > 0 && java.lang.Float.compare(
                speedInMph,
                2.7f
            ) <= 0
        ) {
            return 3.0f
        } else if (java.lang.Float.compare(speedInMph, 2.8f) > 0 && java.lang.Float.compare(
                speedInMph,
                3.3f
            ) <= 0
        ) {
            return 3.5f
        } else if (java.lang.Float.compare(speedInMph, 3.4f) > 0 && java.lang.Float.compare(
                speedInMph,
                3.5f
            ) <= 0
        ) {
            return 4.3f
        } else if (java.lang.Float.compare(speedInMph, 3.5f) > 0 && java.lang.Float.compare(
                speedInMph,
                4.0f
            ) <= 0
        ) {
            return 5.0f
        } else if (java.lang.Float.compare(speedInMph, 4.0f) > 0 && java.lang.Float.compare(
                speedInMph,
                4.5f
            ) <= 0
        ) {
            return 7.0f
        } else if (java.lang.Float.compare(speedInMph, 4.5f) > 0 && java.lang.Float.compare(
                speedInMph,
                5.0f
            ) <= 0
        ) {
            return 8.3f
        } else if (java.lang.Float.compare(speedInMph, 5.0f) > 0) {
            return 9.8f
        }
        return 0f

    }

    @SuppressLint("ResourceAsColor")
    private fun getBimapFromView(view: View): Bitmap {
        val returnBitmap: Bitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas()
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)

        } else {
            canvas.drawColor(android.R.color.white)

        }
        view.draw(canvas)
        return returnBitmap
    }


}


//private operator fun Any.plus(s: String): String {
//
//}

//private operator fun String.invoke(): Any {
//
//}
