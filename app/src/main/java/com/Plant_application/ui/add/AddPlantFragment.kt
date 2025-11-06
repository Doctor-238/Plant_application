package com.Plant_application.ui.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.Plant_application.R
import com.Plant_application.databinding.FragmentAddPlantBinding
import java.io.InputStream
import java.util.Locale

class AddPlantFragment : Fragment(R.layout.fragment_add_plant) {

    private var _binding: FragmentAddPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddPlantViewModel by viewModels()
    private val args: AddPlantFragmentArgs by navArgs()

    private var selectedBitmap: Bitmap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddPlantBinding.bind(view)

        setupToolbar()
        setupButtons()
        observeViewModel()
        handleBackPress()

        if (args.plantAnalysis != null) {
            showToast("추천받은 식물입니다! 닉네임을 정하고 저장해보세요.")
            viewModel.setRecommendedPlant(args.plantAnalysis!!, requireContext().applicationContext)
        } else if (args.imageUri != null) {
            val uri = Uri.parse(args.imageUri)
            val bitmap = getCorrectlyOrientedBitmap(uri)
            if (bitmap != null) {
                viewModel.analyzePlantImage(bitmap)
            } else {
                showToast("이미지를 불러올 수 없습니다.")
                findNavController().popBackStack()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackButton()
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            hideKeyboard()
            val nickname = binding.etNickname.text?.toString() ?: ""
            viewModel.savePlantToDatabase(nickname)
        }

        binding.etNickname.addTextChangedListener {
            viewModel.clearError()
        }
    }

    private fun observeViewModel() {
        viewModel.isAiAnalyzing.observe(viewLifecycleOwner) { isAnalyzing ->
            binding.progressBar.isVisible = isAnalyzing
            binding.textViewPlaceholder.isVisible = !isAnalyzing && viewModel.originalBitmap.value == null

            if (isAnalyzing && viewModel.analysisResult.value == null) {
                binding.tvAiResultContent.text = "AI가 식물 정보를 생성하는 중입니다..."
                binding.tvAiResultContent.setTextColor(resources.getColor(R.color.text_secondary, null))
                binding.cardAiInfo.isVisible = true
                binding.layoutNickname.isVisible = false
                binding.btnSave.isVisible = false
            } else if (isAnalyzing && viewModel.analysisResult.value != null) {
                binding.progressBar.isVisible = true
                binding.textViewPlaceholder.isVisible = false
            }
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            binding.btnSave.isEnabled = !isSaving
            binding.btnSave.text = if (isSaving) "저장 중..." else "저장하기"
        }

        viewModel.originalBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                displayImage(bitmap)
                binding.frameLayoutPreview.isClickable = false
            } else {
                selectedBitmap = null
                binding.imageViewPlantPreview.setImageBitmap(null)
                binding.imageViewPlantPreview.isVisible = false
                binding.textViewPlaceholder.isVisible = true
                binding.cardAiInfo.isVisible = false
                binding.layoutNickname.isVisible = false
                binding.btnSave.isVisible = false
                binding.frameLayoutPreview.isClickable = true
            }
        }

        viewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                val waterRange = formatRange(result.watering_cycle_min_days ?: 0, result.watering_cycle_max_days ?: 0, "일")
                val pesticideRange = formatRange(result.pesticide_cycle_min_days ?: 0, result.pesticide_cycle_max_days ?: 0, "일")
                val lifespanRange = formatRange(result.lifespan_min_years ?: 0, result.lifespan_max_years ?: 0, "년")

                val resultText = buildString {
                    append("🌱 식물명: ${result.official_name}\n")
                    append("💧 물 주기: $waterRange\n")
                    append("🌡️ 적정 온도: ${result.temp_range}\n")
                    append("🐛 살충제: $pesticideRange\n")
                    append("⏳ 수명: $lifespanRange\n")
                    append("❤️ 건강도: ${result.health_rating}/5.0")
                }
                binding.tvAiResultContent.text = resultText
                binding.tvAiResultContent.setTextColor(resources.getColor(R.color.text_primary, null))
                binding.cardAiInfo.isVisible = true
                binding.layoutNickname.isVisible = true
                binding.btnSave.isVisible = true
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.saveComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) {
                showToast("식물이 저장되었습니다!")

                if (args.plantAnalysis != null) {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.onboardingFragment, true)
                        .build()
                    findNavController().navigate(R.id.navigation_home, null, navOptions)
                } else {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun formatRange(min: Int, max: Int, unit: String): String {
        return when {
            max <= 0 -> "필요 없음"
            min == max -> "$max$unit"
            else -> "$min-$max$unit"
        }
    }

    private fun getCorrectlyOrientedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            inputStream = requireContext().contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun displayImage(bitmap: Bitmap) {
        selectedBitmap = bitmap
        binding.imageViewPlantPreview.setImageBitmap(bitmap)
        binding.imageViewPlantPreview.isVisible = true
        binding.textViewPlaceholder.isVisible = false
    }

    private fun handleBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isAiAnalyzing.value == true || viewModel.isSaving.value == true) {
                    showToast("작업이 진행 중입니다.")
                    return
                }

                val cameFromOnboarding = args.plantAnalysis != null

                if (cameFromOnboarding) {
                    viewModel.resetState()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.onboardingFragment, true)
                        .build()
                    findNavController().navigate(R.id.navigation_home, null, navOptions)
                    return
                }

                val hasChanges = selectedBitmap != null

                val exitAction = {
                    viewModel.resetState()
                    findNavController().popBackStack()
                }

                if (hasChanges) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("페이지 나가기")
                        .setMessage("변경사항이 저장되지 않았습니다. 정말 나가시겠습니까?")
                        .setPositiveButton("나가기") { _, _ -> exitAction() }
                        .setNegativeButton("취소", null)
                        .show()
                } else {
                    exitAction()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackButton() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        selectedBitmap?.recycle()
        selectedBitmap = null
        _binding = null
    }
}