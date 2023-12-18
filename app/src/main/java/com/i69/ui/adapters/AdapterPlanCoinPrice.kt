package com.i69.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.i69.GetAllPackagesQuery
import com.i69.R

class AdapterPlanCoinPrice(
    var context: Context,
    var coinPriceInterface: PlanCoinPriceInterface
) :
    RecyclerView.Adapter<AdapterPlanCoinPrice.CoinPriceViewHolder>() {

    private val itemList: ArrayList<GetAllPackagesQuery.Plan?> = ArrayList()
    private var selectedPlanId = ""


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinPriceViewHolder {
        return CoinPriceViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_purchase_plan_comper, parent, false)
        )
//        return CoinPriceViewHolder(
//            LayoutInflater.from(context).inflate(R.layout.item_purchase_plan, parent, false)
//        )
    }

    inner class CoinPriceViewHolder(iView: View) : RecyclerView.ViewHolder(iView) {
        val constarinetPurchasePlan =
            iView.findViewById<ConstraintLayout>(R.id.constarinet_purchase_plan)
        val numberOfCoins = iView.findViewById<TextView>(R.id.numberOfCoins)
        val price = iView.findViewById<TextView>(R.id.price)
        val txtPersentag = iView.findViewById<TextView>(R.id.txt_persentag)
        val description = iView.findViewById<TextView>(R.id.description)

        //        val salePrice = iView.findViewById<TextView>(R.id.salePrice)
//        val priceGoldCircle = iView.findViewById<ImageView>(R.id.priceGoldCircle)
        val subScribePlan = iView.findViewById<LinearLayout>(R.id.subScribePlan)
        val llComparePrice = iView.findViewById<LinearLayout>(R.id.llComparePrice)

        init {
//            itemView.setOnClickListener {
//                val coinPrice = itemList?.get(adapterPosition)
//                coinPriceInterface.onClick(adapterPosition, coinPrice)
//            }

            subScribePlan.setOnClickListener {
                val coinPrice = itemList?.get(adapterPosition)
                coinPriceInterface.onClick(adapterPosition, coinPrice)
            }
        }
    }

    override fun onBindViewHolder(holder: CoinPriceViewHolder, position: Int) {
        val coinPrice = itemList?.get(holder.adapterPosition)
        val showDiscount = true
        holder.numberOfCoins.text = coinPrice?.title
        holder.description.text = coinPrice?.`package`!!.name
        if (coinPrice.isOnDiscount) {
            holder.txtPersentag.visibility = View.VISIBLE
            holder.txtPersentag.text = "${coinPrice.dicountedPriceInCoins}"
            Log.e("MyCoinPriseDiscounts", "${coinPrice.dicountedPriceInCoins}")
        } else {
            holder.txtPersentag.visibility = View.GONE
        }

//        holder.priceGoldCircle.setVisibleOrInvisible(showDiscount)
        val value = coinPrice?.priceInCoins?.toFloat()

//        val valueTruncated = value?.toInt()
//        val value2 = valueTruncated?.toFloat()
//        val value3 = value2?.let { value?.minus(it) }
//        val s = String.format("%.2f", value3)

        holder.price.text = value.toString()

//        holder.description.text = s.substring(1, s.length)


        if (selectedPlanId == "1") {
            holder.llComparePrice.setBackground(context.resources.getDrawable(R.drawable.linyer_comperprice_subcribe))
            holder.constarinetPurchasePlan.setBackground(context.resources.getDrawable(R.drawable.ic_silver_plane_drawable))
        } else if (selectedPlanId == "2") {
            holder.llComparePrice.setBackground(context.resources.getDrawable(R.drawable.linear_comper_price_silver))
            holder.constarinetPurchasePlan.setBackground(context.resources.getDrawable(R.drawable.ic_gold_plane_drawable))
        } else if (selectedPlanId == "3") {
            holder.llComparePrice.setBackground(context.resources.getDrawable(R.drawable.linyer_comperprice_subcribe))
            holder.constarinetPurchasePlan.setBackground(context.resources.getDrawable(R.drawable.ic_platinum_plane_drawable))
        }

        /*if (selectedPlanName.contains(AppStringConstant1.silver_package, true)) {
            holder.llComparePrice.setBackground(context.resources.getDrawable(R.drawable.linyer_comperprice_subcribe))
            holder.constarinetPurchasePlan.setBackground(context.resources.getDrawable(R.drawable.ic_silver_plane_drawable))
        } else if (selectedPlanName.contains(AppStringConstant1.gold_package, true)) {
            holder.llComparePrice.setBackground(context.resources.getDrawable(R.drawable.linear_comper_price_silver))
            holder.constarinetPurchasePlan.setBackground(context.resources.getDrawable(R.drawable.ic_gold_plane_drawable))
        } else if (selectedPlanName.contains(AppStringConstant1.platimum_package, true)) {
            holder.llComparePrice.setBackground(context.resources.getDrawable(R.drawable.linyer_comperprice_subcribe))
            holder.constarinetPurchasePlan.setBackground(context.resources.getDrawable(R.drawable.ic_platinum_plane_drawable))
        }*/

//        if(coinPrice?.title!!.contains("Year", true)){
//            holder. constarinetPurchasePlan.setBackgroundColor(context.resources.getColor(R.color.platinum_plan))
//        }else if(coinPrice.title.contains("6 Month", true)){
//            holder. constarinetPurchasePlan.setBackgroundColor(context.resources.getColor(R.color.gold_plan))
//        }else if(coinPrice.title.contains("3 Month", true)){
//            holder. constarinetPurchasePlan.setBackgroundColor(context.resources.getColor(R.color.month_plan))
//        }else if(coinPrice.title.contains("Month", true)){
//            holder. constarinetPurchasePlan.setBackgroundColor(context.resources.getColor(R.color.silver_plan))
//        }else{
//            holder. constarinetPurchasePlan.setBackgroundColor(context.resources.getColor(R.color.C053440))
//        }


    }

    override fun getItemCount(): Int {
        return itemList?.size!!
    }

    fun updateItemList(currentPlans: List<GetAllPackagesQuery.Plan?>?, selectedPlanId:String) {
        this.selectedPlanId = selectedPlanId
        itemList.clear()
        if (currentPlans != null) {
            itemList.addAll(currentPlans)
//            itemList.addAll(currentPlans)
        }
        notifyDataSetChanged()

    }

    interface PlanCoinPriceInterface {
        fun onClick(index: Int, coinPrice: GetAllPackagesQuery.Plan?)
    }
}
