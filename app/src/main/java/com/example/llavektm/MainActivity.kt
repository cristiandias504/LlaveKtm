package com.example.llavektm

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {

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

        var estadoConexion = false

        btnConectar.setOnClickListener{
            if (!estadoConexion){
                btnConectar.text = getText(R.string.Conectando)
                btnConectar.setTextColor(ContextCompat.getColor(this, R.color.naranja))
                barraProgresoConexion.visibility = View.VISIBLE
                estadoConexion = true
                Handler(Looper.getMainLooper()).postDelayed({
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
                }, 3000)
            } else {
                btnConectar.text = getText(R.string.Conectar)
                btnConectar.setTextColor(ContextCompat.getColor(this, R.color.black))
                barraProgresoConexion.visibility = View.INVISIBLE
                estadoConexion = false
                barraProgresoConexion.isIndeterminate = true
                logoBluetooth.setImageResource(R.drawable.bluetoothoff)
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
            Handler(Looper.getMainLooper()).postDelayed({
                animacionPrincipalPositiva()
            }, 4000)
        }

        btnAlarma.setOnClickListener {
            animacionPrincipalIndeterminada()
            Handler(Looper.getMainLooper()).postDelayed({
                animacionPrincipalNegativa()
            }, 4000)
        }
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
}