package com.example.llavektm

import android.util.Log

class ProcesadorDatos {

    var clave: IntArray = IntArray(4)

    val arrayA: IntArray = intArrayOf(6, 2, 5, 1, 4, 9, 8, 7, 3) // Desplazar X+Num de la izquierda a la derecha
    //val arrayB: IntArray = intArrayOf(2, 9, 8, 4, 6, 1, 5, 3, 7)
    //val arrayC: IntArray = intArrayOf(5, 3, 6, 7, 2, 9, 4, 8, 1)

    var desplazamiento: Int = 0
    var claveDinamicaDescifrada: IntArray = IntArray(5)


    fun procesarClaveInical(mensaje: String): String {

        //Validar la longitud, debe tener 15 caracteres para ser valido
        if (mensaje.length != 15) {
            return "Tamaño de calve inicial no valido: ${mensaje.length}"
        }
        if (!mensaje.all { it.isDigit() }) {
            return "La clave inicial contiene caracteres no numéricos"
        }

        //Convertir a un array que almancena cada numero
        val digitos = IntArray(mensaje.length) { i ->
            mensaje[i].digitToInt()
        }

        //Procesar array para obtener la clave inicial
        clave[0] = (digitos[0] * 10) + digitos[1]

        if (clave[0] <= 13){
            clave[1] = digitos[clave[0] - 1]
            clave[2] = digitos[clave[0]]
            clave[3] = digitos[clave[0] + 1]
            clave[0] = ((clave[1] * 100) + (clave[2] * 10) + (clave[3]))
        } else {
            clave[0] = digitos[1]
            clave[1] = digitos[clave[0] - 1]
            clave[2] = digitos[clave[0]]
            clave[3] = digitos[clave[0] + 1]
            clave[0] = ((clave[1] * 100) + (clave[2] * 10) + (clave[3]))
        }

        Log.d("Clave Inicial", clave[0].toString())

        //Si la conversion es correcta, retorna 200 para iniciar el intercambio de claves dinamicas
        return "200"
    }

    fun procesarClaveDinamica(mensaje: String): String {

        //Validar la longitud, debe tener 5 caracteres para ser valido
        if (mensaje.length != 5) {
            return "Tamaño de clave dinamica no valido ${mensaje.length}"
        }
        if (!mensaje.all { it.isDigit() }) {
            return "La clave dinamica contiene caracteres no numéricos"
        }

        //Convertir a un array que almacena cada numero
        val claveDinamica = IntArray(mensaje.length) { i ->
            mensaje[i].digitToInt()
        }

        for (i in 0..4) {
            for (j in 0..8) {
                if (claveDinamica[i] == arrayA[j]) {
                    if (i == 0) {
                        desplazamiento = j + clave[1]
                    } else {
                        desplazamiento = j + claveDinamica[i - 1] + clave[1]
                    }
                    if (desplazamiento > 8) {
                        desplazamiento -= 9
                    }
                    if (desplazamiento > 8) {
                        desplazamiento -= 9
                    }
                    claveDinamicaDescifrada[i] = arrayA[desplazamiento]

                    break
                }
            }
        }

        for (i in 1 until 5) {
            claveDinamicaDescifrada[0] = claveDinamicaDescifrada[0] * 10 + claveDinamicaDescifrada[i]
        }
        return claveDinamicaDescifrada[0].toString()
    }
}