package com.example.cameramicapp.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.cameramicapp.databinding.FragmentSettingsBinding
import com.example.cameramicapp.ui.theme.AppTheme
import com.example.cameramicapp.ui.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var themeManager: ThemeManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        themeManager = ThemeManager(requireContext())

        setupThemeSettings()
        setupStorageInfo()
        setupClearCacheButton()
    }

    private fun setupThemeSettings() {
        // Configurar selección de tema actual
        when (themeManager.getTheme()) {
            AppTheme.IPN -> binding.ipnTheme.isChecked = true
            AppTheme.ESCOM -> binding.escomTheme.isChecked = true
        }

        // Configurar modo oscuro actual
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.followSystem.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> binding.lightMode.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.darkMode.isChecked = true
        }

        // Configurar cambios de tema
        binding.ipnTheme.setOnClickListener {
            themeManager.setTheme(AppTheme.IPN)
            restartApp()
        }

        binding.escomTheme.setOnClickListener {
            themeManager.setTheme(AppTheme.ESCOM)
            restartApp()
        }

        // Configurar cambios de modo oscuro
        binding.followSystem.setOnClickListener {
            ThemeManager.followSystemTheme()
        }

        binding.lightMode.setOnClickListener {
            ThemeManager.setNightMode(false)
        }

        binding.darkMode.setOnClickListener {
            ThemeManager.setNightMode(true)
        }
    }

    private fun setupStorageInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            // Obtener información de almacenamiento
            val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
                File(it, "CameraMicApp")
            }

            var fileCount = 0
            var totalSize = 0L

            mediaDir?.let { dir ->
                if (dir.exists()) {
                    val files = dir.listFiles() ?: emptyArray()
                    fileCount = files.size
                    totalSize = files.sumOf { it.length() }
                }
            }

            withContext(Dispatchers.Main) {
                binding.mediaCount.text = "$fileCount archivos"
                binding.storageUsed.text = formatFileSize(totalSize)
            }
        }
    }

    private fun setupClearCacheButton() {
        binding.clearCacheButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                // Limpiar caché de Glide
                Glide.get(requireContext()).clearDiskCache()

                withContext(Dispatchers.Main) {
                    Glide.get(requireContext()).clearMemory()
                    Toast.makeText(
                        requireContext(),
                        "Caché de miniaturas eliminada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val formatter = DecimalFormat("#.##")
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${formatter.format(size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${formatter.format(size / (1024.0 * 1024.0))} MB"
            else -> "${formatter.format(size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    private fun restartApp() {
        Toast.makeText(
            requireContext(),
            "Reiniciando la aplicación para aplicar el nuevo tema...",
            Toast.LENGTH_SHORT
        ).show()

        // Mostrar mensaje
        Toast.makeText(
            requireContext(),
            "Reiniciando la aplicación para aplicar el nuevo tema...",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(requireContext(), requireActivity()::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        requireActivity().finish()
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}