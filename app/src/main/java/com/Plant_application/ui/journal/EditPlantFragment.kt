package com.Plant_application.ui.journal

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.Plant_application.R
import com.Plant_application.databinding.FragmentEditPlantBinding
import com.bumptech.glide.Glide
import java.io.File

class EditPlantFragment : Fragment(R.layout.fragment_edit_plant) {

    private var _binding: FragmentEditPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditPlantViewModel by viewModels()
    private val args: EditPlantFragmentArgs by navArgs()

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var toast: Toast? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditPlantBinding.bind(view)

        viewModel.loadPlant(args.plantId)
        setupToolbar()
        setupListeners()
        setupBackButtonHandler()
        observeViewModel()
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarEdit)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarEdit.setNavigationOnClickListener { handleBackButton() }
    }

    private fun observeViewModel() {
        viewModel.plantItemFromDb.observe(viewLifecycleOwner) { dbItem ->
            dbItem?.let { viewModel.setInitialState(it) }
        }

        viewModel.currentPlantItem.observe(viewLifecycleOwner) { plant ->
            plant?.let {
                if (binding.etNickname.text.toString() != it.nickname) {
                    binding.etNickname.setText(it.nickname)
                }
                Glide.with(this).load(Uri.fromFile(File(it.imageUri))).into(binding.ivPlantImage)
            }
        }

        viewModel.canBeSaved.observe(viewLifecycleOwner) { canBeSaved ->
            val saveMenuItem = binding.toolbarEdit.menu.findItem(R.id.menu_save)
            saveMenuItem?.isEnabled = canBeSaved
            val title = saveMenuItem?.title.toString()
            val spannable = SpannableString(title)
            val color = if (canBeSaved) Color.parseColor("#4CAF50") else Color.GRAY
            spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)
            spannable.setSpan(StyleSpan(Typeface.BOLD), 0, spannable.length, 0)
            saveMenuItem?.title = spannable
        }

        viewModel.isChanged.observe(viewLifecycleOwner) { hasChanges ->
            onBackPressedCallback.isEnabled = hasChanges
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.toolbarEdit.menu.findItem(R.id.menu_save)?.isEnabled = !isProcessing && (viewModel.canBeSaved.value == true)
            binding.btnDelete.isEnabled = !isProcessing
            binding.toolbarEdit.navigationIcon = if (isProcessing) null else context?.getDrawable(R.drawable.ic_arrow_back)
        }

        viewModel.isSaveComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) findNavController().popBackStack()
        }

        viewModel.isDeleteComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) {
                showToast("삭제되었습니다.")
                findNavController().popBackStack(R.id.navigation_journal, false)
            }
        }
    }

    private fun setupListeners() {
        binding.toolbarEdit.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_save -> {
                    trySaveChanges()
                    true
                }
                else -> false
            }
        }

        binding.btnDelete.setOnClickListener { showDeleteConfirmDialog() }

        binding.etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateNickname(s.toString())
            }
        })
    }

    private fun setupBackButtonHandler() {
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { showSaveChangesDialog() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun handleBackButton() {
        if (viewModel.isProcessing.value == true) return
        if (viewModel.isChanged.value == true) {
            showSaveChangesDialog()
        } else {
            findNavController().popBackStack()
        }
    }

    private fun showSaveChangesDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("변경사항을 저장하시겠습니까?")
            .setPositiveButton("예") { _, _ -> trySaveChanges() }
            .setNegativeButton("아니오") { _, _ -> findNavController().popBackStack() }
            .setCancelable(true)
            .show()
    }

    private fun trySaveChanges() {
        if (binding.etNickname.text.isNullOrBlank()) {
            showToast("식물 별명을 입력해주세요.")
        } else {
            viewModel.saveChanges()
        }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${viewModel.currentPlantItem.value?.nickname}'을(를) 정말 삭제하시겠습니까?")
            .setPositiveButton("예") { _, _ -> viewModel.deletePlant() }
            .setNegativeButton("아니오", null)
            .show()
    }

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        toast?.cancel()
        _binding = null
    }
}
