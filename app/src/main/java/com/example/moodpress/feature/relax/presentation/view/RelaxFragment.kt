package com.example.moodpress.feature.relax.presentation.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentRelaxBinding
import com.example.moodpress.feature.relax.domain.model.RelaxSound
import com.example.moodpress.feature.relax.presentation.adapter.RelaxSoundAdapter
import com.example.moodpress.feature.relax.presentation.viewmodel.RelaxViewModel
import com.example.moodpress.feature.relax.service.SoundMixerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RelaxFragment : Fragment(R.layout.fragment_relax) {

    private val viewModel: RelaxViewModel by viewModels()
    private var binding: FragmentRelaxBinding? = null
    private var soundService: SoundMixerService? = null
    private var isBound = false
    private lateinit var adapter: RelaxSoundAdapter

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SoundMixerService.LocalBinder
            soundService = binder.getService()
            isBound = true
            syncUIWithService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRelaxBinding.bind(view)

        setupRecyclerView()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), SoundMixerService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
    }

    private fun setupRecyclerView() {
        adapter = RelaxSoundAdapter { sound ->
            if (isBound) {
                soundService?.toggleSound(sound)
                adapter.updateItem(sound.id, sound.isPlaying)
            }
        }
        binding?.rvRelaxSounds?.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sounds.collectLatest { list ->
                adapter.submitList(list)
                syncUIWithService()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun syncUIWithService() {
        if (soundService == null) return

        val isPlayingAny = soundService!!.activePlayers.isNotEmpty()
        val isPaused = soundService!!.isPaused

        val currentList = viewModel.sounds.value
        currentList.forEach { sound ->
            if (!isPlayingAny) {
                sound.isPlaying = false
            } else {
                sound.isPlaying = soundService!!.isPlaying(sound.audioUrl)
            }
        }
        adapter.submitList(currentList)
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}