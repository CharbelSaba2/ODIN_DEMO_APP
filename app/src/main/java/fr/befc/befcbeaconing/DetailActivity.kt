package fr.befc.befcbeaconing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView

class DetailActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#2271BB")))
        window?.statusBarColor = Color.parseColor("#2271BB")
        supportActionBar?.setIcon(R.drawable.logo_black)

        val beaconIDTextView : TextView = findViewById(R.id.beaconTextView)
        val beaconStatusTextView : TextView = findViewById(R.id.beaconStatusTextView)
        val beaconImage : ImageView = findViewById(R.id.beaconCheckImageView)
        val beaconBackgroundColor : CardView = findViewById(R.id.beaconCardView)
        val backButton : Button = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            onStop()
        }


        val ascci = intent.getStringExtra("beacon")?.decodeHex()
        val sharedPreferences = getSharedPreferences("Beacons", Context.MODE_PRIVATE)
        val savedIDs = sharedPreferences.getString("beaconIDs", "")
        val ids = savedIDs!!.split("=")
        val decryptedBeaconId = decryptId(ascci!!)
        var foundID = false

        for(i in ids.indices){
            if(Integer.parseInt(decryptedBeaconId) == Integer.parseInt(ids[i])){
                foundID = true
                break
            }else{
                foundID = false
            }
        }

        if(foundID){
            beaconImage.setImageResource(R.drawable.ic_baseline_check_24)
            beaconBackgroundColor.setCardBackgroundColor(Color.parseColor("#FF4CAF50"))
            beaconStatusTextView.text = "Status: Original"
        }else{
            beaconImage.setImageResource(R.drawable.ic_baseline_close_24)
            beaconBackgroundColor.setCardBackgroundColor(Color.parseColor("#FFC71A1A"))
            beaconStatusTextView.text = "Status: Copy"
        }
        beaconIDTextView.text = "Beacon ID: $decryptedBeaconId"
    }

    private fun String.decodeHex(): String {
        require(length % 2 == 0) {"Must have an even length"}
        return String(
            chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        )
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onStop() {
        super.onStop()
        finish()
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
}