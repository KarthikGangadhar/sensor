package io.geeteshk.sensor.view.adapter.model

import com.polidea.rxandroidble2.RxBleDevice

data class DeviceModel(val device: RxBleDevice, val rssi: Int)