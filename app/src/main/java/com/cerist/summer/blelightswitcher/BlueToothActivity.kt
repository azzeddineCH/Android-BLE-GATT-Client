package com.cerist.summer.blelightswitcher

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_blue_tooth.*
import java.nio.ByteBuffer
import java.util.*


class BlueToothActivity : AppCompatActivity() {
    companion object {
        private val TAG = "BLE"
        val BLUETOOTH_REQUEST_CODE = 1
    }

    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private lateinit var mBlueToothLeScanner:BluetoothLeScanner
    var mBluetoothGatt:BluetoothGatt ?= null
    val mHandler:Handler = Handler()
    lateinit var  mLampSwitcher: Switch




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blue_tooth)
        mLampSwitcher = lamp_state_switcher


    }

    override fun onResume() {
        super.onResume()
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE)
        }else{
            startBleScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == BLUETOOTH_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                startBleScan()
            }else{
              Toast.makeText(this,"This application requires bluetooth",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBleScan(){

        val scanFilter  = ScanFilter.Builder()
                .build()

        val scanFilters:MutableList<ScanFilter> = mutableListOf()
        scanFilters.add(scanFilter)

        val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        Log.d(TAG,"starting BLE scan")
        mBluetoothAdapter.bluetoothLeScanner.startScan(
                scanFilters,
                scanSettings,
                mBleScanCallbacks
        )
        lamp_state_switcher.setOnCheckedChangeListener({ compoundButton: CompoundButton, state: Boolean ->
            Log.d(TAG, "changing the lamp state")
            setLampState(state)
        })

    }
    private fun stopBleScan(){
        mBluetoothAdapter.bluetoothLeScanner.stopScan(mBleScanCallbacks)
    }

    fun processScanResult(scanResult:ScanResult){
        val bluetoothDevice = scanResult.device
        Log.d(TAG, "device name ${bluetoothDevice.name} with address ${bluetoothDevice.address}")
        stopBleScan()
        mBluetoothGatt = bluetoothDevice.connectGatt(BlueToothActivity@this,false,mBleGattCallBack)



    }


    val mBleScanCallbacks: ScanCallback by lazy {
        object : ScanCallback(){
            override fun onScanFailed(errorCode: Int) {
                Log.d(TAG,"on Scan Failed")
            }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                Log.d(TAG,"on Scan result")
                processScanResult(result!!)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                Log.d("SCAN","on batch scan results")
                results?.forEach {
                    processScanResult(it)
                }
            }
        }
    }

    val mBleGattCallBack: BluetoothGattCallback by lazy {
        object : BluetoothGattCallback(){

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                Log.d(TAG,"on Connection state change:" +
                        "      STATE:${DeviceProfile.getStateDescription(newState)}"+
                                        "STATUS=${DeviceProfile.getStateDescription(status)}"    )

                if(newState == BluetoothProfile.STATE_CONNECTED){
                    mBluetoothGatt?.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                Log.d(TAG,"on Services discovered")

                val characteristic = gatt?.getService(DeviceProfile.SERVICE_UUID)
                        ?.getCharacteristic(DeviceProfile.CHARACTERISTIC_STATE_UUID)

                gatt?.readCharacteristic(characteristic)




            }

            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicRead(gatt, characteristic, status)
                Log.d("TAG","reading into the characteristic ${characteristic?.uuid} the value ${characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0)}")

                if(DeviceProfile.CHARACTERISTIC_STATE_UUID == characteristic?.uuid){
                    val value = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0)
                   mHandler.post {
                       mLampSwitcher.isChecked = value==1
                   }
                    gatt?.setCharacteristicNotification(characteristic,true)
                }

            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicChanged(gatt, characteristic)
                Log.d("TAG","reading into the characteristic ${characteristic?.uuid} the value ${characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0)}")
                val value = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0)
                mHandler.post {
                    mLampSwitcher.isChecked = value==1
                }
            }
        }
    }

    private fun setLampState(state:Boolean){
        val i:Byte = if (state) 1 else 0
        val newCharacteristicValue  = ByteBuffer.allocate(1)
                .put(i)
                .array()

        val characteristic = mBluetoothGatt?.getService(DeviceProfile.SERVICE_UUID)
                                             ?.getCharacteristic(DeviceProfile.CHARACTERISTIC_STATE_UUID)

        characteristic?.value = newCharacteristicValue
        mBluetoothGatt?.writeCharacteristic(characteristic)

    }
}
