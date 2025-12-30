package com.example.llavektm

import android.Manifest
import android.animation.ObjectAnimator
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

class MainActivity : AppCompatActivity() {

    // ID de la solicitud de permiso
    private val requestPermissionsCode = 1001

    // Variable estado de conexion
    private var estadoConexion = 0  // 0=Desconectado, 1=Conectado, 2=Conectando

    // Receptor de Broadcast
    private val receptorMensaje = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mensaje = intent?.getStringExtra("Mensaje") ?: "Sin mensaje"
            Log.d("MainActivity", "Desde ServicioConexion: $mensaje")

            when (mensaje) {
                "Servicio iniciado correctamente" -> conexionIntentado()
                "Desconectado, Reiniciando conexion" -> conexionIntentado()
                "Conexion establecida Correctamente" -> conexionEstablecida()
                "Conexión Bluetooth finalizada" -> conexionFinaliza()
                "Respuesta Verificacion de estado = true" -> conexionEstablecida()
                "Respuesta Verificacion de estado = false" -> conexionIntentado()
                "301Y" -> animacionPrincipalPositiva()
                "302Y" -> animacionPrincipalPositiva()
            }
        }
    }

    // Enviar Mensaje por broadcast
    private fun enviarBroadcast(mensaje: String) {
        val enviarBroadcast = Intent("com.example.pruebaconexion.MensajeDeActivity").apply {
            setPackage(packageName)
            putExtra("Mensaje", mensaje)
        }

        Log.d("MainActivity", mensaje)
        sendBroadcast(enviarBroadcast)
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
        solicitarPermisos()

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

        btnConectar.setOnClickListener{
            if (estadoConexion == 0){
                if (!verificarPermisosBluetooth()) {
                    Toast.makeText(this, "Permiso Bluetooth no otorgado", Toast.LENGTH_SHORT).show()
                    solicitarPermisos()
                    return@setOnClickListener
                }
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
            enviarBroadcast("Enviar 301")
        }

        btnAlarma.setOnClickListener {
            animacionPrincipalIndeterminada()
            //Handler(Looper.getMainLooper()).postDelayed({
            //    animacionPrincipalNegativa()
            //}, 4000)
            enviarBroadcast("Enviar 302")
        }
    }
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.example.pruebaconexion.MensajeDeServicio")
        ContextCompat.registerReceiver(
            this, receptorMensaje, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    override fun onResume() {
        super.onResume()
        // Enviar el broadcast para verificar el estado
        enviarBroadcast("Verificacion de estado")
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(receptorMensaje)
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

    private fun verificarPermisosBluetooth(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisos() {
        val permisos = mutableListOf<String>()

        permisos += Manifest.permission.BLUETOOTH_CONNECT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos += Manifest.permission.POST_NOTIFICATIONS
        }

        val faltantes = permisos.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltantes.isNotEmpty()) {
            requestPermissions(faltantes.toTypedArray(), requestPermissionsCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestPermissionsCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos otorgados", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "La app necesita permisos para funcionar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}