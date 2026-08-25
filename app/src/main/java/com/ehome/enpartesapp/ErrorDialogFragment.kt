package com.ehome.enpartesapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.app.AlertDialog
import android.app.Dialog
import androidx.fragment.app.DialogFragment

class ErrorDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_CODE = "code"
        private const val ARG_MESSAGE = "message"

        fun newInstance(code: String, message: String): ErrorDialogFragment {
            val fragment = ErrorDialogFragment()
            val args = Bundle().apply {
                putString(ARG_CODE, code)
                putString(ARG_MESSAGE, message)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val code = arguments?.getString(ARG_CODE) ?: "Unknown Code"
        val message = arguments?.getString(ARG_MESSAGE) ?: "Unknown Error"

        return AlertDialog.Builder(requireContext())
            .setTitle("Error: $code")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}