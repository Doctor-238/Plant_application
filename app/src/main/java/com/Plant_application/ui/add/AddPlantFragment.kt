package com.Plant_application.ui.add

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var toast: Toast? = null
    private var tempImageUri: Uri? = null

    // 카메라 앱 결과 콜백
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempImageUri?.let { uri ->
                val bitmap = getCorrectlyOrientedBitmap(uri)
                if (bitmap != null) {
                    viewModel.onImageSelected(bitmap, getString(R.string.gemini_api_key))
                } else {
                    showToast("사진을 불러오는 데 실패했습니다.")
                }
            }
        }
    }

    // 갤러리 앱 결과 콜백
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = getCorrectlyOrientedBitmap(it)
            if (bitmap != null) {
                viewModel.onImageSelected(bitmap, getString(R.string.gemini_api_key))
            } else {
                showToast("이미지를 불러오는 데 실패했습니다.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddPlantBinding.bind(view)

        // 전달받은 인자가 있다면 ViewModel 상태 초기화
        args.plantAnalysis?.let { analysis ->
            viewModel.setInitialAnalysis(analysis)
            binding.textViewPlaceholder.text = "추천받은 식물의 사진을 추가해주세요!"
        }

        setupListeners()
        setupBackButtonHandler()
        observeViewModel()
    }

    private fun observeViewModel() {
        // AI 분석 중 상태 관찰
        viewModel.isAiAnalyzing.observe(viewLifecycleOwner) { isAnalyzing ->
            binding.progressBar.isVisible = isAnalyzing
            binding.buttonSave.isEnabled = !isAnalyzing && viewModel.originalBitmap.value != null && !binding.editTextPlantNickname.text.isNullOrBlank()
            if (isAnalyzing) {
                binding.cardAiInfo.isVisible = false
            }
        }

        // 저장 중 상태 관찰
        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            binding.savingOverlay.isVisible = isSaving
        }

        // 원본 이미지 비트맵 관찰
        viewModel.originalBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.textViewPlaceholder.isVisible = false
                binding.imageViewPlantPreview.isVisible = true
                binding.imageViewPlantPreview.setImageBitmap(bitmap)
            } else {
                binding.textViewPlaceholder.isVisible = true
                binding.imageViewPlantPreview.isVisible = false
                binding.imageViewPlantPreview.setImageDrawable(null)
                binding.cardAiInfo.isVisible = false
            }
        }

        // AI 분석 결과 관찰
        viewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            binding.cardAiInfo.isVisible = result != null
            if (result != null) {
                binding.tvInfoOfficialName.text = "공식 이름: ${result.official_name ?: "정보 없음"}"
                binding.tvInfoHealth.text = "건강 상태: ${"★".repeat(result.health_rating?.toInt() ?: 0)}${"☆".repeat(5 - (result.health_rating?.toInt() ?: 0))}"
                binding.tvInfoWatering.text = "물 주기: ${result.watering_cycle ?: "정보 없음"}"
                binding.tvInfoPesticide.text = "살충제: ${result.pesticide_cycle ?: "정보 없음"}"
                binding.tvInfoTemp.text = "적정 온도: ${result.temp_range ?: "정보 없음"}"
                binding.tvInfoLifespan.text = "예상 수명: ${result.lifespan ?: "정보 없음"}"
            }
        }

        // 저장 완료 상태 관찰
        viewModel.isSaveCompleted.observe(viewLifecycleOwner) { isCompleted ->
            if (isCompleted) {
                findNavController().popBackStack()
            }
        }

        // 오류 메시지 관찰
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                showToast(message, Toast.LENGTH_LONG)
                viewModel.clearErrorMessage()
            }
        }

        // 변경 사항 여부 관찰 (뒤로가기 제어용)
        viewModel.hasChanges.observe(viewLifecycleOwner) { hasChanges ->
            onBackPressedCallback.isEnabled = hasChanges
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { handleBackButton() }
        binding.frameLayoutPreview.setOnClickListener { showImagePickerDialog() }
        binding.buttonSave.setOnClickListener { savePlantItem() }

        binding.editTextPlantNickname.addTextChangedListener { editable ->
            binding.buttonSave.isEnabled = (viewModel.isAiAnalyzing.value == false) &&
                    (viewModel.originalBitmap.value != null) &&
                    !editable.isNullOrBlank()
        }
    }

    private fun showImagePickerDialog() {
        if (viewModel.isAiAnalyzing.value == true) return
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(requireContext())
            .setTitle("사진 추가")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
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
        val storageDir = requireContext().cacheDir // 캐시 디렉토리 사용
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // 이미지 회전 문제를 해결하는 함수
    private fun getCorrectlyOrientedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            inputStream = requireContext().contentResolver.openInputStream(uri) ?: return originalBitmap
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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

    private fun savePlantItem() {
        val nickname = binding.editTextPlantNickname.text.toString().trim()
        if (nickname.isEmpty()) {
            showToast("식물 별명을 입력해주세요.")
            return
        }
        // 키보드 숨기기
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        viewModel.savePlant(nickname)
    }

    private fun setupBackButtonHandler() {
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                showCancelDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun handleBackButton() {
        if (viewModel.isSaving.value == true) return
        if (onBackPressedCallback.isEnabled) {
            showCancelDialog()
        } else {
            findNavController().popBackStack()
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        toast?.cancel()
        toast = Toast.makeText(requireContext(), message, duration)
        toast?.show()
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("작업을 취소하시겠습니까? 변경사항이 저장되지 않습니다.")
            .setPositiveButton("예") { _, _ ->
                viewModel.resetAllState()
                findNavController().popBackStack()
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        toast?.cancel()
        _binding = null
    }
}