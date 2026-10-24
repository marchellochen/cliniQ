package com.example.cliniq

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.cliniq.databinding.FragmentPlaceholderBinding

/**
 * Temporary page for a tab that hasn't been moved yet.
 * If it has an old screen, a button opens it so nothing is unreachable.
 * Delete this file once the last tab has its real fragment.
 */
class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        binding.tvPlaceholder.text = args.getString(ARG_TITLE)

        val target = args.getString(ARG_TARGET)
        binding.btnOpenOld.isVisible = target != null
        binding.btnOpenOld.setOnClickListener {
            val cls = Class.forName(target ?: return@setOnClickListener)
            startActivity(Intent(requireContext(), cls))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_TARGET = "target"

        fun newInstance(title: String, target: Class<out Activity>? = null) =
            PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_TARGET, target?.name)
                }
            }
    }
}
