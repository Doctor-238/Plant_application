package com.Plant_application.ui.mypage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Plant_application.R
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.databinding.FragmentMyPageBinding
import com.google.android.material.switchmaterial.SwitchMaterial

class MyPageFragment : Fragment(R.layout.fragment_my_page) {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPageViewModel by viewModels()
    private lateinit var prefs: PreferenceManager

    private var pendingSwitch: SwitchMaterial? = null
    private var pendingState: Boolean = false

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingSwitch?.let {
                updatePref(it.id, pendingState)
            }
        } else {
            Toast.makeText(requireContext(), "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            pendingSwitch?.isChecked = false
        }
        pendingSwitch = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyPageBinding.bind(view)
        prefs = PreferenceManager(requireContext())

        setupToggles()
        setupListeners()
        observeViewModel()
    }

    private fun setupToggles() {
        binding.switchNotifWater.isChecked = prefs.notifWaterEnabled
        binding.switchNotifPesticide.isChecked = prefs.notifPesticideEnabled
        binding.switchNotifTemp.isChecked = prefs.notifTempEnabled
    }

    private fun setupListeners() {
        binding.btnResetApp.setOnClickListener {
            showResetConfirmDialog()
        }

        binding.switchNotifWater.setOnCheckedChangeListener { _, isChecked ->
            handleToggle(binding.switchNotifWater, isChecked)
        }
        binding.switchNotifPesticide.setOnCheckedChangeListener { _, isChecked ->
            handleToggle(binding.switchNotifPesticide, isChecked)
        }
        binding.switchNotifTemp.setOnCheckedChangeListener { _, isChecked ->
            handleToggle(binding.switchNotifTemp, isChecked)
        }
    }

    private fun handleToggle(switchView: SwitchMaterial, isChecked: Boolean) {
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        updatePref(switchView.id, true)
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        pendingSwitch = switchView
                        pendingState = true
                        showNotificationRationale()
                    }
                    else -> {
                        pendingSwitch = switchView
                        pendingState = true
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                updatePref(switchView.id, true)
            }
        } else {
            updatePref(switchView.id, false)
        }
    }

    private fun updatePref(switchId: Int, isEnabled: Boolean) {
        when (switchId) {
            R.id.switch_notif_water -> prefs.notifWaterEnabled = isEnabled
            R.id.switch_notif_pesticide -> prefs.notifPesticideEnabled = isEnabled
            R.id.switch_notif_temp -> prefs.notifTempEnabled = isEnabled
        }
    }

    private fun showNotificationRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("알림 권한 필요")
            .setMessage("식물 관리 알림을 받으려면 알림 권한이 필요합니다.")
            .setPositiveButton("권한 허용") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("취소") { _, _ ->
                pendingSwitch?.isChecked = false
                pendingSwitch = null
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.btnResetApp.isEnabled = !isProcessing
            binding.btnResetApp.text = if (isProcessing) "초기화 중..." else "앱 전체 데이터 초기화"
        }

        viewModel.resetComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) {
                Toast.makeText(requireContext(), "모든 데이터가 초기화되었습니다. 앱을 다시 시작합니다.", Toast.LENGTH_LONG).show()

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