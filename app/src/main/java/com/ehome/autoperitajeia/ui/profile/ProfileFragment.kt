package com.ehome.autoperitajeia.ui.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ehome.autoperitajeia.LoginActivity
import com.ehome.autoperitajeia.MainActivity
import com.ehome.autoperitajeia.R
import com.ehome.autoperitajeia.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener la cuenta de usuario pasada a MainActivity
        val currentUser = (activity as? MainActivity)?.currentUserAccount
            .takeIf { !it.isNullOrBlank() }
            ?: requireActivity().intent.extras?.getString("username")
            .takeIf { !it.isNullOrBlank() }
            ?: "Usuario"

        binding.tvProfileName.text = currentUser
        binding.tvDetailAccount.text = currentUser

        // Obtener la versión de la aplicación
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvDetailVersion.text = packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            binding.tvDetailVersion.text = getString(R.string.version_na)
        }

        // Configurar botón de cerrar sesión
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirmar_salir))
                .setMessage(getString(R.string.esta_seguro_que_desea_salir))
                .setPositiveButton(getString(R.string.si)) { _, _ ->
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
