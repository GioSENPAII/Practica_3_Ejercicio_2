package com.example.cameramicapp.ui.audio

import android.app.Application
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cameramicapp.data.models.MediaItem
import com.example.cameramicapp.data.models.MediaType
import com.example.cameramicapp.data.repositories.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)

    private var recorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var startTime: Long = 0
    private var pausedTime: Long = 0

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _recordingDuration = MutableLiveData(0L)
    val recordingDuration: LiveData<Long> = _recordingDuration

    private val _audioSensitivity = MutableLiveData(50) // 0-100
    val audioSensitivity: LiveData<Int> = _audioSensitivity

    private val _timerDuration = MutableLiveData(0) // 0 = no timer
    val timerDuration: LiveData<Int> = _timerDuration

    private val _isPaused = MutableLiveData(false)
    val isPaused: LiveData<Boolean> = _isPaused

    private var durationTimer: Timer? = null

    // Directorio para guardar grabaciones
    private val outputDirectory: File by lazy {
        val mediaDir = application.externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraMicApp/Audio").apply { mkdirs() }
        }

        if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            File(application.filesDir, "Audio").apply { mkdirs() }
        }
    }

    fun setAudioSensitivity(sensitivity: Int) {
        _audioSensitivity.value = sensitivity.coerceIn(0, 100)
    }

    fun setTimerDuration(seconds: Int) {
        _timerDuration.value = seconds
    }

    fun startRecording() {
        val fileName = createAudioFileName()
        currentRecordingFile = File(outputDirectory, fileName)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(getApplication())
        } else {
            MediaRecorder()
        }

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentRecordingFile?.absolutePath)

            // Configurar sensibilidad del micrófono
            val sensitivity = _audioSensitivity.value ?: 50
            setAudioEncodingBitRate(128000 * sensitivity / 50)
            setAudioSamplingRate(44100)

            try {
                prepare()
                start()
                startTime = System.currentTimeMillis()
                pausedTime = 0
                _isRecording.postValue(true)
                _isPaused.postValue(false)

                // Iniciar timer para mostrar duración
                startDurationTimer()

                // Configurar timer para detener grabación si está habilitado
                val timerDurationValue = _timerDuration.value ?: 0
                if (timerDurationValue > 0) {
                    viewModelScope.launch {
                        delay(timerDurationValue * 1000L)
                        if (_isRecording.value == true) {
                            stopRecording()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resetRecorder()
            }
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                _isPaused.postValue(true)
                pausedTime = System.currentTimeMillis()
                durationTimer?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                _isPaused.postValue(false)
                // Ajustar el tiempo de inicio para compensar el tiempo pausado
                if (pausedTime > 0) {
                    val pauseDuration = System.currentTimeMillis() - pausedTime
                    startTime += pauseDuration
                }
                startDurationTimer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }

            durationTimer?.cancel()
            durationTimer = null

            val duration = System.currentTimeMillis() - startTime

            currentRecordingFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val uri = Uri.fromFile(file)
                    saveAudioToDatabase(uri, duration)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            resetRecorder()
        }
    }

    private fun resetRecorder() {
        recorder = null
        _isRecording.postValue(false)
        _isPaused.postValue(false)
        _recordingDuration.postValue(0)
        pausedTime = 0
    }

    private fun startDurationTimer() {
        durationTimer?.cancel()
        durationTimer = Timer()
        durationTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentDuration = System.currentTimeMillis() - startTime
                _recordingDuration.postValue(currentDuration)
            }
        }, 0, 100)
    }

    private fun createAudioFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "AUDIO_$timestamp.aac"
    }

    private fun saveAudioToDatabase(audioUri: Uri, duration: Long) {
        viewModelScope.launch {
            val file = File(audioUri.path ?: "")
            if (file.exists()) {
                val mediaItem = MediaItem(
                    uri = audioUri,
                    filename = file.name,
                    type = MediaType.AUDIO,
                    creationDate = Date(),
                    duration = duration
                )
                repository.saveMediaItem(mediaItem)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value == true) {
            stopRecording()
        }
        durationTimer?.cancel()
        durationTimer = null
    }
}