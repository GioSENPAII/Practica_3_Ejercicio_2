package com.example.cameramicapp.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cameramicapp.R
import com.example.cameramicapp.databinding.FragmentAudioBinding
import com.example.cameramicapp.sensors.AppSensorManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class AudioFragment : Fragment() {
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AudioViewModel

    // Sensor manager para detectar proximidad
    private lateinit var sensorManager: AppSensorManager
    private var isProximityPauseEnabled = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Los permisos fueron concedidos
            Toast.makeText(
                requireContext(),
                "Permisos concedidos. Ya puedes usar el micrófono.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Los permisos de micrófono son necesarios para esta función",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AudioViewModel::class.java)

        // Inicializar sensor manager
        sensorManager = AppSensorManager(requireContext())

        setupUI()
        observeViewModel()
        setupProximitySensor()

        // Solicitar permisos al inicio
        if (!allPermissionsGranted()) {
            requestAudioPermissions()
        }
    }

    private fun setupProximitySensor() {
        // Observar cambios en el sensor de proximidad para pausa automática
        sensorManager.isNear.observe(viewLifecycleOwner) { isNear ->
            if (isProximityPauseEnabled && viewModel.isRecording.value == true) {
                if (isNear) {
                    // Pausar grabación cuando se detecta proximidad
                    viewModel.pauseRecording()
                    binding.proximityIndicator.visibility = View.VISIBLE
                    binding.proximityIndicator.text = "⏸️ Grabación pausada - objeto cerca detectado"
                    binding.proximityIndicator.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
                    )
                } else {
                    // Reanudar grabación cuando no hay proximidad
                    viewModel.resumeRecording()
                    binding.proximityIndicator.text = "🎙️ Grabación reanudada"
                    binding.proximityIndicator.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                    )

                    // Ocultar el indicador después de 2 segundos
                    binding.proximityIndicator.postDelayed({
                        binding.proximityIndicator.visibility = View.GONE
                    }, 2000)
                }
            }
        }
    }

    private fun setupUI() {
        // Configurar botón de grabación
        binding.recordButton.setOnClickListener {
            if (viewModel.isRecording.value == true) {
                viewModel.stopRecording()
            } else {
                if (allPermissionsGranted()) {
                    viewModel.startRecording()
                } else {
                    requestAudioPermissions()
                }
            }
        }

        // Configurar botón de pausa por proximidad
        binding.proximityPauseButton.setOnClickListener {
            toggleProximityPause()
        }

        // Configurar seekbar de sensibilidad
        binding.sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.sensitivityText.text = "$progress%"
                if (fromUser) {
                    viewModel.setAudioSensitivity(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar opciones de temporizador
        binding.timerOff.setOnClickListener { viewModel.setTimerDuration(0) }
        binding.timer30s.setOnClickListener { viewModel.setTimerDuration(30) }
        binding.timer1min.setOnClickListener { viewModel.setTimerDuration(60) }
        binding.timer5min.setOnClickListener { viewModel.setTimerDuration(300) }
    }

    private fun toggleProximityPause() {
        isProximityPauseEnabled = !isProximityPauseEnabled

        if (isProximityPauseEnabled) {
            sensorManager.startProximityListening()
            binding.proximityPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(
                requireContext(),
                "Pausa por proximidad activada: acerca la mano al sensor para pausar grabaciones",
                Toast.LENGTH_LONG
            ).show()
        } else {
            sensorManager.stopProximityListening()
            binding.proximityPauseButton.setImageResource(android.R.drawable.ic_menu_help)
            binding.proximityIndicator.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Pausa por proximidad desactivada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        viewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            updateRecordButton(isRecording)
        }

        viewModel.recordingDuration.observe(viewLifecycleOwner) { duration ->
            updateTimerDisplay(duration)
        }

        viewModel.audioSensitivity.observe(viewLifecycleOwner) { sensitivity ->
            binding.sensitivitySeekBar.progress = sensitivity
            binding.sensitivityText.text = "$sensitivity%"
        }

        viewModel.timerDuration.observe(viewLifecycleOwner) { timerDuration ->
            // Seleccionar el radiobutton correcto
            when (timerDuration) {
                0 -> binding.timerOff.isChecked = true
                30 -> binding.timer30s.isChecked = true
                60 -> binding.timer1min.isChecked = true
                300 -> binding.timer5min.isChecked = true
            }
        }

        viewModel.isPaused.observe(viewLifecycleOwner) { isPaused ->
            if (isPaused && viewModel.isRecording.value == true) {
                binding.recordButton.setImageResource(android.R.drawable.ic_media_play)
                binding.recordingLabel.text = "Grabación pausada"
                binding.microphoneImage.animate().scaleX(1.0f).scaleY(1.0f).duration = 300
            }
        }
    }

    private fun updateRecordButton(isRecording: Boolean) {
        if (isRecording) {
            binding.recordButton.setImageResource(android.R.drawable.ic_media_pause)
            binding.microphoneImage.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.microphoneImage.animate().scaleX(1.2f).scaleY(1.2f).duration = 300
            binding.recordingLabel.text = "Grabando..."
            binding.proximityPauseButton.isEnabled = true
        } else {
            binding.recordButton.setImageResource(android.R.drawable.ic_media_play)
            binding.microphoneImage.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.microphoneImage.animate().scaleX(1.0f).scaleY(1.0f).duration = 300
            binding.recordingLabel.text = "Grabación de Audio"
            binding.proximityPauseButton.isEnabled = false
            binding.proximityIndicator.visibility = View.GONE
        }
    }

    private fun updateTimerDisplay(durationMillis: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) -
                TimeUnit.MINUTES.toSeconds(minutes)

        val timeString = String.format("%02d:%02d", minutes, seconds)
        binding.recordingTimerText.text = timeString
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onResume() {
        super.onResume()
        if (isProximityPauseEnabled) {
            sensorManager.startProximityListening()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.stopProximityListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.stopAllSensors()
        _binding = null
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
    }
}