package io.geeteshk.sensor.view.adapter

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.polidea.rxandroidble2.RxBleDevice
import io.geeteshk.sensor.R
import io.geeteshk.sensor.view.adapter.model.DeviceModel
import io.geeteshk.sensor.view.ui.PairActivity
import kotlinx.android.synthetic.main.item_device.view.*
import java.util.*

class DevicesAdapter(ctx: Context) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    private val devicesList = ArrayList<DeviceModel>()
    private val context = ctx

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = devicesList.size

    fun add(deviceModel: DeviceModel) {
        if (!hasDevice(deviceModel.device)) {
            devicesList.add(deviceModel)
            devicesList.sortByDescending { it.rssi }
            notifyDataSetChanged()
        }
    }

    private fun hasDevice(device: RxBleDevice): Boolean {
        devicesList.forEach {
            if (it.device.macAddress == device.macAddress) {
                return true
            }
        }

        return false
    }

    fun clear() {
        devicesList.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devicesList[position].device
        val name = device.name ?: "Unknown"

        holder.setName(name)
        holder.setAddress(device.macAddress)
        holder.setListener(View.OnClickListener {
            val pairedIntent = Intent(context, PairActivity::class.java)
            pairedIntent.putExtra(ADDRESS_EXTRA, device.macAddress)
            context.startActivity(pairedIntent)
        })

        holder.setSignalStrength(devicesList[position].rssi)
    }

    companion object {
        const val ADDRESS_EXTRA = "io.geeteshk.sensor.view.adapter.ADDRESS_EXTRA"
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun setName(name: String) {
            view.deviceName.text = name
        }

        fun setAddress(address: String) {
            view.deviceAddress.text = address
        }

        fun setListener(listener: View.OnClickListener) {
            view.cardView.setOnClickListener(listener)
        }

        fun setSignalStrength(rssi: Int) {
            when {
                rssi >= -67 -> view.signalStrength.setImageResource(R.drawable.ic_signal_cellular_3)
                rssi >= -70 -> view.signalStrength.setImageResource(R.drawable.ic_signal_cellular_2)
                rssi >= -80 -> view.signalStrength.setImageResource(R.drawable.ic_signal_cellular_1)
                else -> view.signalStrength.setImageResource(R.drawable.ic_signal_cellular_outline)
            }
        }
    }
}