package com.ehome.enpartesapp

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.ehome.enpartesapp.databinding.FragmentDerechosBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class DerechosFragment : Fragment() {

    private var _binding: FragmentDerechosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Aceptar la licencia, los derechos y terminos.
    private lateinit var checkBoxAccept: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDerechosBinding.inflate(inflater, container, false)

        checkBoxAccept = binding.checkBoxAccept

        // Format the license textval licenseTextView: TextView = binding.textViewLicense
        val formattedLicenseText = formatLicenseText(getString(R.string.licencia_derechos))

        val textLicencia: TextView = binding.textViewLicense
        textLicencia.text = formattedLicenseText

        // Handle back button press
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (checkBoxAccept.isChecked) {
                    // Navigate back to the previous fragment
                    findNavController().popBackStack()
                } else {
                    // Optionally, show a message to the user
                    // that they must accept the license
                    android.widget.Toast.makeText(requireContext(), "Debe aceptar los terminos y condiciones", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatLicenseText(licenseText: String): CharSequence {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(licenseText, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(licenseText)
        }
    }

}