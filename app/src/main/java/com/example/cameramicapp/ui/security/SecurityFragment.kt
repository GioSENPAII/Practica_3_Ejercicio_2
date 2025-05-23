package com.example.cameramicapp.ui.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cameramicapp.databinding.FragmentSecurityBinding
import com.example.cameramicapp.sensors.AuthResult
import com.example.cameramicapp.sensors.BiometricAvailability
import com.example.cameramicapp.sensors.SensorHelper

class SecurityFragment : Fragment() {

    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SecurityViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            SecurityViewModelFactory(requireActivity())
        )[SecurityViewModel::class.java]

        setupUI()
        observeViewModel()
        checkBiometricAvailability()
    }

    private fun setupUI() {
        binding.authenticateButton.setOnClickListener {
            viewModel.authenticateUser()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }

        binding.testProximityButton.setOnClickListener {
            if (viewModel.isAuthenticated.value == true) {
                viewModel.toggleProximityTesting()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Primero debes autenticarte",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isAuthenticated.observe(viewLifecycleOwner) { isAuthenticated ->
            updateAuthenticationState(isAuthenticated)
        }

        viewModel.authenticationResult.observe(viewLifecycleOwner) { result ->
            handleAuthResult(result)
        }

        viewModel.proximityData.observe(viewLifecycleOwner) { proximityData ->
            if (viewModel.isAuthenticated.value == true && viewModel.isProximityTesting.value == true) {
                binding.proximityDistance.text = "Distancia: ${proximityData.distance} cm"
                binding.proximityStatus.text = if (proximityData.isNear) {
                    "🔴 Objeto cerca detectado"
                } else {
                    "🟢 Sin objetos cerca"
                }

                if (proximityData.isNear) {
                    binding.proximityCard.setCardBackgroundColor(
                        requireContext().getColor(android.R.color.holo_red_light)
                    )
                } else {
                    binding.proximityCard.setCardBackgroundColor(
                        requireContext().getColor(android.R.color.holo_green_light)
                    )
                }
            }
        }

        viewModel.isProximityTesting.observe(viewLifecycleOwner) { isTesting ->
            binding.testProximityButton.text = if (isTesting) {
                "Detener prueba de proximidad"
            } else {
                "Iniciar prueba de proximidad"
            }

            binding.proximityCard.visibility = if (isTesting) View.VISIBLE else View.GONE
        }
    }

    private fun checkBiometricAvailability() {
        val availability = viewModel.checkBiometricAvailability()
        val sensorHelper = SensorHelper(requireContext())

        binding.biometricStatus.text = sensorHelper.getBiometricInfo()
        binding.proximityInfo.text = sensorHelper.getProximitySensorInfo()

        val availableSensors = sensorHelper.getAllAvailableSensors()
        val sensorsText = availableSensors.joinToString("\n\n") { sensor ->
            "${if (sensor.isAvailable) "✅" else "❌"} ${sensor.name}\n${sensor.details}"
        }
        binding.sensorsInfo.text = sensorsText

        binding.authenticateButton.isEnabled = availability == BiometricAvailability.AVAILABLE
    }

    private fun updateAuthenticationState(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            binding.authenticationStatus.text = "✅ Autenticado"
            binding.authenticateButton.visibility = View.GONE
            binding.logoutButton.visibility = View.VISIBLE
            binding.secureContent.visibility = View.VISIBLE
            binding.testProximityButton.isEnabled = true
        } else {
            binding.authenticationStatus.text = "❌ No autenticado"
            binding.authenticateButton.visibility = View.VISIBLE
            binding.logoutButton.visibility = View.GONE
            binding.secureContent.visibility = View.GONE
            binding.testProximityButton.isEnabled = false
            binding.proximityCard.visibility = View.GONE
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        val message = when (result) {
            is AuthResult.Success -> result.message
            is AuthResult.Error -> result.message
            is AuthResult.Failed -> result.message
            is AuthResult.LoggedOut -> "Sesión cerrada"
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isProximityTesting.value == true) {
            viewModel.startProximityListening()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopProximityListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopAllSensors()
        _binding = null
    }
}