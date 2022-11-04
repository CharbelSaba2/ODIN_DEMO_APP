package fr.befc.befcbeaconing

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.lifecycle.Observer
import fr.befc.befcbeaconing.databinding.ActivityMainBinding
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var neverAskAgainPermissions = ArrayList<String>()
    private lateinit var beFCBeaconingApplication: BeaconReferenceApplication
    private lateinit var binding: ActivityMainBinding
    private var alertDialog: AlertDialog? = null
    private lateinit var beaconListView: ListView
    private lateinit var detail: Intent
    private var beaconAdapterData = ArrayList<String>()
    private var oldBeacons: Collection<Beacon>? = null
    private lateinit var myBleAdapter: BleAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#2271BB")))
        supportActionBar?.setIcon(R.drawable.logo_black)
        window?.statusBarColor = Color.parseColor("#2271BB")

        //The IDs that are saved for the checks later on
        val ids = ArrayList<Int>()
        ids.add(75775102)
        ids.add(95832850)
        val sharedPreferences = getSharedPreferences("Beacons", Context.MODE_PRIVATE)
        var allIDs = ""

        for(i in ids.indices){
            if(i == 0){
                allIDs += ids[i]
            }else{
                allIDs += "=" + ids[i]
            }
        }
        sharedPreferences.edit().putString("beaconIDs", allIDs).apply()


        beFCBeaconingApplication = application as BeaconReferenceApplication
        beaconListView = binding.beaconList
        binding.beaconCount.text = "No beacons detected"
        beaconListView.adapter = BleAdapter(this, arrayOf("--"))


        // Set up a Live Data observer for beacon data
        val regionViewModel = BeaconManager.getInstanceForApplication(this)
            .getRegionViewModel(beFCBeaconingApplication.region)
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        regionViewModel.regionState.observe(this, monitoringObserver)
        // observer will be called each time a new list of beacons is ranged (typically ~1 second in the foreground)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)

        binding.swiperefresh.setOnRefreshListener {
            oldBeacons = null
            binding.swiperefresh.isRefreshing = false
            beaconListView.adapter = BleAdapter(this, arrayOf("--"))
            sharedPreferences.edit().putString("storedBeacons", "").apply()
        }

        beaconListView.setOnItemClickListener { _, _, position, _ ->

            if (beaconAdapterData.isNotEmpty()) {
                val id1 = beaconAdapterData[position].subSequence(2, 22).toString()
                val id2 = beaconAdapterData[position].subSequence(25, beaconAdapterData[position].length).toString()
                detail = Intent(applicationContext, DetailActivity::class.java)
                detail.putExtra("beacon", id1 + id2)
                startActivity(detail)
            }
        }
        binding.beaconCount.setOnClickListener {
        }
    }


    @SuppressLint("SetTextI18n")
    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
            binding.beaconCount.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"
            val sharedPreferences = getSharedPreferences("Beacons", Context.MODE_PRIVATE)
            val storedBeacons = sharedPreferences.getString("storedBeacons", "")!!

            val detectedBeacons = beacons.sortedBy { it.distance }.map { "${it.id1} ${it.id2}" }.toTypedArray()

            if(storedBeacons.isNotEmpty()){
                val b = storedBeacons.split("=")
                var p = storedBeacons


                for(i in b.indices){
                    val a = b[i].split(":")
                    for(j in detectedBeacons.indices){
                        val id1 = detectedBeacons[j].subSequence(2, 22).toString()
                        val id2 = detectedBeacons[j].subSequence(25, detectedBeacons[j].length).toString()
                        val id = decryptId((id1 + id2).decodeHex())

                        if(id.isNotEmpty() && a[0].isNotEmpty()){
                            if(!p.contains(id)){
                                p += if(p.isEmpty()) {
                                    "$id:1"
                                }else {
                                    "=$id:1"
                                }
                            }
                            if(Integer.parseInt(a[0]) == Integer.parseInt(id)){
                                val v = Integer.parseInt(a[1])
                                if(p.isNotEmpty()) {

                                    p = p.replace("$id:$v", "$id:${v+1}")
                                }
                            }
                        }
                    }
                }
                sharedPreferences.edit().putString("storedBeacons", p).apply()
                Log.d("Charbel55", sharedPreferences.getString("storedBeacons", "")!!)
            }else{
                var s = ""
                for(i in detectedBeacons.indices){
                    val id1 = detectedBeacons[i].subSequence(2, 22).toString()
                    val id2 = detectedBeacons[i].subSequence(25, detectedBeacons[i].length).toString()
                    s += if(s.isEmpty()){
                        "${decryptId((id1 + id2).decodeHex())}:1"
                    }else{
                        "=${decryptId((id1 + id2).decodeHex())}:1"
                    }

                }
                sharedPreferences.edit().putString("storedBeacons", s).apply()
                Log.d("Charbel56", s)
            }

            if (oldBeacons.isNullOrEmpty() || oldBeacons!!.size < beacons.size ) {


                myBleAdapter = BleAdapter(this, beacons.sortedBy { it.distance }.map { "${it.id1} ${it.id2}" }.toTypedArray())

                for (i in 0 until myBleAdapter.count) {
                    if (!beaconAdapterData.contains(myBleAdapter.getItem(i).toString())) {
                        beaconAdapterData.add(myBleAdapter.getItem(i).toString())
                    }
                }
                oldBeacons = beacons

                beaconListView.adapter = myBleAdapter
            }
            if (oldBeacons!!.contains(beacons.elementAtOrNull(0)) || oldBeacons!!.contains(beacons.elementAtOrNull(1)) || oldBeacons!!.contains(beacons.elementAtOrNull(2))) {
                myBleAdapter.counter ++
                beaconListView.adapter = myBleAdapter
            }

        }
    }

    @SuppressLint("SetTextI18n")
    private val monitoringObserver = Observer<Int> { state ->
        var dialogTitle = "Beacons detected"
        var dialogMessage = "didEnterRegionEvent has fired"
        val stateString = "inside"
        if (state == MonitorNotifier.OUTSIDE) {
            dialogTitle = "No beacons detected"
            dialogMessage = "didExitRegionEvent has fired"
//            stateString == "outside"
            binding.beaconCount.text = "Outside of the beacon region -- no beacons detected"
            //beaconListView.adapter = BleAdapter(this, arrayOf("--"))
        } else {
            binding.beaconCount.text = "Inside the beacon region."
        }
        Log.d(TAG, "monitoring state changed to : $stateString")
        val builder =
            AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        builder.setMessage(dialogMessage)
        builder.setPositiveButton(android.R.string.ok, null)
        alertDialog?.dismiss()
        alertDialog = builder.create()
        //alertDialog?.show()
    }

    private fun String.decodeHex(): String {
        require(length % 2 == 0) {"Must have an even length"}
        return String(
            chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        )
    }

    private fun decryptId(id : String) : String{
        val cutAscci = id.substring(0, 8)
        var decryptedBeaconId = ""
        for(i in cutAscci.indices){
            when(cutAscci[i]){
                '0' -> decryptedBeaconId += "7"
                '1' -> decryptedBeaconId += "5"
                '2' -> decryptedBeaconId += "4"
                '3' -> decryptedBeaconId += "8"
                '4' -> decryptedBeaconId += "9"
                '5' -> decryptedBeaconId += "0"
                '6' -> decryptedBeaconId += "2"
                '7' -> decryptedBeaconId += "3"
                '8' -> decryptedBeaconId += "6"
                '9' -> decryptedBeaconId += "1"
            }
        }
        return decryptedBeaconId
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("Beacons", Context.MODE_PRIVATE).edit().putString("storedBeacons", "").apply()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("Beacons", Context.MODE_PRIVATE).edit().putString("storedBeacons", "").apply()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in 1 until permissions.size) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                //check if user select "never ask again" when denying any permission
                if (!shouldShowRequestPermissionRationale(permissions[i])) {
                    neverAskAgainPermissions.add(permissions[i])
                }
            }
        }
    }


    private fun checkPermissions() {
        // base permissions are for M and higher
        var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        var permissionRationale =
            "This app needs fine location permission to detect beacons.  Please grant this now."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
            )
            permissionRationale =
                "This app needs fine location permission, and bluetooth scan permission to detect beacons.  Please grant all of these now."
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionRationale =
                    "This app needs fine location permission to detect beacons.  Please grant this now."
            } else {
                permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                permissionRationale =
                    "This app needs background location permission to detect beacons in the background.  Please grant this now."
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            permissionRationale =
                "This app needs both fine location permission and background location permission to detect beacons in the background.  Please grant both now."
        }
        var allGranted = true
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) allGranted =
                false
        }
        if (!allGranted) {
            if (neverAskAgainPermissions.isEmpty()) {
                val builder =
                    AlertDialog.Builder(this)
                builder.setTitle("This app needs permissions to detect beacons")
                builder.setMessage(permissionRationale)
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    requestPermissions(
                        permissions,
                        PERMISSION_REQUEST_FINE_LOCATION
                    )
                }
                builder.show()
            } else {
                val builder =
                    AlertDialog.Builder(this)
                builder.setTitle("Functionality limited")
                builder.setMessage("Since location and device permissions have not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location and device discovery permissions to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { }
                builder.show()
            }
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        val builder =
                            AlertDialog.Builder(this)
                        builder.setTitle("This app needs background location access")
                        builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener {
                            requestPermissions(
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                PERMISSION_REQUEST_BACKGROUND_LOCATION
                            )
                        }
                        builder.show()
                    } else {
                        val builder =
                            AlertDialog.Builder(this)
                        builder.setTitle("Functionality limited")
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener { }
                        builder.show()
                    }
                }
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S &&
                (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                    val builder =
                        AlertDialog.Builder(this)
                    builder.setTitle("This app needs bluetooth scan permission")
                    builder.setMessage("Please grant scan permission so this app can detect beacons.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener {
                        requestPermissions(
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                            PERMISSION_REQUEST_BLUETOOTH_SCAN
                        )
                    }
                    builder.show()
                } else {
                    val builder =
                        AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since bluetooth scan permission has not been granted, this app will not be able to discover beacons  Please go to Settings -> Applications -> Permissions and grant bluetooth scan permission to this app.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            val builder =
                                AlertDialog.Builder(this)
                            builder.setTitle("This app needs background location access")
                            builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                            builder.setPositiveButton(android.R.string.ok, null)
                            builder.setOnDismissListener {
                                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                    PERMISSION_REQUEST_BACKGROUND_LOCATION
                                )
                            }
                            builder.show()
                        } else {
                            val builder =
                                AlertDialog.Builder(this)
                            builder.setTitle("Functionality limited")
                            builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                            builder.setPositiveButton(android.R.string.ok, null)
                            builder.setOnDismissListener { }
                            builder.show()
                        }
                    }
                }
            }
        }

    }

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_BACKGROUND_LOCATION = 0
        const val PERMISSION_REQUEST_BLUETOOTH_SCAN = 1
//        const val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2
        const val PERMISSION_REQUEST_FINE_LOCATION = 3
    }
}