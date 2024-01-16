package com.i69.ui.adapters

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.i69.applocalization.AppStringConstant
import com.i69.data.remote.responses.CoinPrice
import com.i69.databinding.ItemPurchaseCoinsNewBinding

class AdapterCoinPrice(
    var context: Context,
    var itemList: MutableList<CoinPrice>,
    var appStringConst : AppStringConstant,
    var coinPriceInterface: CoinPriceInterface
) :
    RecyclerView.Adapter<AdapterCoinPrice.CoinPriceViewHolder>() {


    inner class CoinPriceViewHolder(val viewBinding: ItemPurchaseCoinsNewBinding) :
        RecyclerView.ViewHolder(viewBinding.root){
//}

//    inner class CoinPriceViewHolder(iView: View) : RecyclerView.ViewHolder(iView) {
//        val numberOfCoins = iView.findViewById<TextView>(R.id.numberOfCoins)
//        val price = iView.findViewById<TextView>(R.id.price)
//        val priceSmall = iView.findViewById<TextView>(R.id.priceSmall)
//        val salePrice = iView.findViewById<TextView>(R.id.salePrice)
//        val priceGoldCircle = iView.findViewById<ImageView>(R.id.priceGoldCircle)

        val numberOfCoins = viewBinding.numberOfCoins
        val price = viewBinding.price
        val priceSmall =  viewBinding.priceSmall
        val salePrice =  viewBinding.salePrice
        //val priceGoldCircle =  viewBinding.priceGoldCircle
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ):  CoinPriceViewHolder =
        CoinPriceViewHolder(ItemPurchaseCoinsNewBinding.inflate(LayoutInflater.from(context), parent, false).apply {
                stringConstant = appStringConst
            })

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinPriceViewHolder {
//        return CoinPriceViewHolder(
//            LayoutInflater.from(context).inflate(R.layout.item_purchase_coins, parent, false).apply {
//                stringConstant = AppStringConstant(context)
//            }
//        )
//    }

    override fun onBindViewHolder(holder: CoinPriceViewHolder, position: Int) {
        val coinPrice = itemList[holder.bindingAdapterPosition]
        //val showDiscount = true
        holder.numberOfCoins.text = coinPrice.coinsCount
        //holder.priceGoldCircle.setVisibleOrInvisible(showDiscount)
        val value = coinPrice.discountedPrice.toFloat()
        val valueTruncated = value.toInt()
        val value2 = valueTruncated.toFloat()
        val value3 = value - value2
        val s = String.format("%.2f", value3)
        holder.price.text = "€$valueTruncated"
        holder.priceSmall.text = s.substring(1, s.length)

        holder.itemView.setOnClickListener {
            val coinPrice = itemList[holder.bindingAdapterPosition]
            coinPriceInterface.onClick(holder.bindingAdapterPosition, coinPrice)
        }
        /*if (!showDiscount) {
            holder.price.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
            holder.priceSmall.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
        }*/
        holder.salePrice.text = Html.fromHtml("<strike>€${coinPrice.originalPrice}</strike>")
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    interface CoinPriceInterface {
        fun onClick(index: Int, coinPrice: CoinPrice)
    }
}