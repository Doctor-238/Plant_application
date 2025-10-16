package com.Plant_application

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)

        // 최초 실행 여부 확인
        val prefs = PreferenceManager(this)
        if (prefs.isFirstLaunch) {
            navGraph.setStartDestination(R.id.onboardingFragment)
        } else {
            navGraph.setStartDestination(R.id.navigation_home)
        }
        navController.graph = navGraph

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // 중앙 카메라 아이템은 네비게이션 목적지가 아니므로 직접 클릭 리스너를 설정합니다.
                R.id.navigation_add_plant -> {
                    navController.navigate(R.id.action_global_addPlantFragment)
                    // false를 반환하여 다른 탭이 선택된 상태를 유지하고, 카메라 탭이 활성화되지 않도록 합니다.
                    return@setOnItemSelectedListener false
                }
                else -> {
                    // 기존의 선택된 아이템과 다른 아이템을 눌렀을 때만 navigate 호출
                    if (navController.currentDestination?.id != item.itemId) {
                        navController.navigate(item.itemId)
                    }
                    return@setOnItemSelectedListener true
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // AddPlantFragment는 전체 화면으로 취급하여 네비게이션 바 아이템을 선택하지 않음
            if (destination.id != R.id.addPlantFragment) {
                binding.navView.menu.findItem(destination.id)?.isChecked = true
            }
        }

        // 중앙 카메라 버튼을 다시 눌러도 아무 동작도 하지 않도록 reselected listener 추가
        binding.navView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_add_plant) {
                // 아무것도 하지 않음
            }
        }
    }
}