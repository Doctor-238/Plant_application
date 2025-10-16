package com.Plant_application.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Plant_application.MainActivity
import com.Plant_application.R
import com.Plant_application.databinding.FragmentMyPageBinding

class MyPageFragment : Fragment(R.layout.fragment_my_page) {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyPageBinding.bind(view)

        binding.btnResetApp.setOnClickListener {
            showResetConfirmDialog()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.btnResetApp.isEnabled = !isProcessing
            binding.btnResetApp.text = if (isProcessing) "초기화 중..." else "앱 전체 데이터 초기화"
        }

        viewModel.resetComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) {
                Toast.makeText(requireContext(), "모든 데이터가 초기화되었습니다. 앱을 다시 시작합니다.", Toast.LENGTH_LONG).show()

                // 앱 재시작
                val packageManager = requireContext().packageManager
                val intent = packageManager.getLaunchIntentForPackage(requireContext().packageName)
                val componentName = intent!!.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                requireContext().startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }

    private fun showResetConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("전체 초기화")
            .setMessage("저장된 모든 식물 정보와 설정이 영구적으로 삭제됩니다. 정말 진행하시겠습니까?")
            .setPositiveButton("예") { _, _ -> viewModel.resetAllData() }
            .setNegativeButton("아니오", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}