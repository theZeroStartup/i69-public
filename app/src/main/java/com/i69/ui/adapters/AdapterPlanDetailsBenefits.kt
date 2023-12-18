package com.i69.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.i69.GetAllPackagesQuery
import com.i69.R
import com.i69.data.models.PlanBnefits

class AdapterPlanDetailsBenefits(
    var context: Context
) :
    RecyclerView.Adapter<AdapterPlanDetailsBenefits.CoinPriceViewHolder>() {

    private val itemList: ArrayList<PlanBnefits> = ArrayList()

    //    private var selectedPackageId = ""
    private var selectedPlanName = ""

    inner class CoinPriceViewHolder(iView: View) : RecyclerView.ViewHolder(iView) {
        val txtName = iView.findViewById<MaterialTextView>(R.id.purchasePackageTitle)

        val imgPlatnium = iView.findViewById<ImageView>(R.id.imgPlatnium)
        val imgGold = iView.findViewById<ImageView>(R.id.imgGold)
        val imgSilver = iView.findViewById<ImageView>(R.id.imgSilver)

        init {
//            itemView.setOnClickListener {
//                val coinPrice = itemList?.get(adapterPosition)
//
//            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinPriceViewHolder {
        return CoinPriceViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_plan_details_benefits, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CoinPriceViewHolder, position: Int) {
        val coinPrice = itemList.get(holder.adapterPosition)
        holder.txtName.text = coinPrice?.name

//        coinPrice.

        if (coinPrice.isPlatnium) {
//if(selectedPackageId.equals(coinPrice.selectedPackageId)){
//            if (selectedPlanName.contains("platimum", true)) {
//            if (selectedPlanName.contains(context.getString(R.string.platnium), true)) {
//
//                holder.imgPlatnium.setColorFilter(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.colorNavigationBackground
//                    )
//                )
//            }
            holder.imgPlatnium.setImageResource(R.drawable.check_mark_right_round)
//            holder.imgPlatnium.visibility = View.VISIBLE
        } else {
            holder.imgPlatnium.setImageResource(R.drawable.delete_round)
//            holder.imgPlatnium.visibility = View.GONE
        }

        if (coinPrice?.isGold) {

//            if(selectedPackageId.equals(coinPrice.selectedPackageId)){
//            if (selectedPlanName.contains("gold", true)) {
//            if (selectedPlanName.contains(context.getString(R.string.gold), true)) {
//
//                holder.imgGold.setColorFilter(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.colorNavigationBackground
//                    )
//                )
//            }


//            holder.imgGold.visibility = View.VISIBLE
            holder.imgGold.setImageResource(R.drawable.check_mark_right_round)

        } else {
//            holder.imgGold.visibility = View.GONE
            holder.imgGold.setImageResource(R.drawable.delete_round)
        }

        if (coinPrice?.isSilver) {

//            if(selectedPackageId.equals(coinPrice?.selectedPackageId)){
//            if (selectedPlanName.contains("silver", true)) {
//            if (selectedPlanName.contains(context.getString(R.string.silver), true)) {
//
//                holder.imgSilver.setColorFilter(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.colorNavigationBackground
//                    )
//                )
//            }

//            holder.imgSilver.visibility = View.VISIBLE
            holder.imgSilver.setImageResource(R.drawable.check_mark_right_round)
        } else {
//            holder.imgSilver.visibility = View.GONE
            holder.imgSilver.setImageResource(R.drawable.delete_round)
        }
    }

    override fun getItemCount(): Int {
        return itemList?.size!!
    }

    fun updateItemList(currentPlans: List<PlanBnefits>, selectedPlanName: String) {
        this.selectedPlanName = selectedPlanName
        itemList.clear()
        if (currentPlans != null) {
            itemList.addAll(currentPlans)
        }
        notifyDataSetChanged()

    }

    interface PlanCoinPriceInterface {
        fun onClick(index: Int, coinPrice: GetAllPackagesQuery.Plan?)
    }
}