package com.cerist.summer.blelightswitcher

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import java.util.*

class DeviceProfile{

    companion object {
        var SERVICE_UUID = UUID.fromString("000F00D-1212-EFDE-1523-785FEABCD123")

        //Read-Wrote only characteristic providing the state of the lamp
        var CHARACTERISTIC_STATE_UUID = UUID.fromString("000BEEF-1212-EFDE-1523-785FEABCD123")


        fun getStateDescription(state: Int): String {
            return when (state) {
                BluetoothProfile.STATE_CONNECTED -> "Connected"
                BluetoothProfile.STATE_CONNECTING ->  "Connecting"
                BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
                BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
                else -> "Unknown State $state"
            }
        }

        fun getStatusDescription(status: Int): String {
            return when (status) {
                BluetoothGatt.GATT_SUCCESS ->  "SUCCESS"
                else ->  "Unknown Status $status"
            }
        }

    }
}