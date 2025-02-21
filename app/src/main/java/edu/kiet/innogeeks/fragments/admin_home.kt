package edu.kiet.innogeeks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.databinding.FragmentAdminHomeBinding
import edu.kiet.innogeeks.fragments.admin.addStudentFragment
import edu.kiet.innogeeks.fragments.admin.addTeacherFragment
import edu.kiet.innogeeks.fragments.admin.removeStudentFragment
import edu.kiet.innogeeks.fragments.admin.removeTeacherFragment


class admin_home : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate to Remove Teacher Fragment
        binding.removeTeacher.setOnClickListener {
            openFragment(removeTeacherFragment())
        }

        // Navigate to Add Teachers Fragment
        binding.addTeachers.setOnClickListener {
            openFragment(addTeacherFragment())
        }

        // Navigate to Remove Students Fragment
        binding.removeStd.setOnClickListener {
            openFragment(removeStudentFragment())
        }

        // Navigate to Add Students Fragment
        binding.addStudents.setOnClickListener {
            openFragment(addStudentFragment())
        }
        binding.addResources.setOnClickListener{
            openFragment(addResourceFragment())
        }
    }

    private fun openFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame, fragment)
            .addToBackStack(null) // Enables back navigation
            .commit()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}