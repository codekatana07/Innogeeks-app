package edu.kiet.innogeeks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.databinding.FragmentCoordinatorHomeBinding
import edu.kiet.innogeeks.markAttendanceFragment

class coordinatorHomeFragment : Fragment() {

        private var _binding: FragmentCoordinatorHomeBinding? = null
        private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCoordinatorHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.markAttendanceCard.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame, markAttendanceFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }

}