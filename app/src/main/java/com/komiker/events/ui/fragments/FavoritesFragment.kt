package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.komiker.events.R
import com.komiker.events.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                initButtonFavorite()
                initButtonNotification()

                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initButtonFavorite() {
        val buttonFavorite = view?.findViewById<ImageButton>(R.id.button_favorite)
        buttonFavorite?.post {
            val buttonWidth = buttonFavorite.width

            val padding = (buttonWidth * 0.1625).toInt()

            buttonFavorite.setPadding(padding, padding, padding, padding)

            buttonFavorite.visibility = View.VISIBLE

            buttonFavorite.setOnClickListener {

            }
        }
    }

    private fun initButtonNotification() {
        val buttonNotification = view?.findViewById<ImageButton>(R.id.button_notification)
        buttonNotification?.post {
            val buttonWidth = buttonNotification.width

            val padding = (buttonWidth * 0.1625).toInt()

            buttonNotification.setPadding(padding, padding, padding, padding)

            buttonNotification.visibility = View.VISIBLE

            buttonNotification.setOnClickListener {

            }
        }
    }
}