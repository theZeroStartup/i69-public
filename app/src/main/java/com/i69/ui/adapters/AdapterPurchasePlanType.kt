package com.i69.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.i69.GetAllPackagesQuery
import com.i69.R
import com.i69.databinding.ListItemPlanNameBinding


//private val listener: AdapterPurchasePlanType.ClickonListener,
class AdapterPurchasePlanType(
    private val ctx: Context,

    private var allusermoments: List<GetAllPackagesQuery.AllPackage?>,
    var planInterface: AdapterPurchasePlanType.PlanInterface
) : RecyclerView.Adapter<AdapterPurchasePlanType.ViewHolder>() {
    override fun onBindViewHolder(holder: AdapterPurchasePlanType.ViewHolder, position: Int) {

        val item = allusermoments?.get(position)
        holder.bind(position, item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterPurchasePlanType.ViewHolder =
        ViewHolder(
            ListItemPlanNameBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )


    override fun getItemCount(): Int {
        return if (allusermoments == null) 0 else allusermoments?.size!!
    }


    inner class ViewHolder(val viewBinding: ListItemPlanNameBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(position: Int, item: GetAllPackagesQuery.AllPackage?) {

            viewBinding.planTitle.setText(item!!.name)
            if (item!!.id == "1") {
                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.silver_plan))
            } else if(item!!.id == "2") {
                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.gold_plan))
            } else if(item!!.id == "3") {
                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.platinum_plan))
            }
            /*if(item!!.name.contains(AppStringConstant1.silver_package, true)) {
                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.silver_plan))
            }else  if(item!!.name.contains(AppStringConstant1.gold_package, true)){
                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.gold_plan))
            }else  if(item!!.name.contains(AppStringConstant1.platimum_package, true)){
                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.platinum_plan))
            }*/
            viewBinding.cvPlanType.setOnClickListener(View.OnClickListener {
                planInterface.onClick(bindingAdapterPosition, item)

            })
//            if(item!!.name.contains("silver", true)) {
//                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.silver_plan))
//            }else  if(item!!.name.contains("gold", true)){
//                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.gold_plan))
//            }else  if(item!!.name.contains("platimum", true)){
//                viewBinding.clPlanType.setBackgroundColor(ctx.resources.getColor(R.color.platinum_plan))
//            }
//            viewBinding.cvPlanType.setOnClickListener(View.OnClickListener {
//                planInterface.onClick(bindingAdapterPosition, item)
//
//            })
        }

    }

    interface PlanInterface {
        fun onClick(index: Int, coinPrice: GetAllPackagesQuery.AllPackage?)
    }
}
