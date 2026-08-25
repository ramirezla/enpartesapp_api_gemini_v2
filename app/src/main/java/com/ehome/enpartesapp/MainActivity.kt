package com.ehome.enpartesapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.ehome.enpartesapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    //private var isSiniestroExpanded = false // Estado del menú Siniestro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se recibe el valor del id del usuario y el password permitido.
        val nombreUsuario = intent.extras?.getString("username") ?: "" // Provide a default value if null
//        val passwordUsuario = intent.extras?.getString("userpassword")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        navigationView = binding.navView // Use the binding reference
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configurar el AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_consultas_abiertas,
                R.id.nav_gallery,
                R.id.exitMenuItem // Remove R.id.nav_siniestro
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Get the header view
        val headerView = navigationView.getHeaderView(0)
        // Find the TextView in the header
        val usernameTextView: TextView = headerView.findViewById(R.id.username)
        // Set the username
        usernameTextView.text = nombreUsuario

        // Get the footer view
        //val menu = navigationView.menu
        val footerView: View? = navigationView.getChildAt(navigationView.childCount - 1)
        // Find the TextView in the footer
        val appVersionTextView: TextView? = footerView?.findViewById(R.id.appVersionTextView)

        // Set the app version
        // TODO: aun no muestra bien la version, se esta colocando en el layout del footer
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            appVersionTextView?.text = getString(R.string.version, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            appVersionTextView?.text = getString(R.string.version_na)
        }

        // Se activa el NavigationItemSelectedListener para obtener la opcion seleccionada
        // Manejar la selección de ítems del menú
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_consultas_abiertas -> {
                    // Navegar al fragmento de consultas abiertas
                    navController.navigate(R.id.nav_consultas_abiertas)// verificar
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                // opcion para el Perfil de usuario
//                R.id.nav_consultar_siniestro -> {
//                    // Expandir o colapsar el submenú de Siniestro
//                    //toggleSiniestroSubmenu()
//                    return@setNavigationItemSelectedListener false // No navegar
//                }
                R.id.nav_reportar_siniestro -> {
                    // Navegar al fragmento de reportar siniestro
                    navController.navigate(R.id.nav_presupuestofragment)
                    // verificar
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_consultar_siniestro -> {
                    // Navegar al fragmento de consultar siniestro
                    navController.navigate(R.id.nav_consultafragment)
                    // verificar
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                else -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
            }
        }

// Botón de salida
        //val navigationView: NavigationView = findViewById(R.id.nav_view) // Remove this line
        // Botón de salida
        val exitMenuItem = navigationView.menu.findItem(R.id.exitMenuItem) // Use the binding reference
        exitMenuItem.setOnMenuItemClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(getString(R.string.confirmar_salir))
                .setMessage(getString(R.string.esta_seguro_que_desea_salir))
                .setPositiveButton(getString(R.string.si)) { _, _ ->
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Opcional: Finalizar la actividad actual
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            true
        }

// Controlando si presiona retroceder para salir de la aplicación
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle(getString(R.string.confirmar_salir))
                    .setMessage(getString(R.string.esta_seguro_que_desea_salir))
                    .setPositiveButton(getString(R.string.si)) { _, _ ->
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Opcional: Finalizar la actividad actual
                    }
                    .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

//    private fun toggleSiniestroSubmenu() {
//        val menu = navigationView.menu
//        val mainMenuItem = menu.findItem(R.id.nav_consultar_siniestro) // Get the "Siniestro" menu item
//
//        // Get the submenu group
//        val submenuGroup = menu.findItem(R.id.nav_consultar_siniestro).subMenu?.findItem(R.id.group_siniestros)
//
//        if (submenuGroup != null) {
//            val isVisible = submenuGroup.isVisible // Get the current visibility
//            submenuGroup.subMenu?.setGroupVisible(R.id.group_siniestros, !isVisible) // Toggle visibility
//            isSiniestroExpanded = !isVisible //Updatethe state
//            // Set the item as checked
//            mainMenuItem.isChecked = !isVisible
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}