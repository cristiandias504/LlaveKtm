package com.example.llavektm

import android.Manifest
import android.animation.ObjectAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // ID de la solicitud de permiso
    private val REQUEST_PERMISSIONS_CODE = 1

    // Variable estado de conexion
    private var estadoConexion = 0  // 0=Desconectado, 1=Conectado, 2=Conectando

    // Permisos necesarios para el uso del Bluetooth   ***ALGUNOS AUN NO SE USAN*** Nesesarios para scanear
    private val requiredPermissions = arrayOf(
        //Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        //Manifest.permission.ACCESS_FINE_LOCATION,
        //Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val deviceName = "ESP32"
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar SPP
    private var btSocket: BluetoothSocket? = null
    private lateinit var btDevice: BluetoothDevice
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Receptor de Broadcast
    private val receptorMensaje = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mensaje = intent?.getStringExtra("Mensaje") ?: "Sin mensaje"
            Log.d("MainActivity", "Mensaje recibido: $mensaje")
            if (mensaje == "Servicio iniciado correctamente") {
                conexionIntentado()
            } else if (mensaje == "Conexion establecida Correctamente") {
                conexionEstablecida()
            } else if (mensaje == "Conexión Bluetooth finalizada") {
                conexionFinaliza()
            } else if (mensaje == "Respuesta Verificacion de estado = true") {
                conexionEstablecida()
                Log.d("MainActivity", "Verificacion Completa: Servicio Activo, Conexion Estable")
            } else if (mensaje == "Respuesta Verificacion de estado = false") {
                conexionIntentado()
                Log.d("MainActivity", "Verificacion Completa: Servicio Activo, Sin Conexion")
            } else if (mensaje == "301Y") {
                animacionPrincipalPositiva()
            } else {
                //CuadroMensaje.text = mensaje.toString()
            }
        }
    }

    private lateinit var btnConectar: TextView
    private lateinit var btnProximidad: ToggleButton
    private lateinit var barraProgresoConexion: ProgressBar
    private lateinit var barraProgresoPrincipal: ProgressBar
    private lateinit var logoBluetooth:ImageView
    private lateinit var imagenKTM: ImageView
    private lateinit var layoutBonotes: ConstraintLayout
    private lateinit var btnApagar: Button
    private lateinit var btnAlarma: Button

    private val duracionAnimacion: Long = 300L
    private var conexionProximidad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        solicitarPermiso()


        // Verificar permisos
        if (!verificarPermisos()) {
            solicitarPermiso()
        }


        btnConectar = findViewById(R.id.btnConectar)
        btnProximidad = findViewById(R.id.btnProximidad)
        barraProgresoConexion = findViewById(R.id.barraProgresoConexion)
        barraProgresoPrincipal = findViewById(R.id.barraProgresoPrincipal)
        logoBluetooth = findViewById(R.id.logoBluetooth)
        imagenKTM = findViewById(R.id.imagenKTM)
        layoutBonotes = findViewById(R.id.layoutBotones)
        btnApagar = findViewById(R.id.btnApagar)
        btnAlarma = findViewById(R.id.btnAlarma)

        animacionImagenKTM()

        // Crear y enviar el broadcast
        val intentBroadcast = Intent("com.example.pruebaconexion.MensajeDeActivity").apply {
            setPackage(packageName)
            putExtra("Mensaje", "Verificacion de estado")
        }

        Log.d("MainActivity", "Verificacion de estado")
        sendBroadcast(intentBroadcast)

        btnConectar.setOnClickListener{
            if (estadoConexion == 0){

                //if (!hasPermissions()) {
                //    Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                //    solicitarPermiso()
                //    return@setOnClickListener
                //}

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Sin permiso BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Sin permiso BLUETOOTH_CONNECT")
                    return@setOnClickListener
                }

                //val pairedDevices: Set<BluetoothDevice>? = btAdapter?.bondedDevices
                //btDevice = pairedDevices?.find { it.name == deviceName }
                //    ?: run {
                //        Toast.makeText(this, "ESP32 no emparejado", Toast.LENGTH_SHORT).show()
                //        Log.e("MainActivity", "ESP32 no emparejado")
                //        return@setOnClickListener
                //    }

                Log.d("MainActivity", "Lanzando Servicio")
                val intent = Intent(this, ServicioConexion::class.java)
                startService(intent)

            } else {
                Log.d("MainActivity", "Deteniendo Servicio")
                val intent = Intent(this, ServicioConexion::class.java)
                stopService(intent)
            }
        }



        btnProximidad.setOnClickListener {
            if (conexionProximidad){
                animacionPrincipalIndeterminada()
                Handler(Looper.getMainLooper()).postDelayed({
                    animacionPrincipalPositiva()
                    btnProximidad.setTextColor(ContextCompat.getColor(this, R.color.white))
                    btnProximidad.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris))
                    conexionProximidad = false
                }, 4000)
            } else {
                animacionPrincipalIndeterminada()
                Handler(Looper.getMainLooper()).postDelayed({
                    animacionPrincipalPositiva()
                    btnProximidad.setTextColor(ContextCompat.getColor(this, R.color.black))
                    btnProximidad.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.naranja))
                    conexionProximidad = true
                }, 4000)
            }
        }

        imagenKTM.setOnClickListener{
            if (layoutBonotes.isVisible){
                layoutBonotes.visibility = View.INVISIBLE
            } else {
                layoutBonotes.visibility = View.VISIBLE
            }
        }

        btnApagar.setOnClickListener {
            animacionPrincipalIndeterminada()
            //Handler(Looper.getMainLooper()).postDelayed({
            //    animacionPrincipalPositiva()
            //}, 4000)
            val intentBroadcast = Intent("com.example.pruebaconexion.MensajeDeActivity").apply {
                setPackage(packageName)
                putExtra("Mensaje", "Enviar 301")
            }

            Log.d("MainActivity", "Enviar 301")
            sendBroadcast(intentBroadcast)
        }

        btnAlarma.setOnClickListener {
            //animacionPrincipalIndeterminada()
            //Handler(Looper.getMainLooper()).postDelayed({
            //    animacionPrincipalNegativa()
            //}, 4000)
            val intentBroadcast = Intent("com.example.pruebaconexion.MensajeDeActivity").apply {
                setPackage(packageName)
                putExtra("Mensaje", "Enviar 302")
            }

            Log.d("MainActivity", "Enviar 302")
            sendBroadcast(intentBroadcast)
        }
    }

    private fun conexionIntentado() {
        estadoConexion = 2
        btnConectar.text = getText(R.string.Conectando)
        btnConectar.setTextColor(ContextCompat.getColor(this, R.color.naranja))
        barraProgresoConexion.visibility = View.VISIBLE
        logoBluetooth.setImageResource(R.drawable.bluetoothoff)
        barraProgresoConexion.isIndeterminate = true
        barraProgresoConexion.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.naranja))
    }

    private fun conexionEstablecida() {
        estadoConexion = 1
        btnConectar.text = getText(R.string.Conectado)
        btnConectar.setTextColor(ContextCompat.getColor(this, R.color.verde))
        barraProgresoConexion.isIndeterminate = false

        ObjectAnimator.ofInt(barraProgresoConexion, "progress", 0, 100).apply {
            duration = 500 // 5 segundos
            interpolator = LinearInterpolator()
            start()
        }

        animacionPrincipalPositiva()
        barraProgresoConexion.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde))
        logoBluetooth.setImageResource(R.drawable.bluetoothon)
    }

    private fun conexionFinaliza() {
        estadoConexion = 0
        btnConectar.text = getText(R.string.Conectar)
        btnConectar.setTextColor(ContextCompat.getColor(this, R.color.black))
        barraProgresoConexion.visibility = View.INVISIBLE
        barraProgresoConexion.isIndeterminate = true
        logoBluetooth.setImageResource(R.drawable.bluetoothoff)
        animacionPrincipalNegativa()
    }

    private fun animacionPrincipalIndeterminada() {
        barraProgresoPrincipal.scaleX = 1f
        barraProgresoPrincipal.isIndeterminate = true
        barraProgresoPrincipal.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.naranja))
        barraProgresoPrincipal.visibility = View.VISIBLE
    }

    private fun animacionPrincipalPositiva() {
        barraProgresoPrincipal.scaleX = 1f
        barraProgresoPrincipal.isIndeterminate = false
        barraProgresoPrincipal.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde))
        barraProgresoPrincipal.visibility = View.VISIBLE

        ObjectAnimator.ofInt(barraProgresoPrincipal, "progress", 0, 100).apply {
            duration = duracionAnimacion
            interpolator = LinearInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            barraProgresoPrincipal.scaleX = -1f

            ObjectAnimator.ofInt(barraProgresoPrincipal, "progress", 100, 0).apply {
                duration = duracionAnimacion
                interpolator = LinearInterpolator()
                start()
            }
        }, 500)
    }

    private fun animacionPrincipalNegativa() {
        barraProgresoPrincipal.scaleX = 1f
        barraProgresoPrincipal.isIndeterminate = false
        barraProgresoPrincipal.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.rojo))
        barraProgresoPrincipal.visibility = View.VISIBLE

        ObjectAnimator.ofInt(barraProgresoPrincipal, "progress", 0, 100).apply {
            duration = duracionAnimacion
            interpolator = LinearInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            barraProgresoPrincipal.scaleX = -1f

            ObjectAnimator.ofInt(barraProgresoPrincipal, "progress", 100, 0).apply {
                duration = duracionAnimacion
                interpolator = LinearInterpolator()
                start()
            }
        }, 500)
    }

    private fun animacionImagenKTM() {
        val animX = ObjectAnimator.ofFloat(imagenKTM, "scaleX", 0.1f, 1.25f)
        animX.duration = duracionAnimacion
        animX.interpolator = LinearInterpolator()

        val animY = ObjectAnimator.ofFloat(imagenKTM, "scaleY", 0.1f, 1.25f)
        animY.duration = duracionAnimacion
        animY.interpolator = LinearInterpolator()

        animX.start()
        animY.start()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.example.pruebaconexion.MensajeDeServicio")

        ContextCompat.registerReceiver(
            this,
            receptorMensaje,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receptorMensaje)
    }

    private fun verificarPermisos(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    private val PERMISOS_BLE_31 = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    private val PERMISOS_BLE_LEGACY = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val PERMISO_NOTIFICACIONES = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
    )

    private fun solicitarPermiso() {

        val permisosApedir = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisosApedir += PERMISOS_BLE_31
        } else {
            permisosApedir += PERMISOS_BLE_LEGACY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisosApedir += PERMISO_NOTIFICACIONES
        }

        val permisosFaltantes = permisosApedir.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (permisosFaltantes.isNotEmpty()) {
            requestPermissions(permisosFaltantes.toTypedArray(), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos otorgados", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "La app necesita permisos para funcionar", Toast.LENGTH_LONG).show()
            }
        }
    }
}