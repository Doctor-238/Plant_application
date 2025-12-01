package com.Plant_application.ui.journal

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ItemJournalDetailBinding.bind(view)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupBackButtonHandler()
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
        diaryAdapter = DiaryAdapter(
            onItemClick = { entry ->
                if (viewModel.isDeleteMode.value == true) {
                    viewModel.toggleItemSelection(entry.id)
                }
            },
            onItemLongClick = { entry ->
                viewModel.enterDeleteMode(entry.id)
            },
            isDeleteMode = { viewModel.isDeleteMode.value ?: false },
            isSelected = { id -> viewModel.selectedItems.value?.contains(id) ?: false }
        )
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

        binding.ivBackDeleteMode.setOnClickListener {
            viewModel.exitDeleteMode()
        }

        binding.btnDelete.setOnClickListener {
            val count = viewModel.selectedItems.value?.size ?: 0
            if (count > 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("${count}개의 일지를 삭제하시겠습니까? (연동된 일정도 삭제됩니다)")
                    .setPositiveButton("예") { _, _ -> viewModel.deleteSelectedEntries() }
                    .setNegativeButton("아니오", null)
                    .show()
            }
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (viewModel.isDeleteMode.value == true) {
                    viewModel.exitDeleteMode()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun observeViewModel() {
        viewModel.diaryEntries.observe(viewLifecycleOwner) { entries ->
            diaryAdapter.submitList(entries)
        }

        viewModel.isDeleteMode.observe(viewLifecycleOwner) { isDelete ->
            onBackPressedCallback.isEnabled = isDelete
            binding.toolbar.isVisible = !isDelete
            binding.toolbarDelete.isVisible = isDelete
            binding.layoutInputArea.isVisible = !isDelete // 삭제 모드일 땐 입력창 숨김

            diaryAdapter.notifyItemRangeChanged(0, diaryAdapter.itemCount, "DELETE_MODE_CHANGED")
        }

        viewModel.selectedItems.observe(viewLifecycleOwner) { selectedIds ->
            binding.btnDelete.isEnabled = selectedIds.isNotEmpty()
            diaryAdapter.notifyItemRangeChanged(0, diaryAdapter.itemCount, "SELECTION_CHANGED")
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        binding.rvDiaryList.adapter = null
        _binding = null
    }
}