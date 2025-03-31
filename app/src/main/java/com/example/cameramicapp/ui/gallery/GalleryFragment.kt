package com.example.cameramicapp.ui.gallery

import kotlinx.coroutines.isActive
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.cameramicapp.R
import com.example.cameramicapp.data.models.MediaItem
import com.example.cameramicapp.data.models.MediaType
import com.example.cameramicapp.databinding.FragmentGalleryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import android.util.Log

class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GalleryViewModel
    private lateinit var adapter: MediaAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var updateSeekbarJob: Job? = null
    private var detailViewVisible = false

    private val categories = listOf("Default", "Trabajo", "Personal", "Vacaciones", "Otros")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)

        setupRecyclerView()
        setupTabs()
        setupDetailView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MediaAdapter { mediaItem ->
            viewModel.selectItem(mediaItem)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadAllMedia()
                    1 -> viewModel.loadPhotoItems()
                    2 -> viewModel.loadAudioItems()
                    3 -> viewModel.loadFavorites()
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupDetailView() {
        // Configurar cierre de vista detalle
        binding.closeDetailButton.setOnClickListener {
            lifecycleScope.launch {
                hideDetailView()
            }
        }

        binding.mediaDetailContainer.setOnClickListener {
            // Solo cerrar si se hace clic fuera de los controles
            if (it == binding.mediaDetailContainer) {
                lifecycleScope.launch {
                    hideDetailView()
                }
            }
        }

        // Configurar botón de favorito
        binding.favoriteButton.setOnClickListener {
            viewModel.selectedItem.value?.let { item ->
                viewModel.toggleFavorite(item)
            }
        }

        // Configurar botón de categoría
        binding.categoryButton.setOnClickListener {
            viewModel.selectedItem.value?.let { item ->
                showCategoryDialog(item)
            }
        }

        // Configurar botón de compartir
        binding.shareButton.setOnClickListener {
            viewModel.selectedItem.value?.let { item ->
                shareMediaItem(item)
            }
        }

        // Configurar botón de eliminar
        binding.deleteButton.setOnClickListener {
            viewModel.selectedItem.value?.let { item ->
                showDeleteConfirmationDialog(item)
            }
        }

        // Configurar reproductor de audio
        binding.audioPlaybackIcon.setOnClickListener {
            toggleAudioPlayback()
        }

        binding.audioPlaybackSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    updateAudioTimeDisplay(progress.toLong(), mediaPlayer?.duration?.toLong() ?: 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No es necesario implementar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // No es necesario implementar
            }
        })
    }

    private fun observeViewModel() {
        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            // Mostrar vista vacía si no hay elementos
            binding.emptyView.visibility = if (items.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (items.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            if (item != null && !detailViewVisible) {
                lifecycleScope.launch {
                    showDetailView(item)
                }
            } else if (item == null && detailViewVisible) {
                lifecycleScope.launch {
                    hideDetailView()
                }
            }
        }
    }

    private suspend fun showDetailView(item: MediaItem) {
        withContext(Dispatchers.Main) {
            // Asegurar que el reproductor anterior esté liberado
            releaseMediaPlayer()

            when (item.type) {
                MediaType.PHOTO -> {
                    binding.photoDetailView.visibility = View.VISIBLE
                    binding.audioDetailView.visibility = View.GONE

                    try {
                        // Cargar la imagen con Glide
                        Glide.with(requireContext())
                            .load(item.uri)
                            .fitCenter()
                            .into(binding.photoDetailView)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                MediaType.AUDIO -> {
                    binding.photoDetailView.visibility = View.GONE
                    binding.audioDetailView.visibility = View.VISIBLE

                    binding.audioFileName.text = item.filename
                    setupAudioPlayer(item)
                }
            }

            // Actualizar icono de favorito
            updateFavoriteIcon(item.favorite)

            // Mostrar el contenedor de detalles
            binding.mediaDetailContainer.visibility = View.VISIBLE
            detailViewVisible = true
        }
    }

    private suspend fun hideDetailView() {
        withContext(Dispatchers.Main) {
            // Cancelar jobs de coroutines
            updateSeekbarJob?.cancel()
            updateSeekbarJob = null

            try {
                // Liberar reproductor si existe
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
                mediaPlayer = null

                // Limpiar imagen cargada por Glide
                if (binding.photoDetailView.visibility == View.VISIBLE) {
                    Glide.with(requireContext()).clear(binding.photoDetailView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Ocultar vista de detalle
            binding.mediaDetailContainer.visibility = View.GONE
            detailViewVisible = false

            // Notificar al ViewModel
            if (viewModel.selectedItem.value != null) {
                viewModel.clearSelection()
            }
        }
    }

    private fun setupAudioPlayer(item: MediaItem) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), item.uri)
                prepare()

                // Configurar barra de progreso
                binding.audioPlaybackSlider.max = duration
                binding.audioPlaybackSlider.progress = 0

                // Mostrar duración total
                updateAudioTimeDisplay(0, duration.toLong())

                // Cambiar icono a play
                binding.audioPlaybackIcon.setImageResource(android.R.drawable.ic_media_play)

                // Configurar listener para cuando termina la reproducción
                setOnCompletionListener {
                    binding.audioPlaybackSlider.progress = 0
                    binding.audioPlaybackIcon.setImageResource(android.R.drawable.ic_media_play)
                    updateAudioTimeDisplay(0, mediaPlayer?.duration?.toLong() ?: 0)
                    updateSeekbarJob?.cancel()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Error al reproducir el audio: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleAudioPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                // Pausar reproducción
                player.pause()
                binding.audioPlaybackIcon.setImageResource(android.R.drawable.ic_media_play)
                updateSeekbarJob?.cancel()
            } else {
                // Iniciar reproducción
                player.start()
                binding.audioPlaybackIcon.setImageResource(android.R.drawable.ic_media_pause)
                startSeekbarUpdate()
            }
        }
    }

    private fun startSeekbarUpdate() {
        updateSeekbarJob?.cancel()
        updateSeekbarJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                while (isActive && mediaPlayer != null) {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val currentPosition = player.currentPosition
                            binding.audioPlaybackSlider.progress = currentPosition
                            updateAudioTimeDisplay(currentPosition.toLong(), player.duration.toLong())
                        }
                    }
                    delay(100) // Actualizar cada 100ms
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateAudioTimeDisplay(currentPosition: Long, duration: Long) {
        val currentFormatted = formatDuration(currentPosition)
        val durationFormatted = formatDuration(duration)

        binding.audioCurrentTime.text = currentFormatted
        binding.audioDuration.text = durationFormatted
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        binding.favoriteButton.setImageResource(
            if (isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    private fun showCategoryDialog(item: MediaItem) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar categoría")
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    categories
                )
            ) { _, which ->
                val selectedCategory = categories[which]
                viewModel.updateCategory(item, selectedCategory)
                Toast.makeText(
                    requireContext(),
                    "Categoría actualizada a $selectedCategory",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun shareMediaItem(item: MediaItem) {
        lifecycleScope.launch {
            try {
                val file = File(item.uri.path ?: "")
                if (!file.exists()) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró el archivo para compartir: ${file.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Log para depuración
                Log.d("GalleryFragment", "Attempting to share file: ${file.absolutePath}")

                // Usar FileProvider para obtener un URI compartible
                val authority = "${requireContext().packageName}.fileprovider"

                // Intenta usar ContentUri en lugar de FileProvider si es necesario
                val fileUri = try {
                    FileProvider.getUriForFile(
                        requireContext(),
                        authority,
                        file
                    )
                } catch (e: IllegalArgumentException) {
                    Log.e("GalleryFragment", "Error al crear URI con FileProvider: ${e.message}")

                    // Intenta usar el URI original como alternativa
                    Toast.makeText(
                        requireContext(),
                        "Error al compartir: No se puede acceder al archivo",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)

                    // Establecer tipo según el tipo de medio
                    type = when (item.type) {
                        MediaType.PHOTO -> "image/*"
                        MediaType.AUDIO -> "audio/*"
                    }

                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir con..."))
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("GalleryFragment", "Error al compartir: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error al compartir: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(item: MediaItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar medio")
            .setMessage("¿Estás seguro de que deseas eliminar este elemento? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteItem(item)
                    Toast.makeText(
                        requireContext(),
                        "Elemento eliminado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun releaseMediaPlayer() {
        withContext(Dispatchers.IO) {
            try {
                updateSeekbarJob?.cancel()
                updateSeekbarJob = null

                val player = mediaPlayer
                if (player != null) {
                    withContext(Dispatchers.Main) {
                        if (player.isPlaying) {
                            player.stop()
                        }
                        player.reset()
                        player.release()
                    }
                    mediaPlayer = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            releaseMediaPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            releaseMediaPlayer()

            // Forzar la limpieza de recursos
            if (detailViewVisible) {
                hideDetailView()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.launch {
            releaseMediaPlayer()

            // Forzar la limpieza de recursos en el contexto actual
            try {
                if (isAdded && context != null) {
                    Glide.with(requireContext()).clear(binding.photoDetailView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _binding = null
        }
    }
}