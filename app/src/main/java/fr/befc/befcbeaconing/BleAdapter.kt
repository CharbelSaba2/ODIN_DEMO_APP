package fr.befc.befcbeaconing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView


class BleAdapter(private val context: Context, private val dataSource: Array<String>, var counter: Int = 0) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var packets = 1

    override fun getCount(): Int {
    return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.list_item_beacons, parent, false)

        val titleTextView = rowView.findViewById(R.id.titleTextView) as TextView

        val thumbnailImageView = rowView.findViewById(R.id.beaconImageView) as ImageView

        val beaconStatusCard = rowView.findViewById(R.id.beaconStatusCard) as CardView

        val beaconStatusText = rowView.findViewById(R.id.beaconStatusText) as TextView

        val beaconCount = rowView.findViewById(R.id.beaconCountTextView) as TextView

        val beacon = dataSource[position]
        val id1: String
        val id2: String
        var foundID = false

        if(beacon.length > 2){
            id1 = beacon.subSequence(2, 22).toString()
            id2 = beacon.subSequence(25, beacon.length).toString()
            val decryptedId = decryptId((id1+id2).decodeHex())

            titleTextView.text = "Beacon ID : $decryptedId"
            val sharedPreferences = context.getSharedPreferences("Beacons", Context.MODE_PRIVATE)


            val foundIDs = sharedPreferences.getString("beaconIDs", "")
            val ids = foundIDs!!.split("=")
            for(i in ids.indices){
                if(Integer.parseInt(ids[i]) == Integer.parseInt(decryptedId)){
                    foundID = true
                    break
                }else{
                    foundID = false
                }
            }

            val storedIds = sharedPreferences.getString("storedBeacons", "")!!.split("=")
            for(i in storedIds.indices){
                val m = storedIds[i].split(":")
                if(Integer.parseInt(decryptedId) == Integer.parseInt(m[0])){
                    beaconCount.text = "Packets: ${m[1]}"
                    packets = Integer.parseInt(m[1])
                }
            }
        }





        if (foundID) {
            thumbnailImageView.setImageResource(R.drawable.befc_logo_blue)
            beaconStatusText.text = "Original"
            beaconStatusCard.setCardBackgroundColor(Color.parseColor("#FF4CAF50"))
        }
        else if (dataSource.contentEquals(arrayOf("--"))) {
            thumbnailImageView.visibility = View.INVISIBLE
            beaconStatusText.text = "Copy"
            beaconStatusCard.setCardBackgroundColor(Color.parseColor("#FFC71A1A"))
        }
        else {
            thumbnailImageView.setImageResource(R.drawable.eddystone)
            beaconStatusText.text = "Copy"
            beaconStatusCard.setCardBackgroundColor(Color.parseColor("#FFC71A1A"))
        }

        return rowView
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

}