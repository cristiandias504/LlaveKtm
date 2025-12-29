package com.example.llavektm

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.*

class ServicioConexion : Service() {

    companion object {
        private const val TAG = "BLE"
        private const val PREFS = "BLE_PREFS"
        private const val KEY_MAC = "ESP32_MAC"
    }

    // ===== CONFIG BLE =====
    private val DEVICE_NAME = "ESP32_BLE_TEST"

    private val SERVICE_UUID =
        UUID.fromString("12345678-1234-1234-1234-1234567890ab")

    private val RX_UUID =
        UUID.fromString("12345678-1234-1234-1234-1234567890ac")

    private val TX_UUID =
        UUID.fromString("12345678-1234-1234-1234-1234567890ad")

    private val CCCD_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private val bleScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private var conexionEstablecida = false
    private var servicioFinalizado = false

    private var dispositivoGuardado: BluetoothDevice? = null

    private val procesadorDatos = ProcesadorDatos()

    // ===== BROADCAST RECEIVER =====
    private val receptorMensaje = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mensaje = intent?.getStringExtra("Mensaje") ?: return
            Log.d(TAG, "📨 Desde Activity: $mensaje")

            when (mensaje) {
                "Enviar 301" -> enviarMensaje("301")
                "Enviar 302" -> enviarMensaje("302")
            }
        }
    }

    // ===== CICLO DE VIDA =====
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Servicio creado")

        crearCanal()
        iniciarForeground()

        cargarDispositivoGuardado()

        if (dispositivoGuardado == null) {
            Log.d(TAG, "🔍 No hay dispositivo guardado, iniciando SCAN")
            iniciarScanBLE()
        } else {
            Log.d(TAG, "🔗 Conectando directo al dispositivo guardado")
            conectarGatt(dispositivoGuardado!!, autoConnect = true)
        }

        val filter = IntentFilter("com.example.pruebaconexion.MensajeDeActivity")
        ContextCompat.registerReceiver(
            this,
            receptorMensaje,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        //iniciarScanBLE()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "▶️ onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 Servicio destruido")
        servicioFinalizado = true
        bluetoothGatt?.let {
            refreshGattCache(it)
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
        unregisterReceiver(receptorMensaje)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ===== NOTIFICACIÓN =====
    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "canal_ble",
                "Servicio BLE",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    private fun iniciarForeground() {
        val notification = NotificationCompat.Builder(this, "canal_ble")
            .setContentTitle("BLE activo")
            .setContentText("Esperando ESP32")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()

        startForeground(1, notification)
    }

    // ===== SCAN BLE (SOLO PRIMERA VEZ) =====
    private fun iniciarScanBLE() {
        Log.d(TAG, "🔍 iniciarScanBLE()")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ Sin permiso BLUETOOTH_SCAN")
            return
        }

        val filtro = ScanFilter.Builder()
            .setDeviceName(DEVICE_NAME)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(listOf(filtro), settings, scanCallback)
        Log.d(TAG, "📡 Escaneando ESP32 BLE...")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            Log.d(TAG, "✅ Encontrado: ${result.device.name} | ${result.device.address}")
            bleScanner?.stopScan(this)

            guardarDispositivo(result.device)
            conectarGatt(result.device, autoConnect = false)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "❌ Scan fallido: $errorCode")
        }
    }

    // ===== CONEXIÓN GATT =====
    private fun conectarGatt(device: BluetoothDevice, autoConnect: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        bluetoothGatt = device.connectGatt(this, autoConnect, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            Log.d(TAG, "🔄 ConnectionState status=$status newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "❌ Error GATT: $status")
                gatt.close()
                reiniciarConexion()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "🔐 Conectado, descubriendo servicios")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "⚠️ Desconectado")
                reiniciarConexion()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID) ?: return

            rxCharacteristic = service.getCharacteristic(RX_UUID)
            txCharacteristic = service.getCharacteristic(TX_UUID)

            activarNotificaciones(gatt)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val mensajeRecibido = characteristic.value.toString(Charsets.UTF_8)
            Log.d(TAG, "📥 RX: $mensajeRecibido")
            if (mensajeRecibido.length == 15) {
                val respuesta = procesadorDatos.procesarClaveInical(mensajeRecibido)
                if (respuesta == "200"){
                    enviarMensaje("200")
                }
            } else if (mensajeRecibido.length == 5) {
                val respuesta = procesadorDatos.procesarClaveDinamica(mensajeRecibido)
                enviarMensaje(respuesta)
                Log.d(TAG, "Clave Descifrada: $respuesta")
            }

        }







    }

    // ===== NOTIFICACIONES =====
    private fun activarNotificaciones(gatt: BluetoothGatt) {
        Log.d(TAG, "🔔 Activando notificaciones")

        gatt.setCharacteristicNotification(txCharacteristic, true)

        val descriptor = txCharacteristic?.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.e(TAG, "❌ CCCD no encontrado")
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val ok = gatt.writeDescriptor(descriptor)

        Log.d(TAG, "✍️ writeDescriptor enviado: $ok")
        conexionEstablecida = true
    }

    // ===== ENVÍO =====
    private fun enviarMensaje(mensaje: String) {
        if (!conexionEstablecida) {
            Log.w(TAG, "⚠️ No conectado, no se envía")
            return
        }

        rxCharacteristic?.value = (mensaje).toByteArray() //+ "\n").toByteArray()
        bluetoothGatt?.writeCharacteristic(rxCharacteristic)
        Log.d(TAG, "📤 TX: $mensaje")
    }

    private fun refreshGattCache(gatt: BluetoothGatt): Boolean {
        return try {
            val refresh = gatt.javaClass.getMethod("refresh")
            refresh.invoke(gatt) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "❌ No se pudo refrescar GATT", e)
            false
        }
    }

    // ===== RECONEXIÓN =====
    private fun reiniciarConexion() {
        Log.w(TAG, "🔄 Reiniciando conexión")
        conexionEstablecida = false

        bluetoothGatt?.let {
            refreshGattCache(it)
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null

        Handler(Looper.getMainLooper()).postDelayed({
            if (!servicioFinalizado && dispositivoGuardado != null) {
                Log.d(TAG, "🔁 Esperando reconexión automática")
                conectarGatt(dispositivoGuardado!!, autoConnect = true)
            }
        }, 3000)
    }

    private fun guardarDispositivo(device: BluetoothDevice) {
        dispositivoGuardado = device
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_MAC, device.address)
            .apply()
    }

    private fun cargarDispositivoGuardado() {
        val mac = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(KEY_MAC, null)

        mac?.let {
            dispositivoGuardado = bluetoothAdapter?.getRemoteDevice(it)
        }
    }
}