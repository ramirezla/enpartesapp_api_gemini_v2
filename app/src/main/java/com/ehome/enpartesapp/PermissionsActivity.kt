package com.ehome.enpartesapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import android.util.Log

class PermissionsActivity : AppCompatActivity() {

    private lateinit var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>

    private val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        requestMultiplePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            Log.d("PermissionsActivity", "Permissions callback received: $permissionsMap")
            val allPermissionsGranted = permissionsMap.all { it.value }
            if (allPermissionsGranted) {
                Log.d("PermissionsActivity", "All permissions granted.")
                // All permissions granted, proceed to the login activity
                navigateToLoginActivity()
            } else {
                Log.d("PermissionsActivity", "Some permissions denied.")
                // Some permissions denied, show a message or handle it accordingly
                showPermissionDeniedDialog()
            }
        }

        // Check if permissions are already granted
        if (checkPermissions()) {
            Log.d("PermissionsActivity", "Permissions already granted on onCreate.")
            // Permissions are already granted, proceed to the login activity
            navigateToLoginActivity()
        } else {
            Log.d("PermissionsActivity", "Permissions not granted on onCreate, requesting.")
            // Permissions are not granted, launch the permission request
            requestMultiplePermissionsLauncher.launch(permissions)
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d("PermissionsActivity", "Checking permissions...")
        // for (permission in permissions) {
        //     if (ContextCompat.checkSelfPermission(
        //             this,
        //             permission
        //         ) != PackageManager.PERMISSION_GRANTED
        //     ) {
        //         Log.d("PermissionsActivity", "Permission $permission not granted.")
        //         return false // At least one permission is not granted
        //     }
        // }
        Log.d("PermissionsActivity", "All permissions granted.")
        return true // All permissions are granted
    }

    private fun navigateToLoginActivity() {
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