package com.i69.ui.screens.main.camera

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ImagePreviewAdapter(private val context: Context,
                          private var data: MutableList<Preview>,
                          private val onClick: (Int, String, Preview) -> Unit){
//    : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {
//
//    class ViewHolder(val binding: ItemCapturePreviewBinding) : RecyclerView.ViewHolder(binding.root)
//    private lateinit var binding: ItemCapturePreviewBinding
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        binding = ItemCapturePreviewBinding.inflate(LayoutInflater.from(context), parent, false)
//        return ViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = data[position]
//        binding.ivCapturePreview.setImageURI(item.imageUri)
//
//        if (item.isSelected) binding.ivSelected.visibility = View.VISIBLE
//        else binding.ivSelected.visibility = View.GONE
//
//        binding.ivCapturePreview.setOnClickListener {
//            onClick.invoke(position, "tap", item)
//        }
//
//        binding.ivCapturePreview.setOnLongClickListener {
//            onClick.invoke(position, "hold", item)
//            false
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return data.size
//    }
//
//    fun refreshItems(data: MutableList<Preview>){
//        this.data = data
//        notifyDataSetChanged()
//    }
//
//    fun refreshItem(position: Int, data: Preview){
//        this.data[position] = data
//        notifyItemChanged(position)
//    }
//
//    fun addItem(data: Preview){
//        this.data.add(data)
//        notifyDataSetChanged()
//    }
//
//    fun getData(): MutableList<Preview>{
//        return data
//    }
//
//    fun removeItem(position: Int){
//        data.removeAt(position)
//        notifyItemRemoved(position)
//    }
//
//    fun removeAllItem(){
//        val articleList: MutableList<Preview> = ArrayList()
//        this.data = articleList
//        notifyDataSetChanged()
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return position
//    }
}