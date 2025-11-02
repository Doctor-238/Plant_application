package com.Plant_application.ui.add

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPlantFragment : Fragment(R.layout.fragment_add_plant) {

    private var _binding: FragmentAddPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddPlantViewModel by viewModels()
    private val args: AddPlantFragmentArgs by navArgs()

    private var tempImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var pendingAction: (() -> Unit)? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingAction?.invoke()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showGoToSettingsDialog("카메라 권한이 필요합니다. '설정'으로 이동하여 권한을 허용해주세요.")
            } else {
                showToast("카메라 권한이 거부되었습니다.")
            }
        }
        pendingAction = null
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempImageUri?.let { uri ->
                val bitmap = getCorrectlyOrientedBitmap(uri)
                if (bitmap != null) {
                    viewModel.analyzePlantImage(bitmap)
                } else {
                    showToast("이미지를 불러올 수 없습니다.")
                }
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = getCorrectlyOrientedBitmap(it)
            if (bitmap != null) {
                viewModel.analyzePlantImage(bitmap)
            } else {
                showToast("이미지를 불러올 수 없습니다.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddPlantBinding.bind(view)

        setupToolbar()
        setupImagePicker()
        setupButtons()
        observeViewModel()
        handleBackPress()

        args.plantAnalysis?.let { analysis ->
            showToast("추천받은 식물입니다! 닉네임을 정하고 저장해보세요.")
            viewModel.setRecommendedPlant(analysis, requireContext().applicationContext)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackButton()
        }
    }

    private fun setupImagePicker() {
        binding.frameLayoutPreview.setOnClickListener {
            if (viewModel.isAiAnalyzing.value != true && args.plantAnalysis == null) {
                showImagePickerDialog()
            }
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
                // AI가 식물 종류를 분석 중일 때
                binding.tvAiResultContent.text = "AI가 식물 정보를 생성하는 중입니다..."
                binding.tvAiResultContent.setTextColor(resources.getColor(R.color.text_secondary, null))
                binding.cardAiInfo.isVisible = true
                binding.layoutNickname.isVisible = false
                binding.btnSave.isVisible = false
            } else if (isAnalyzing && viewModel.analysisResult.value != null) {
                // 추천 식물의 이미지를 로드 중일 때
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
                if (args.plantAnalysis != null) {
                    binding.frameLayoutPreview.isClickable = false
                }
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
                val resultText = buildString {
                    append("🌱 식물명: ${result.official_name}\n")
                    append("💧 물 주기: ${result.watering_cycle}\n")
                    append("🌡️ 적정 온도: ${result.temp_range}\n")
                    append("🐛 살충제: ${result.pesticide_cycle}\n")
                    append("⏳ 수명: ${result.lifespan}\n")
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

    private fun showImagePickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("식물 사진 추가")
            .setItems(arrayOf("카메라로 촬영", "갤러리에서 선택")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                pendingAction = { openCamera() }
                showPermissionRationaleDialog()
            }
            else -> {
                pendingAction = { openCamera() }
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("카메라 권한 안내")
            .setMessage("식물 사진을 촬영하기 위해 카메라 권한이 필요합니다.")
            .setPositiveButton("권한 허용") { _, _ ->
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("거부", null)
            .show()
    }

    private fun showGoToSettingsDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("권한 필요")
            .setMessage(message)
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("닫기", null)
            .setCancelable(false)
            .show()
    }

    private fun openCamera() {
        val file = createTempImageFile()
        tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri)
        }
        takePictureLauncher.launch(intent)
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
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