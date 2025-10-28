package com.Plant_application

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
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
            binding.navView.visibility = View.GONE // 온보딩에서는 바텀 네비게이션 숨기기
        } else {
            navGraph.setStartDestination(R.id.navigation_home)
        }
        navController.graph = navGraph

        setupBottomNav()
    }

    private fun setupBottomNav() {
        binding.navView.setupWithNavController(navController)

        binding.navView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.navigation_add_plant) {
                navController.navigate(R.id.action_global_addPlantFragment)
                return@setOnItemSelectedListener false // 선택 상태를 변경하지 않음
            }
            if (navController.currentDestination?.id != item.itemId) {
                navController.navigate(item.itemId)
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.onboardingFragment, R.id.addPlantFragment, R.id.plantDetailFragment, R.id.editPlantFragment -> {
                    binding.navView.visibility = View.GONE
                }
                else -> {
                    binding.navView.visibility = View.VISIBLE
                }
            }
        }
    }
}
