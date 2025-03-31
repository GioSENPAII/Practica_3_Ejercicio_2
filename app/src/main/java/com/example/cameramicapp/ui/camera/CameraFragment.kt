package com.example.cameramicapp.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.cameramicapp.R
import com.example.cameramicapp.databinding.FragmentCameraBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CameraViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var countdownJob: kotlinx.coroutines.Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startCamera()
            Toast.makeText(
                requireContext(),
                "Permisos concedidos. Ya puedes usar la cámara.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Los permisos de cámara son necesarios para esta función",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)

        // Inicializar el ejecutor de la cámara
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Solicitar permisos si es necesario
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Configurar botón de captura
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        // Configurar botón de ajustes
        binding.settingsButton.setOnClickListener {
            binding.photoSettingsPanel.isVisible = !binding.photoSettingsPanel.isVisible
        }

        // Configurar botón de flash
        binding.flashButton.setOnClickListener {
            when (viewModel.flashMode.value) {
                FlashMode.AUTO -> viewModel.setFlashMode(FlashMode.ON)
                FlashMode.ON -> viewModel.setFlashMode(FlashMode.OFF)
                FlashMode.OFF -> viewModel.setFlashMode(FlashMode.AUTO)
                else -> viewModel.setFlashMode(FlashMode.AUTO)
            }
        }

        // Configurar filtros
        binding.normalFilter.setOnClickListener {
            viewModel.setFilter(CameraFilter.NORMAL)
            applyFilter(CameraFilter.NORMAL)
        }
        binding.grayscaleFilter.setOnClickListener {
            viewModel.setFilter(CameraFilter.GRAYSCALE)
            applyFilter(CameraFilter.GRAYSCALE)
        }
        binding.sepiaFilter.setOnClickListener {
            viewModel.setFilter(CameraFilter.SEPIA)
            applyFilter(CameraFilter.SEPIA)
        }
        binding.negativeFilter.setOnClickListener {
            viewModel.setFilter(CameraFilter.NEGATIVE)
            applyFilter(CameraFilter.NEGATIVE)
        }
        binding.vintageFilter.setOnClickListener {
            viewModel.setFilter(CameraFilter.VINTAGE)
            applyFilter(CameraFilter.VINTAGE)
        }

        // Configurar temporizador
        binding.timerOff.setOnClickListener { viewModel.setTimerDuration(0) }
        binding.timer3s.setOnClickListener { viewModel.setTimerDuration(3) }
        binding.timer10s.setOnClickListener { viewModel.setTimerDuration(10) }
    }

    private fun observeViewModel() {
        viewModel.flashMode.observe(viewLifecycleOwner) { mode ->
            updateFlashIcon(mode)
            imageCapture?.flashMode = when (mode) {
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_AUTO
            }
        }

        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            applyFilter(filter)
        }
    }

    private fun updateFlashIcon(mode: FlashMode) {
        val icon = when (mode) {
            FlashMode.ON -> android.R.drawable.ic_menu_compass
            FlashMode.OFF -> android.R.drawable.ic_menu_close_clear_cancel
            FlashMode.AUTO -> android.R.drawable.ic_menu_compass
        }
        binding.flashButton.setImageResource(icon)
    }

    private fun applyFilter(filter: CameraFilter) {
        when (filter) {
            CameraFilter.NORMAL -> {
                binding.filterOverlay.visibility = View.GONE
            }
            CameraFilter.GRAYSCALE -> {
                binding.filterOverlay.visibility = View.VISIBLE
                binding.filterOverlay.background =
                    ColorDrawable(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                binding.filterOverlay.background.alpha = 80
                // En una app real, aplicaríamos un filtro real usando CameraX o procesamiento de imágenes
            }
            CameraFilter.SEPIA -> {
                binding.filterOverlay.visibility = View.VISIBLE
                binding.filterOverlay.background =
                    ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ipn_primary))
                binding.filterOverlay.background.alpha = 60
            }
            CameraFilter.NEGATIVE -> {
                binding.filterOverlay.visibility = View.VISIBLE
                binding.filterOverlay.background =
                    ColorDrawable(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                binding.filterOverlay.background.alpha = 80
            }
            CameraFilter.VINTAGE -> {
                binding.filterOverlay.visibility = View.VISIBLE
                binding.filterOverlay.background =
                    ColorDrawable(ContextCompat.getColor(requireContext(), R.color.escom_primary))
                binding.filterOverlay.background.alpha = 40
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Cancelar cualquier cuenta regresiva activa
        countdownJob?.cancel()

        val timerDuration = viewModel.timerDuration.value ?: 0
        if (timerDuration > 0) {
            // Iniciar cuenta regresiva
            binding.timerText.isVisible = true
            countdownJob = lifecycleScope.launch {
                for (i in timerDuration downTo 1) {
                    binding.timerText.text = i.toString()
                    delay(1000)
                }
                binding.timerText.isVisible = false
                captureImage(imageCapture)
            }
        } else {
            // Capturar inmediatamente
            captureImage(imageCapture)
        }
    }

    private fun captureImage(imageCapture: ImageCapture) {
        // Crear archivo de salida
        val photoFile = viewModel.createImageFile()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Foto guardada: $savedUri"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                    // Guardar en la galería
                    viewModel.savePhotoToGallery(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error al capturar la foto: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
    }
}