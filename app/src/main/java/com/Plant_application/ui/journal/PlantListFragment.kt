package com.Plant_application.ui.journal

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.Plant_application.R
import com.Plant_application.databinding.FragmentPlantListBinding

class PlantListFragment : Fragment(R.layout.fragment_plant_list) {

    private var _binding: FragmentPlantListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: JournalAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlantListBinding.bind(view)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = JournalAdapter(
            onItemClicked = { clickedItem ->
                if (viewModel.isDeleteMode.value == true) {
                    viewModel.toggleItemSelection(clickedItem.id)
                } else {
                    val action = JournalFragmentDirections.actionNavigationJournalToPlantDetailFragment(clickedItem.id)
                    findNavController().navigate(action)
                }
            },
            onItemLongClicked = { longClickedItem -> viewModel.enterDeleteMode(longClickedItem.id) },
            isDeleteMode = { viewModel.isDeleteMode.value ?: false },
            isItemSelected = { itemId -> viewModel.selectedItems.value?.contains(itemId) ?: false }
        )
        binding.recyclerViewPlantList.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPlantList.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.getPlantsForCurrentTab().observe(viewLifecycleOwner) { plants ->
            adapter.submitList(plants)
            binding.emptyViewContainer.isVisible = plants.isEmpty() && viewModel.searchQuery.value.isNullOrEmpty()
        }
    }

    fun notifyAdapter(payload: String) {
        if (::adapter.isInitialized) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, payload)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewPlantList.adapter = null
        _binding = null
    }

    companion object {
        fun newInstance() = PlantListFragment()
    }
}