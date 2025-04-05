package com.fouadaha.astralis

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fouadaha.astralis.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_main) as NavHostFragment
        val navController = navHostFragment.navController
        binding.mainMenu.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.bodyDetailFragment) {
                binding.mainMenu.visibility = View.GONE
            } else {
                binding.mainMenu.visibility = View.VISIBLE

            }
        }
    }
}