package com.ehome.autoperitajeia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionsActivity : AppCompatActivity() {

    private lateinit var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var isGpsDialogShowing = false
    private var isNavigating = false

    private val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            // Android 11 y 12 (API 30 y 31 - TECNO PDVA Neo)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        requestMultiplePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissionsMap ->
            Log.d("PermissionsActivity", "Permissions callback received: $permissionsMap")
            val allPermissionsGranted = permissionsMap.all { (_, isGranted) -> isGranted }
            if (allPermissionsGranted) {
                Log.d("PermissionsActivity", "All permissions granted.")
                verificarGpsYNavegar()
            } else {
                Log.d("PermissionsActivity", "Some permissions denied.")
                showPermissionDeniedDialog()
            }
        }

        // Check if permissions are already granted
        if (checkPermissions()) {
            Log.d("PermissionsActivity", "Permissions already granted on onCreate.")
            verificarGpsYNavegar()
        } else {
            Log.d("PermissionsActivity", "Permissions not granted on onCreate, requesting.")
            requestMultiplePermissionsLauncher.launch(permissions)
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            verificarGpsYNavegar()
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d("PermissionsActivity", "Checking permissions...")
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("PermissionsActivity", "Permission $permission not granted.")
                return false
            }
        }
        Log.d("PermissionsActivity", "All permissions granted.")
        return true
    }

    private fun verificarGpsYNavegar() {
        val locationManager = getSystemService(LocationManager::class.java) ?: return
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            Log.w("PermissionsActivity", "El GPS está desactivado. Mostrando diálogo obligatorio.")
            mostrarDialogoGpsObligatorio()
        } else {
            Log.d("PermissionsActivity", "GPS activo. Navegando a LoginActivity.")
            navigateToLoginActivity()
        }
    }

    private fun mostrarDialogoGpsObligatorio() {
        if (isGpsDialogShowing || isNavigating) return
        isGpsDialogShowing = true

        AlertDialog.Builder(this)
            .setTitle("GPS Desactivado")
            .setMessage("Para usar AutoPeritajeIA y realizar la valoración del peritaje, es obligatorio que active la ubicación por GPS. ¿Desea activarlo ahora?")
            .setPositiveButton("Activar GPS") { dialog, _ ->
                isGpsDialogShowing = false
                dialog.dismiss()
                try {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("PermissionsActivity", "Error al abrir ajustes de ubicación: ${e.message}", e)
                }
            }
            .setNegativeButton("Salir de la aplicación") { dialog, _ ->
                isGpsDialogShowing = false
                dialog.dismiss()
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToLoginActivity() {
        if (isNavigating) return
        isNavigating = true
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permisos_denegados))
            .setMessage(getString(R.string.para_usar_la_aplicacion_debes_conceder_todos_los_permisos))
            .setPositiveButton(getString(R.string.retry_button)) { _, _ ->
                // Re-launch the permission request.
                requestMultiplePermissionsLauncher.launch(permissions)
            }
            .setNegativeButton(getString(R.string.exit_button)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}