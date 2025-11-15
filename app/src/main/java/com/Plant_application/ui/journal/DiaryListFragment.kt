package com.Plant_application.ui.journal

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.Plant_application.R
import com.Plant_application.databinding.ItemJournalDetailBinding
import com.bumptech.glide.Glide
import java.io.File

class DiaryListFragment : Fragment(R.layout.item_journal_detail) {

    private var _binding: ItemJournalDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryListViewModel by viewModels()
    private val args: DiaryListFragmentArgs by navArgs()
    private lateinit var diaryAdapter: DiaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ItemJournalDetailBinding.bind(view)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadEntries(args.plantId)
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.toolbarLayout.title = args.plantName
        Glide.with(this)
            .load(Uri.fromFile(File(args.plantImageUri)))
            .into(binding.ivPlantImage)
    }

    private fun setupRecyclerView() {
        diaryAdapter = DiaryAdapter()
        binding.rvDiaryList.apply {
            adapter = diaryAdapter
            layoutManager = LinearLayoutManager(context).apply {
                reverseLayout = false
                stackFromEnd = false
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddDiary.setOnClickListener {
            val content = binding.etDiaryInput.text.toString()
            if (content.isNotBlank()) {
                viewModel.addCustomDiaryEntry(content)
                binding.etDiaryInput.text.clear()
                hideKeyboard()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.diaryEntries.observe(viewLifecycleOwner) { entries ->
            diaryAdapter.submitList(entries)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvDiaryList.adapter = null
        _binding = null
    }
}