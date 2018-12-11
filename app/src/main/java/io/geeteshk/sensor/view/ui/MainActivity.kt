package io.geeteshk.sensor.view.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.firebase.auth.FirebaseAuth
import com.polidea.rxandroidble2.scan.ScanSettings
import io.geeteshk.sensor.App
import io.geeteshk.sensor.R
import io.geeteshk.sensor.extension.openAppDetails
import io.geeteshk.sensor.util.DataHandler
import io.geeteshk.sensor.view.ItemOffsetDecoration
import io.geeteshk.sensor.view.adapter.DevicesAdapter
import io.geeteshk.sensor.view.adapter.model.DeviceModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var deviceAdapter: DevicesAdapter? = null

    private var scanDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (!DataHandler.checkIfUsernameSet(this)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter a folder name")
            val text = EditText(this)
            text.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(text)
            builder.setCancelable(false)
            builder.setPositiveButton("SET", null)

            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (text.text.toString().isNotBlank()) {
                    if (text.text.toString().matches(Regex("[A-Za-z0-9]+"))) {
                        DataHandler.setUsername(this, text.text.toString())
                        dialog.dismiss()
                    } else {
                        text.error = "Folder name can only contain A-Z, a-z and 0-9"
                    }
                } else {
                    text.error = "Please enter a folder name"
                }
            }
        }

        // Android makes us ask for permissions so we can use specific features
        requestPermissions()

        Toast.makeText(
                this,
                "Welcome back, ${FirebaseAuth.getInstance().currentUser!!.displayName}!",
                Toast.LENGTH_SHORT
        ).show()
    }

    // Function to setup the list view
    private fun setupRecyclerView() {
        deviceAdapter = DevicesAdapter(this)
        devicesView.layoutManager = LinearLayoutManager(this)

        devicesView.itemAnimator = DefaultItemAnimator()
        devicesView.adapter = deviceAdapter
        devicesView.addItemDecoration(ItemOffsetDecoration(this, R.dimen.item_offset))

        swipeContainer.setColorSchemeResources(R.color.colorAccent)
        swipeContainer.setOnRefreshListener {
            lookForDevices()
        }
    }

    // Permission request so we can use BLE and storage features
    private fun requestPermissions() {
        askPermission(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            DataHandler.init()
            setupRecyclerView()
        }.onDeclined { e ->
            if (e.hasForeverDenied()) {
                Snackbar.make(rootLayout, "All the permissions are required for communication to the Bluetooth module", Snackbar.LENGTH_INDEFINITE)
                        .setAction("SETTINGS") {this.openAppDetails()}
                        .show()
            } else if (e.hasDenied()) {
                Snackbar.make(rootLayout, "Please accept all the permissions for full functionality of the app", Snackbar.LENGTH_SHORT)
                        .show()
                requestPermissions()
            }
        }
    }

    // Helper function to scan for devices
    private fun lookForDevices() {
        deviceAdapter?.clear()

        if (bluetoothAdapter.isEnabled) {
            scanLeDevice()
        } else {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, RC_ENABLE_BT)
        }
    }

    // Using Rx we can make a disposable to help with scanning for devices
    private fun scanLeDevice() {
        if (scanDisposable != null) {
            deviceAdapter?.clear()
            scanDisposable!!.dispose()
            showRefresh(false)
        }

        scanDisposable = App.bleClient.scanBleDevices(
                ScanSettings.Builder()
                        .build()
        )
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { dispose() }
                .subscribe(
                        { scanResult ->
                            showRefresh(false)
                            deviceAdapter?.add(DeviceModel(scanResult!!.bleDevice, scanResult!!.rssi))
                        },
                        { throwable ->
                            Timber.e(throwable)
                        }
                )
    }

    private fun dispose() {
        scanDisposable = null
        deviceAdapter?.clear()
        showRefresh(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    scanLeDevice()
                }
            }
        }
    }

    private fun showRefresh(refreshing: Boolean) {
        swipeContainer.isRefreshing = refreshing
    }

    override fun onResume() {
        super.onResume()
        lookForDevices()
        showRefresh(false)
    }

    override fun onPause() {
        super.onPause()
        scanDisposable?.dispose()
    }

    companion object {
        private const val RC_ENABLE_BT = 0
    }
}
