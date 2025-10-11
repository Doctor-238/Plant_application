package com.Plant_application

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.Plant_application.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        // 중앙 카메라 아이템은 네비게이션 목적지가 아니므로 직접 클릭 리스너를 설정합니다.
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_add_plant -> {
                    navController.navigate(R.id.action_global_addPlantFragment)
                    // true를 반환하면 안됩니다. (선택 상태로 보이지 않게)
                    // false를 반환하여 다른 탭이 선택된 상태를 유지합니다.
                    return@setOnItemSelectedListener false
                }
                else -> {
                    navController.navigate(item.itemId)
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
    }
}