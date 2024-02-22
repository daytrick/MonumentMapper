package com.example.monumentmapper.ui.account

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.monumentmapper.MainActivity
import com.example.monumentmapper.databinding.FragmentAccountBinding
import com.example.monumentmapper.ui.login.LoginActivity

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val accountViewModel =
            ViewModelProvider(this).get(AccountViewModel::class.java)

        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Call login activity
        // How to call a diff Activity from a Fragment from: https://stackoverflow.com/a/53361903
        val intent = Intent(getActivity(), LoginActivity::class.java)
        getActivity()?.startActivity(intent)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}