package com.Plant_application.ui.journal

import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.Plant_application.R
import com.Plant_application.databinding.FragmentJournalBinding
import kotlinx.coroutines.launch

class JournalFragment : Fragment(R.layout.fragment_journal) {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJournalBinding.bind(view)

        setupViewPager()
        setupSearch()
        setupSortSpinner()
        setupBackButtonHandler()
        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.ivBackDeleteMode.setOnClickListener { viewModel.exitDeleteMode() }

        binding.ivSelectAll.setOnClickListener {
            val state = viewModel.currentTabState.value ?: return@setOnClickListener
            if (state.items.isEmpty()) return@setOnClickListener
            val allSelected = state.items.isNotEmpty() && state.items.all { it.id in state.selectedItemIds }
            if (allSelected) viewModel.deselectAll(state.items) else viewModel.selectAll(state.items)
        }

        binding.btnDelete.setOnClickListener {
            val count = viewModel.selectedItems.value?.size ?: 0
            if (count > 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("${count}개의 식물을 정말 삭제하시겠습니까?")
                    .setPositiveButton("예") { _, _ -> viewModel.deleteSelectedItems() }
                    .setNegativeButton("아니오", null)
                    .show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.currentTabState.observe(viewLifecycleOwner) { state ->
            updateToolbarVisibility(state.isDeleteMode)
            onBackPressedCallback.isEnabled = state.isDeleteMode
            binding.btnDelete.isEnabled = state.selectedItemIds.isNotEmpty()

            if (state.isDeleteMode) {
                binding.ivSelectAll.isEnabled = state.items.isNotEmpty()
                updateSelectAllIcon(state.items.isNotEmpty() && state.items.all { it.id in state.selectedItemIds })
            }
        }

        viewModel.isDeleteMode.observe(viewLifecycleOwner) { notifyAdapterPayload("DELETE_MODE_CHANGED") }
        viewModel.selectedItems.observe(viewLifecycleOwner) { notifyAdapterPayload("SELECTION_CHANGED") }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resetSearchEvent.collect {
                    binding.searchViewJournal.setQuery("", false)
                }
            }
        }
    }

    private fun updateToolbarVisibility(isDeleteMode: Boolean) {
        TransitionManager.beginDelayedTransition(binding.toolbarContainer)
        binding.toolbarNormal.visibility = if (isDeleteMode) View.GONE else View.VISIBLE
        binding.toolbarDelete.visibility = if (isDeleteMode) View.VISIBLE else View.GONE
    }

    private fun updateSelectAllIcon(isChecked: Boolean) {
        binding.ivSelectAll.setImageResource(if (isChecked) R.drawable.ic_checkbox_checked_custom else R.drawable.ic_checkbox_unchecked_custom)
    }

    private fun setupViewPager() {
        binding.viewPagerJournal.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 1
            override fun createFragment(position: Int): Fragment = PlantListFragment.newInstance()
        }
    }

    private fun notifyAdapterPayload(payload: String) {
        // ViewPager2는 프래그먼트를 다시 생성할 수 있으므로, ID로 찾는 것이 더 안정적입니다.
        val fragment = childFragmentManager.findFragmentByTag("f0")
        (fragment as? PlantListFragment)?.notifyAdapter(payload)
    }

    private fun setupSearch() {
        binding.searchViewJournal.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
    }

    private fun setupSortSpinner() {
        val sortOptions = listOf("최신순", "오래된 순", "이름 오름차순", "이름 내림차순")
        binding.spinnerSort.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_centered_normal, sortOptions)
        binding.spinnerSort.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                viewModel.setSortType(sortOptions[pos])
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { viewModel.exitDeleteMode() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        _binding = null
    }
}