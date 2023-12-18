package com.i69.ui.screens.main.search.userProfile

import android.os.Bundle
import android.view.LayoutInflater

import android.view.ViewGroup
import com.i69.BuildConfig
import com.i69.data.models.Photo

import com.i69.databinding.ImagesliderItemBinding
import com.i69.ui.base.BaseFragment
import com.i69.utils.loadImage

class ImageFragment : BaseFragment<ImagesliderItemBinding>() {


    companion object {

        // Method for creating new instances of the fragment
        fun newInstance(imgdata: Photo): ImageFragment {

            // Store the movie data in a Bundle object
            val args = Bundle()
            args.putString("ImgId",imgdata.id)
            args.putString("ImgUrl",imgdata.url)


            // Create a new MovieFragment and set the Bundle as the arguments
            // to be retrieved and displayed when the view is created
            val fragment = ImageFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) = ImagesliderItemBinding.inflate(inflater, container, false).apply {

    }

    override fun setupTheme() {

        val args = arguments

        binding.imageView.loadImage(
            args!!.getString("ImgUrl")?.replace("${BuildConfig.BASE_URL_REP}media/","${BuildConfig.BASE_URL}media/").toString())


    }

    override fun setupClickListeners() {
    }
}