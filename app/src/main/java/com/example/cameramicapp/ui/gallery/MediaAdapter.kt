package com.example.cameramicapp.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cameramicapp.R
import com.example.cameramicapp.data.models.MediaItem
import com.example.cameramicapp.data.models.MediaType
import com.example.cameramicapp.databinding.ItemMediaBinding

class MediaAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MediaViewHolder(
        private val binding: ItemMediaBinding,
        private val onItemClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            // Configurar información básica
            binding.mediaName.text = item.filename

            // Mostrar icono de favorito si corresponde
            binding.favoriteIcon.visibility = if (item.favorite) View.VISIBLE else View.GONE

            // Configurar icono y vista previa según el tipo de medio
            when (item.type) {
                MediaType.PHOTO -> {
                    binding.mediaTypeIcon.setImageResource(android.R.drawable.ic_menu_camera)

                    // Cargar la imagen con Glide
                    Glide.with(binding.root.context)
                        .load(item.uri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(binding.mediaThumb)
                }
                MediaType.AUDIO -> {
                    binding.mediaTypeIcon.setImageResource(android.R.drawable.ic_btn_speak_now)

                    // Para audio, mostrar un placeholder
                    binding.mediaThumb.setImageResource(android.R.drawable.ic_media_play)
                    binding.mediaThumb.scaleType = android.widget.ImageView.ScaleType.CENTER
                    binding.mediaThumb.setBackgroundResource(R.color.ipn_primary_light)
                }
            }

            // Configurar clic en el item
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private class MediaDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
}