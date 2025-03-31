package com.example.cameramicapp.ui.gallery

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
import java.io.File
import java.util.concurrent.TimeUnit

class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GalleryViewModel
    private lateinit var adapter: MediaAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var updateSeekbarJob: Job? = null

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
            hideDetailView()
        }

        binding.mediaDetailContainer.setOnClickListener {
            // Solo cerrar si se hace clic fuera de los controles
            if (it == binding.mediaDetailContainer) {
                hideDetailView()
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
            if (item != null) {
                showDetailView(item)
            } else {
                hideDetailView()
            }
        }
    }

    private fun showDetailView(item: MediaItem) {
        releaseMediaPlayer() // Liberar reproductor si estaba en uso

        when (item.type) {
            MediaType.PHOTO -> {
                binding.photoDetailView.visibility = View.VISIBLE
                binding.audioDetailView.visibility = View.GONE

                // Cargar la imagen con Glide
                Glide.with(this)
                    .load(item.uri)
                    .fitCenter()
                    .into(binding.photoDetailView)
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
    }

    private fun hideDetailView() {
        releaseMediaPlayer()
        binding.mediaDetailContainer.visibility = View.GONE
        viewModel.clearSelection()
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
            }
            // Continuación del código del GalleryFragment.kt
            // Cambiar icono a play
            binding.audioPlaybackIcon.setImageResource(android.R.drawable.ic_media_play)

            // Configurar listener para cuando termina la reproducción
            mediaPlayer?.setOnCompletionListener {
                binding.audioPlaybackSlider.progress = 0
                binding.audioPlaybackIcon.setImageResource(android.R.drawable.ic_media_play)
                updateAudioTimeDisplay(0, mediaPlayer?.duration?.toLong() ?: 0)
                updateSeekbarJob?.cancel()
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
            while (true) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPosition = player.currentPosition
                        binding.audioPlaybackSlider.progress = currentPosition
                        updateAudioTimeDisplay(currentPosition.toLong(), player.duration.toLong())
                    }
                }
                delay(100) // Actualizar cada 100ms
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
        try {
            val file = File(item.uri.path ?: "")
            if (!file.exists()) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el archivo para compartir",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val fileUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

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
            Toast.makeText(
                requireContext(),
                "Error al compartir: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDeleteConfirmationDialog(item: MediaItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar medio")
            .setMessage("¿Estás seguro de que deseas eliminar este elemento? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteItem(item)
                Toast.makeText(
                    requireContext(),
                    "Elemento eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun releaseMediaPlayer() {
        updateSeekbarJob?.cancel()
        updateSeekbarJob = null

        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseMediaPlayer()
        _binding = null
    }
}