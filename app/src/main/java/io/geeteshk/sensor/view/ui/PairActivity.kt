package io.geeteshk.sensor.view.ui

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import biz.kasual.materialnumberpicker.MaterialNumberPicker
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.geeteshk.sensor.App
import io.geeteshk.sensor.R
import io.geeteshk.sensor.util.DataHandler
import io.geeteshk.sensor.util.DataHandler.Companion.dataToMap
import io.geeteshk.sensor.view.RoundedBottomSheetDialogFragment
import io.geeteshk.sensor.view.adapter.DevicesAdapter
import io.geeteshk.sensor.view.adapter.SensorsAdapter
import io.geeteshk.sensor.view.ui.table.TableFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_pair.*
import timber.log.Timber
import java.io.Serializable
import java.util.*

class PairActivity : RxAppCompatActivity() {

    private lateinit var adapter: SensorsAdapter
    private var deviceAddress = ""

    private lateinit var bleDevice: RxBleDevice

    private lateinit var connectionObservable: Observable<RxBleConnection>

    private var currentSensor = 1
    private lateinit var sensorItem: MenuItem
    private lateinit var saveItem: MenuItem

    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair)
        setSupportActionBar(toolbar)

        deviceAddress = intent.getStringExtra(DevicesAdapter.ADDRESS_EXTRA)
        bleDevice = App.bleClient.getBleDevice(deviceAddress)

        supportActionBar?.title = bleDevice.name ?: "Unknown"
        adapter = SensorsAdapter(this)

        // Initialize the connection state
        connectionObservable = bleDevice.establishConnection(false)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .compose(ReplayingShare.instance())

        // Try and establish a connection
        connectionObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("Connection established successfully!")
                    writeData("S001")
                    setupSensorChart()
                    pauseData.setOnClickListener { _ ->
                        isPaused = if (isPaused) {
                            saveItem.isEnabled = false
                            pauseData.setImageResource(R.drawable.ic_pause)
                            sensorChart.data.clearValues()
                            sensorValue.text = ""
                            false
                        } else {
                            // Write the exit code for the previous sensor to break the loop
                            writeData("#")

                            saveItem.isEnabled = true
                            pauseData.setImageResource(R.drawable.ic_refresh)
                            true
                        }
                    }

                    setupNotify()
                }, {
                    Timber.e(it)
                    finish()
                    Toast.makeText(this, "Connection closed by BLE device. Please try again.", Toast.LENGTH_SHORT).show()
                }, {
                    Timber.d("Connection finished.")
                })
    }

    // Are we connected to a BLE device
    private fun isConnected() = bleDevice.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED

    // Start listening for data changes in BLE
    private fun setupNotify() {
        if (isConnected()) {
            connectionObservable
                    .flatMap { it.setupNotification(READ_UUID) }
                    .flatMap { it }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        runOnUiThread {
                            if (!isPaused) {
                                addEntry(String(it))
                            }
                        }
                    }, {
                        Timber.e(it)
                    })
        }
    }

    // Sending data to BLE
    private fun writeData(input: String) {
        if (isConnected()) {
            connectionObservable
                    .firstOrError()
                    .flatMap {
                        it.writeCharacteristic(WRITE_UUID, input.toByteArray())
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({ data ->
                        Timber.d("WRITE: %s", String(data))
                    }, { err ->
                        Timber.e(err)
                    })
        }
    }

    // Function for setting up UI chart library
    private fun setupSensorChart() {
        sensorChart.apply {
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            description.isEnabled = false
            legend.isEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    onTouchListener.setLastHighlighted(null)
                    highlightValues(null)
                }

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val x = e!!.x
                    val y = e.y
                    val text = "x = $x\ny = $y"
                    sensorValue.text = text
                }
            })
        }

        val data = LineData()
        data.setValueTextColor(Color.BLACK)
        sensorChart.data = data

        val xAxis = sensorChart.xAxis
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisLineColor = Color.BLACK
        xAxis.axisLineWidth = 1.5F
        xAxis.isEnabled = true

        val leftAxis = sensorChart.axisLeft
        leftAxis.axisMinimum = 0F
        leftAxis.axisLineWidth = 1.5F
        leftAxis.axisLineColor = Color.BLACK
        leftAxis.isEnabled = true

        val rightAxis = sensorChart.axisRight
        rightAxis.isEnabled = false
    }

    // Used to add a data point to the chart once we get a value
    private fun addEntry(stringExtra: String) {
        val data = sensorChart.data
        if (data != null) {
            var set = data.getDataSetByIndex(0)
            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            val value = (stringExtra.substring(stringExtra.indexOf(":") + 1, stringExtra.indexOf("K"))).toFloat()
            val x = set.entryCount.toFloat()
            val text = "x = $x\ny = $value"
            sensorValue.text = text
            data.addEntry(Entry(x, value), 0)
            data.notifyDataChanged()

            sensorChart.notifyDataSetChanged()
            sensorChart.setVisibleXRangeMaximum(20F)
            sensorChart.moveViewToX(data.entryCount.toFloat())
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic Data")
        set.apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = ContextCompat.getColor(this@PairActivity, R.color.colorPrimary)
            setCircleColor(ContextCompat.getColor(this@PairActivity, R.color.colorAccent))
            lineWidth = 2F
            circleRadius = 4F
            setCircleColorHole(ContextCompat.getColor(this@PairActivity, R.color.colorAccent))
            fillAlpha = 0
            valueTextColor = Color.BLACK
            valueTextSize = 9f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
            highlightLineWidth = 1.2F
        }

        return set
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pair, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        sensorItem = menu!!.findItem(R.id.current_sensor)
        saveItem = menu.findItem(R.id.action_save)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.current_sensor -> {
                val numberPicker = MaterialNumberPicker.Builder(this)
                        .minValue(1)
                        .maxValue(128)
                        .defaultValue(currentSensor)
                        .backgroundColor(Color.WHITE)
                        .separatorColor(Color.TRANSPARENT)
                        .textColor(Color.BLACK)
                        .textSize(20F)
                        .enableFocusability(false)
                        .wrapSelectorWheel(true)
                        .build()

                AlertDialog.Builder(this)
                        .setTitle("Select a sensor")
                        .setView(numberPicker)
                        .setPositiveButton("Select") { _: DialogInterface, _: Int ->
                            currentSensor = numberPicker.value
                            sensorItem.title = currentSensor.toString()
                            pauseData.setImageResource(R.drawable.ic_pause)
                            sensorChart.data.clearValues()

                            // Sleeping for a quarter of a second 'shouldn't' hurt anything
                            Thread.sleep(250)

                            // Send the sensor number
                            val sensorFormatted = String.format(Locale.getDefault(), "%03d", currentSensor)
                            writeData("S$sensorFormatted")

                            if (isPaused) {
                                isPaused = false
                            }
                        }
                        .setNegativeButton(getString(android.R.string.cancel)) { i: DialogInterface, _: Int ->
                            i.cancel()
                        }
                        .show()

                true
            }

            R.id.action_show_table -> {
                if (!sensorChart.isEmpty) {
                    val tableFragment = TableFragment()
                    val bundle = Bundle()
                    bundle.putSerializable("data_map", dataToMap(sensorChart.data.dataSets[0]) as Serializable)
                    tableFragment.arguments = bundle
                    tableFragment.show(supportFragmentManager, tableFragment.tag)
                } else {
                    Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
                }

                true
            }

            R.id.action_save -> {
                if (!sensorChart.isEmpty) {
                    DataHandler.storeData(this, pairedLayout, currentSensor, sensorChart.data.dataSets[0])
                } else {
                    Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
                }

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
    }

    companion object {
        val READ_UUID: UUID = UUID.fromString("d973f2e1-b19e-11e2-9e96-0800200c9a66")
        val WRITE_UUID: UUID = UUID.fromString("d973f2e2-b19e-11e2-9e96-0800200c9a66")
    }
}
