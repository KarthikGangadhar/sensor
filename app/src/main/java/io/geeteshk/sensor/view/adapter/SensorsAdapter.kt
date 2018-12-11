package io.geeteshk.sensor.view.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.geeteshk.sensor.R
import kotlinx.android.synthetic.main.item_sensor.view.*

class SensorsAdapter(ctx: Context) : RecyclerView.Adapter<SensorsAdapter.ViewHolder>() {

    private val context = ctx
    private var sensorCount = 0

    interface SensorClickListener {
        fun onSensorClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sensor, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = sensorCount

    fun setup() {
        sensorCount = 128
        notifyDataSetChanged()
    }

    fun clear() {
        sensorCount = 0
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setNumber(position + 1)
        /*holder.setInfo("Unknown")
        holder.setListener(View.OnClickListener {
            val intent = Intent(context, SensorActivity::class.java)
            intent.putExtra(EXTRA_SENSOR_NUM, position + 1)
            context.startActivity(intent)
        })*/
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun setNumber(number: Int) {
            val text = "Sensor $number"
            view.sensorID.text = text
        }

        /*fun setInfo(info: String) {
            view.sensorInfo.text = info
        }

        fun setListener(listener: View.OnClickListener) {
            view.sensorRoot.setOnClickListener(listener)
        }*/
    }

    companion object {
        const val EXTRA_SENSOR_NUM = "io.geeteshk.sensor.view.adapter.EXTRA_SENSOR_NUM"
    }
}