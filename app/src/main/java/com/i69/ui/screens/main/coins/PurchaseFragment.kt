package com.i69.ui.screens.main.coins

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wallet.PaymentData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.i69.*
import com.i69.R
import com.i69.R.id.ed_pin
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.billing.BillingDataSource
import com.i69.billing.BillingDataSource.Companion.IS_Payment_Done
import com.i69.billing.BillingDataSource.Companion.paypalCaptureId
import com.i69.data.remote.requests.PurchaseRequest
import com.i69.data.remote.responses.CoinPrice
import com.i69.databinding.FragmentPurchaseNewBinding
import com.i69.singleton.App
import com.i69.ui.adapters.AdapterCoinPrice
import com.i69.ui.adapters.OperatorsAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.viewModels.PurchaseCoinsViewModel
import com.i69.utils.*
import com.paypal.checkout.paymentbutton.PayPalButton
import com.stripe.android.*
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.view.CardInputWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class PurchaseFragment : BaseFragment<FragmentPurchaseNewBinding>() {

    val PAYPAL_REQUEST_CODE = 123
    private val viewModel: PurchaseCoinsViewModel by activityViewModels()
    private var selectedSku: Int = -1
    private var selectedCoins: Int = 0
    private var selectedPrice: Double = 0.0
    private lateinit var dialog: Dialog
    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    var paymentMeth = ""
    var latitude = ""
    var longtitude = ""
    private var coinPriceList = mutableListOf<CoinPrice>()
    private lateinit var adapterCoinPrice: AdapterCoinPrice
    var amount = 0.0
    var currentCurrency = ""
    lateinit var sharedPref: SharedPref
    lateinit var stripeBottomsheetDialog: BottomSheetDialog
    lateinit var paymentBottomsheetDialog: BottomSheetDialog

    lateinit var googlePay: MaterialCardView
    lateinit var google: MaterialCardView
    lateinit var boku: MaterialCardView
    lateinit var stripePay: MaterialCardView
    lateinit var payapal: MaterialCardView
//    lateinit var paypalButton: PaymentButtonContainer
    lateinit var paypalButton: PayPalButton

    lateinit var googlePayRadioButton: RadioButton
    lateinit var googleRadioButton: RadioButton
    lateinit var bokuRadioButton: RadioButton
    lateinit var stripeRadioButton: RadioButton
    lateinit var payapalRadioButton: RadioButton
    var paypalCreateOrderId = ""


//    lateinit var    paymentLauncher :PaymentLauncher

    private var stripe: Stripe? = null
    private var paymentMethod = mutableListOf<GetPaymentMethodsQuery.GetPaymentMethod?>()

//    private  var paymentMethodId:String?=null
//    private  var paymentSession: PaymentSession?=null

    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    var appStringConst: AppStringConstant? = null

    override fun getStatusBarColor() = R.color.colorPrimary
    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPurchaseNewBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }


    override fun setupTheme() {
        navController = findNavController()

        viewStringConstModel.data.observe(this@PurchaseFragment) { data ->
            if (data != null) {
                binding.stringConstant = data
                Log.e("MyCoinsValueSetted", data.buy_coins_purchase)
                appStringConst = data
            }
        }
        viewStringConstModel.data.also {
            if (it.value != null) {
                binding.stringConstant = it.value
                appStringConst = it.value
            }
    //            Log.e("MydataBasesss", it.value!!.messages)
        }
        sharedPref = SharedPref(requireContext())
     //    setupDialog()
        /*showPrice(1, binding.firstPrice30, 30, 1.99, "1", ".99", "3.99")
        showPrice(2, binding.firstPrice, 100, 4.99, "4", ".99", "8.99")
        showPrice(3, binding.secondPrice, 250, 9.99, "9", ".99", "12.99")
        showPrice(4, binding.thirdPrice, 500, 14.99, "14", ".99", "19.99")
        showPrice(5, binding.fourthPrice, 1150, 24.99, "24", ".99", "34.99")
        showPrice(6, binding.fifthPrice, 2550, 49.99, "49", ".99", "66.99")
        showPrice(7, binding.sixthPrice, 5600, 99.99, "99", ".99", "149.99")*/
        /*lifecycleScope.launchWhenResumed {
            LogUtil.debug("Here")
            val res = try {
                apolloClient(requireContext(), getCurrentUserToken()!!).query(GetCoinPricesQuery())
                    .execute()
            } catch (e: ApolloException) {
                LogUtil.debug("Here 1  ${e.message}")
                LogUtil.debug("Here 1  ${e.localizedMessage}")
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception to get room ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }
            val allCoins = res.data?.allCoinPrices
            LogUtil.debug("Here size  : : ${allCoins?.data?.edges?.size}")
            allCoins?.data?.edges?.forEach {
                LogUtil.debug("CoinPrice : : ${it?.node?.coinsCount}")
            }
        }*/
        getStripePublishableKey()

        getPaymentMethods()
        OnPaymentMethodChange()

        lifecycleScope.launch(Dispatchers.Main) {
            val token = getCurrentUserToken()!!
//          viewModel.getCoinPrices(token).observe(viewLifecycleOwner) {
//                it?.let { coinSettings ->
//                    coinPriceList.addAll(coinSettings)
//                    adapterCoinPrice.notifyDataSetChanged()
//                    coinSettings.forEach { coinSetting ->
//                        LogUtil.debug("Here ${coinSetting.coinsCount}")
//                    }
//                }
//            }

            viewModel.getCoinPrices(token) { error ->
                if (error == null) {
                    activity?.runOnUiThread {
                        coinPriceList.addAll(viewModel.coinPrice)
                        if (::adapterCoinPrice.isInitialized) {
                            adapterCoinPrice.notifyDataSetChanged()
                        }
                    }

                } else {

                }
            }

        }

        appStringConst?.let {
            adapterCoinPrice = AdapterCoinPrice(
                requireContext(),
                coinPriceList,
                it,
                object : AdapterCoinPrice.CoinPriceInterface {
                    override fun onClick(index: Int, coinPrice: CoinPrice) {
                        Log.e("paymentCurrency", "${coinPrice.currency} ${coinPrice.discountedPrice.toDouble()}" +
                                "${coinPrice.coinsCount.toInt()}")
                        amount = coinPrice.discountedPrice.toDouble()
                        currentCurrency = coinPrice.currency
                        showBottomSheetForPaymentOptions(
                            index + 1,
                            coinPrice.coinsCount.toInt(),
                            coinPrice.discountedPrice.toDouble(),
                            coinPrice.currency
                        )
                    }
                })
            binding.recyclerViewCoins.adapter = adapterCoinPrice
            viewModel.consumedPurchase.observe(viewLifecycleOwner) { isPurchased ->
                isPurchased?.let {
                    Timber.d("Observing Purchased  $it")
                    showProgressView()
                    Log.e("time_1", "onPaymentSuccess")
                    if (it == 0) onPaymentSuccess("in-app-purchase", "$it")
                }
            }
        }
    }

//    fun setPaymentLauncherForStripe(paymentLaunch: PaymentLauncher) {
//        paymentLauncher = paymentLaunch
//
//    }


    fun OnPaymentMethodChange() {

        Log.e("OnPaymentMethodChange", "OnPaymentMethodChange")
//        onPaymentSuccess()
        lifecycleScope.launch(Dispatchers.IO) {
            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClientSubscription(requireContext(), userToken).subscription(
                    OnPaymentMethodChangeSubscription()
                ).execute()
            } catch (e: ApolloException) {
                Log.e("Exception paymentChange", "${e.message}")

                Timber.d("apolloResponse ${e.message}")

                binding.root.snackbar("${getString(R.string.exception_get_payment_methods)}${e.message}")
                hideProgressView()
                return@launch
            }

            if (response.hasErrors()) {
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorgetPaymentMethods", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {

                var i = 0
                if (paymentMethod.isNotEmpty()) {

                    for (avc in paymentMethod) {

                        i++
                        if (avc!!.paymentMethod.contentEquals(response.data!!.onPaymentMethodChange!!.paymentMethod)) {

//                            avc.isAllowed = response.data!!.onPaymentMethodChange!!.isAllowed!!
//                            if (avc.isAllowed) {
//                                googlePay.visibility = View.VISIBLE
//                            } else {
//                                googlePay.visibility = View.GONE
//                            }
                            break;
                        }
                    }
                }
//                else{

                paymentMethod.removeAt(i)
                var paymentData =
                    GetPaymentMethodsQuery.GetPaymentMethod(
                        isAllowed = response.data!!.onPaymentMethodChange!!.isAllowed!!,
                        paymentMethod = response.data!!.onPaymentMethodChange!!.paymentMethod!!
                    )

                paymentMethod.add(i, paymentData)
//                }

                if (::paymentBottomsheetDialog.isInitialized && paymentBottomsheetDialog.isShowing) {

                    for (avc in paymentMethod) {

                        if (avc!!.paymentMethod.contentEquals("Gpay")) {
                            if (avc.isAllowed) {
                                googlePay.visibility = View.VISIBLE
                            } else {
                                googlePay.visibility = View.GONE
                            }

                        } else if (avc.paymentMethod.contentEquals("Paypal")) {
                            if (avc.isAllowed) {
                                payapal.visibility = View.VISIBLE
                            } else {
                                payapal.visibility = View.GONE
                            }
                        } else if (avc.paymentMethod.contentEquals("Stripe")) {
                            if (avc.isAllowed) {
                                stripePay.visibility = View.VISIBLE
                            } else {
                                stripePay.visibility = View.GONE
                            }
                        } else if (avc.paymentMethod.contentEquals("Boku")) {
                            if (avc.isAllowed) {
                                boku.visibility = View.VISIBLE
                            } else {
                                boku.visibility = View.GONE
                            }
                        }
                    }

                    if (googlePay.visibility == View.GONE) {
                        if (boku.visibility == View.GONE) {
                            if (stripePay.visibility == View.GONE) {
                                if (payapal.visibility == View.GONE) {
                                   googlePayRadioButton.isChecked = false
                                    googleRadioButton.isChecked = false
                                    googleRadioButton.isChecked = false
                                    bokuRadioButton.isChecked = false
                                    stripeRadioButton.isChecked = false
                                    payapalRadioButton.isChecked = false
                                } else {
                                   googlePayRadioButton.isChecked = false
                                    googleRadioButton.isChecked = false
                                    bokuRadioButton.isChecked = false
                                    stripeRadioButton.isChecked = false
                                    payapalRadioButton.isChecked = true
                                }
                            } else {
                               googlePayRadioButton.isChecked = false
                                    googleRadioButton.isChecked = false
                                bokuRadioButton.isChecked = false
                                stripeRadioButton.isChecked = true
                                payapalRadioButton.isChecked = false
                            }
                        } else {
                           googlePayRadioButton.isChecked = false
                                    googleRadioButton.isChecked = false
                            bokuRadioButton.isChecked = true
                            stripeRadioButton.isChecked = false
                            payapalRadioButton.isChecked = false
                        }
                    } else {
                        googlePayRadioButton.isChecked = true
                        bokuRadioButton.isChecked = false
                        stripeRadioButton.isChecked = false
                        payapalRadioButton.isChecked = false
                    }

                }
//                paymentMethod =response.data!!.getPaymentMethods
                Log.e("onPaymentUpdate", "${Gson().toJson(response)}")

            }
        }
    }


    fun getPaymentMethods() {

        lifecycleScope.launchWhenResumed {
            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken).query(
                    GetPaymentMethodsQuery()
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("${getString(R.string.exception_get_payment_methods)}${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            if (response.hasErrors()) {
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorgetPaymentMethods", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {

                response.data!!.getPaymentMethods?.let { paymentMethod.addAll(it) }
//                paymentMethod = response.data!!.getPaymentMethods as MutableList<GetPaymentMethodsQuery.GetPaymentMethod?>
                Log.e("getPaymentMethodsRespns", "${Gson().toJson(response)}")

            }
        }
    }

    fun getStripePublishableKey() {

        lifecycleScope.launchWhenResumed {

            val userToken = getCurrentUserToken()!!
            val response = try {
                apolloClient(requireContext(), userToken).query(
                    StripePublishableKeyQuery()

                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
//                binding.root.snackbar("Exception Stripe Publish key${e.message}")
                return@launchWhenResumed
            }finally {
                hideProgressView()
            }


            if (response.hasErrors()) {
                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorInstripePublishkey", "$errorMessage")

                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {


                App.initStripe(
                    response.data!!.stripePublishableKey!!.publishableKey!!,
                    this@PurchaseFragment
                )



                Log.e("getStripePublishKey", Gson().toJson(response))

//
                lifecycleScope.launch(Dispatchers.Main) {

                    stripe = Stripe(
                        requireContext(),
                        PaymentConfiguration.getInstance(requireContext()).publishableKey
                    )
//                        val paymentConfiguration = PaymentConfiguration.getInstance(requireContext())
//
//                      PaymentConfiguration.init(requireContext(),paymentConfiguration.publishableKey, paymentConfiguration.stripeAccountId )

                    MainActivity.getMainActivity()!!.setStripePayMentIntent(stripe!!)
                }
            }
        }
    }

    private fun statusCheck() {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGPS()
        }
    }

    private fun buildAlertMessageNoGPS() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setMessage("Your GPS seems to be disabled, do you want to enable it ?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton(
                "No"
            ) { p0, _ -> p0?.dismiss() }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun setupDialog() {
        dialog = Dialog(requireContext())
        with(dialog) {
            setContentView(R.layout.dialog_payment_options)
            setCancelable(true)
            window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
       /* dialog.findViewById<ImageButton>(R.id.google_pay).setOnClickListener {
            dialog.dismiss()
            val product = getSkuProductById(selectedSku)
            makePurchase(product)
        }*/
    }

    @SuppressLint("MissingInflatedId")
    private fun showBottomSheetForPaymentOptions(
        sku: Int,
        coins: Int,
        buyingPrice: Double,
        coinCurrency: String
    ) {
        paymentBottomsheetDialog = BottomSheetDialog(requireContext())
        val customView = layoutInflater.inflate(R.layout.dialog_payment_options_new, null, false)
        googlePay = customView.findViewById<MaterialCardView>(R.id.cd_googlepay)
        google = customView.findViewById<MaterialCardView>(R.id.cd_google)
        boku = customView.findViewById<MaterialCardView>(R.id.cd_boku)
        stripePay = customView.findViewById<MaterialCardView>(R.id.cd_stripe)
        payapal = customView.findViewById<MaterialCardView>(R.id.cd_paypal)
//        paypalButton = customView.findViewById<PayPalButton>(R.id.paypalButtonn)

        googlePayRadioButton = customView.findViewById<RadioButton>(R.id.rb_gpay)
        googleRadioButton = customView.findViewById<RadioButton>(R.id.rb_gOOGLE)
        bokuRadioButton = customView.findViewById<RadioButton>(R.id.rb_boku)
        stripeRadioButton = customView.findViewById<RadioButton>(R.id.rb_stripe)
        payapalRadioButton = customView.findViewById<RadioButton>(R.id.rb_paypal)


        val proceedToPayment = customView.findViewById<MaterialCardView>(R.id.cd_proceedtopayment)


        for (avc in paymentMethod!!) {

            if (avc!!.paymentMethod.contentEquals("Gpay")) {
                if (avc.isAllowed) {
                    googlePay.visibility = View.VISIBLE
                } else {
                    googlePay.visibility = View.GONE
                }

            } else if (avc.paymentMethod.contentEquals("Paypal")) {
                if (avc.isAllowed) {
                    payapal.visibility = View.VISIBLE
                } else {
                    payapal.visibility = View.GONE
                }
            } else if (avc.paymentMethod.contentEquals("Stripe")) {
                if (avc.isAllowed) {
                    stripePay.visibility = View.VISIBLE
                } else {
                    stripePay.visibility = View.GONE
                }
            } else if (avc.paymentMethod.contentEquals("Boku")) {
                if (avc.isAllowed) {
                    boku.visibility = View.VISIBLE
                } else {
                    boku.visibility = View.GONE
                }
            }
        }

        if (googlePay.visibility == View.GONE) {
            if (boku.visibility == View.GONE) {
                if (stripePay.visibility == View.GONE) {
                    if (payapal.visibility == View.GONE) {
                       googlePayRadioButton.isChecked = false
                        googleRadioButton.isChecked = false
                        bokuRadioButton.isChecked = false
                        stripeRadioButton.isChecked = false
                        payapalRadioButton.isChecked = false
                    } else {
                       googlePayRadioButton.isChecked = false
                        googleRadioButton.isChecked = false
                        bokuRadioButton.isChecked = false
                        stripeRadioButton.isChecked = false
                        payapalRadioButton.isChecked = true
                    }
                } else {
                    googlePayRadioButton.isChecked = false
                    googleRadioButton.isChecked = false
                    bokuRadioButton.isChecked = false
                    stripeRadioButton.isChecked = true
                    payapalRadioButton.isChecked = false
                }
            } else {
                googlePayRadioButton.isChecked = false
                googleRadioButton.isChecked = false
                bokuRadioButton.isChecked = true
                stripeRadioButton.isChecked = false
                payapalRadioButton.isChecked = false
            }
        } else {
            googlePayRadioButton.isChecked = true
            bokuRadioButton.isChecked = false
            stripeRadioButton.isChecked = false
            payapalRadioButton.isChecked = false
        }

            googlePay.setOnClickListener {
            googlePayRadioButton.isChecked = true
            googleRadioButton.isChecked = false
            bokuRadioButton.isChecked = false
            stripeRadioButton.isChecked = false
            payapalRadioButton.isChecked = false
        }
        google.setOnClickListener {
            googleRadioButton.isChecked = true
            googlePayRadioButton.isChecked = false
            bokuRadioButton.isChecked = false
            stripeRadioButton.isChecked = false
            payapalRadioButton.isChecked = false
        }

        boku.setOnClickListener {
           googlePayRadioButton.isChecked = false
            googleRadioButton.isChecked = false
            bokuRadioButton.isChecked = true
            stripeRadioButton.isChecked = false
            payapalRadioButton.isChecked = false
        }

        stripePay.setOnClickListener {
           googlePayRadioButton.isChecked = false
            googleRadioButton.isChecked = false
            bokuRadioButton.isChecked = false
            stripeRadioButton.isChecked = true
            payapalRadioButton.isChecked = false
        }

        payapal.setOnClickListener {
            googlePayRadioButton.isChecked = false
            googleRadioButton.isChecked = false
            bokuRadioButton.isChecked = false
            stripeRadioButton.isChecked = false
            payapalRadioButton.isChecked = true
        }



//        paypalButton.setup(
//            createOrder =
//            CreateOrder { createOrderActions ->0
//                val order =
//                    Order(
//
//                        intent = OrderIntent.CAPTURE,
//                        appContext = AppContext(userAction = UserAction.PAY_NOW),
//
//                        purchaseUnitList = listOf(
//                            PurchaseUnit(
//                                amount =
//                                Amount(currencyCode = CurrencyCode.USD, value = amount.toString())
//                            )
//                        )
//                    )
//
//                paypalcreateOrder(amount, currentCurrency)
//                createOrderActions.create(order)
//            },
//
//            onApprove = OnApprove { approval ->
//                approval.orderActions.capture { captureOrderResult ->
//                    paymentBottomsheetDialog.dismiss()
//
//                    Log.e("CaptureOrder", "CaptureOrderResult: $captureOrderResult")
////                    Toast.makeText(
////                        requireContext(),
////                        requireContext().resources.getString(R.string.payment_successful),
////                        Toast.LENGTH_LONG
////                    ).show()
//                    selectedSku = sku
//                    selectedCoins = coins
//                    selectedPrice = buyingPrice
//                    paymentMeth = "paypal"
//                    var cccv = captureOrderResult
////                    Log.e("paypalData", Gson().toJson(approval.orderActions))
//                    Log.e("paypalData", Gson().toJson(approval.data))
//                    approval.data.orderId?.let { paypalCapturePayment(it) }
////                    approval.data.orderId?.let { onPaymentSuccess("in-app-purchase", it) }
//
////                    approval.data.payerId?.let { onPaymentSuccess("in-app-purchase", it) }
////                    approval.data.billingToken?.let { onPaymentSuccess("in-app-purchase", it) }
//
//                }
//            },
//            onCancel = OnCancel {
//                paymentBottomsheetDialog.dismiss()
//                Log.d("OnCancel", "Buyer canceled the PayPal experience.")
//                Toast.makeText(
//                    requireContext(),
//                    AppStringConstant1.buyer_canceled_paypal_transaction,
////                    getString(R.string.buyer_canceled_paypal_transaction),
//                    Toast.LENGTH_LONG
//                ).show()
//            },
//            onError = OnError { errorInfo ->
//                paymentBottomsheetDialog.dismiss()
//                Log.d("OnError", "Error: $errorInfo")
//                Toast.makeText(
//                    requireContext(),
//                    AppStringConstant1.msg_some_error_try_after_some_time,
////                    getString(R.string.msg_some_error_try_after_some_time),
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        )
        proceedToPayment.setOnClickListener {

            if (stripeRadioButton.isChecked) {
                selectedSku = sku
                selectedCoins = coins
                selectedPrice = buyingPrice
                paymentMeth = "stripe"
                startStripePayment(coins, buyingPrice, coinCurrency)
                paymentBottomsheetDialog.dismiss()
            } else if (googlePayRadioButton.isChecked) {
                Log.d("InAppPurchase", "showPrice:Data ")
                selectedSku = sku
                selectedCoins = coins
                selectedPrice = buyingPrice
                paymentMeth = "Gpay"
                requestPayment()
                paymentBottomsheetDialog.dismiss()

            }else if (googleRadioButton.isChecked) {
                Log.d("InAppPurchase", "showPrice:Data ")
                selectedSku = sku
                selectedCoins = coins
                selectedPrice = buyingPrice
                paymentMeth = "InApp"
                val product = getSkuProductById(selectedSku)
                Log.d("InAppPurchaseproduct", product)
                makePurchase(product)
                paymentBottomsheetDialog.dismiss()

            } else if (bokuRadioButton.isChecked) {
                Log.d("InAppPurchase", "Latitude $latitude Longitude $longtitude")
                if (latitude.isNotEmpty()) {
                    paymentMeth = "Boku"
                    startBokuPayment()
                    paymentBottomsheetDialog.dismiss()
                } else {
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(requireContext())
                    fusedLocationProviderClient?.lastLocation?.addOnSuccessListener { location ->
                        if (location != null) {
                            latitude = location.latitude.toString()
                            longtitude = location.longitude.toString()
                            paymentMeth = "Boku"
                            startBokuPayment()
                            paymentBottomsheetDialog.dismiss()
                        } else {
                            statusCheck()
                        }
                    }
                }
            } else if (payapalRadioButton.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    PayPalCheckout.registerCallbacks(onApprove = OnApprove { approval ->
//                        approval.orderActions.capture { captureOrderResult ->
//                            paymentBottomsheetDialog.dismiss()
//
//                            Log.e("CaptureOrder", "CaptureOrderResult: $captureOrderResult")
////                    Toast.makeText(
////                        requireContext(),
////                        requireContext().resources.getString(R.string.payment_successful),
////                        Toast.LENGTH_LONG
////                    ).show()
//                            selectedSku = sku
//                            selectedCoins = coins
//                            selectedPrice = buyingPrice
//                            paymentMeth = "paypal"
//                            var cccv = captureOrderResult
////                    Log.e("paypalData", Gson().toJson(approval.orderActions))
//                            Log.e("paypalData", Gson().toJson(approval.data))
//                            approval.data.orderId?.let { paypalCapturePayment(it) }
////                    approval.data.orderId?.let { onPaymentSuccess("in-app-purchase", it) }
//
////                    approval.data.payerId?.let { onPaymentSuccess("in-app-purchase", it) }
////                    approval.data.billingToken?.let { onPaymentSuccess("in-app-purchase", it) }
//
//                        }
//                    },
//                        onCancel = OnCancel {
//                            paymentBottomsheetDialog.dismiss()
//                            Log.d("OnCancel", "Buyer canceled the PayPal experience.")
//                            Toast.makeText(
//                                requireContext(),
//                                AppStringConstant1.buyer_canceled_paypal_transaction,
////                    getString(R.string.buyer_canceled_paypal_transaction),
//                                Toast.LENGTH_LONG
//                            ).show()
//                        },
//                        onError = OnError { errorInfo ->
//                            paymentBottomsheetDialog.dismiss()
//                            Log.d("OnError", "Error: $errorInfo")
//                            Toast.makeText(
//                                requireContext(),
//                                AppStringConstant1.msg_some_error_try_after_some_time,
////                    getString(R.string.msg_some_error_try_after_some_time),
//                                Toast.LENGTH_LONG
//                            ).show()
//                        })
//                    PayPalCheckout.startCheckout(createOrder =
//                    CreateOrder { createOrderActions ->
//                        val order =
//                            Order(
//
//                                intent = OrderIntent.CAPTURE,
//                                appContext = AppContext(userAction = UserAction.PAY_NOW),
//
//                                purchaseUnitList = listOf(
//                                    PurchaseUnit(
//                                        amount =
//                                        Amount(currencyCode = CurrencyCode.USD, value = amount.toString())
//                                    )
//                                )
//                            )
//
//
////                        createOrderActions.create(order)
//                    })
                    selectedSku = sku
                            selectedCoins = coins
                            selectedPrice = buyingPrice
                            paymentMeth = "paypal"
                    paypalcreateOrder(amount, currentCurrency)

                } else {
                    Toast.makeText(activity, "Checkout SDK only available for API 23+", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "Checkout SDK only available for API 23+", Toast.LENGTH_SHORT).show()

            }
        }
        paymentBottomsheetDialog.setContentView(customView)
        paymentBottomsheetDialog.show()

        paymentBottomsheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        paymentBottomsheetDialog.dismissWithAnimation = true
        paymentBottomsheetDialog.behavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        paymentBottomsheetDialog.dismiss()
                    }

                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })


    }


    private fun startBokuPayment() {
        val bottomsheetDialog = BottomSheetDialog(requireContext())
        val customView = layoutInflater.inflate(R.layout.dialog_operators_list, null, false)
        val recyclerViewOperators = customView.findViewById<RecyclerView>(R.id.rv_operators)
        val textviewNoOperators =
            customView.findViewById<MaterialTextView>(R.id.tv_nooperatorsavailable)
        val progressBar = customView.findViewById<ProgressBar>(R.id.progressbar)
        val operatorAdapter = OperatorsAdapter()
        recyclerViewOperators.adapter = operatorAdapter
        recyclerViewOperators.layoutManager = LinearLayoutManager(requireContext())
        operatorAdapter.setOnItemClickListener {
            sharedPref.setOperatorCode(it.toString())
            startPinAuthorisation()
            bottomsheetDialog.dismiss()
        }
        lifecycleScope.launch {
            val userToken = getCurrentUserToken()
            try {
                val response = apolloClient(requireContext(), userToken!!).mutation(
                    AvailableBokuOperatorsMutation(latitude, longtitude)
                ).execute()
                Log.e("csdcdscc",response.toString())
                progressBar.visibility = View.GONE
                if (response.hasErrors()) {
                    textviewNoOperators.visibility = View.VISIBLE
                    textviewNoOperators.text = response.errors?.get(0)?.message
                        ?: AppStringConstant1.something_went_wrong_please_try_again_later
//                        ?: "Something went wrong, Please try after sometime"
                } else {
                    val operators: List<String?> =
                        response.data?.availableBokuOperators?.operators ?: listOf()
                    Log.d("PurchaseFragment", "Operators $operators")
                    if (operators.isNotEmpty()) {
                        operatorAdapter.operatorList = operators
                    } else {
                        textviewNoOperators.visibility = View.VISIBLE
                    }
                }
            } catch (e: ApolloException) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")
                progressBar.visibility = View.GONE
                textviewNoOperators.text = e.message
            } catch (e: Exception) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")
                progressBar.visibility = View.GONE
                textviewNoOperators.text = e.message
            }
        }
        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()
    }

    private fun startPinAuthorisation() {
        val bottomsheetDialog = BottomSheetDialog(requireContext())
        val customView = layoutInflater.inflate(R.layout.dialog_entermobilenumber, null, false)
        val ediTextMobileNumber = customView.findViewById<TextInputEditText>(R.id.ed_mobile)
        val submitCardView = customView.findViewById<MaterialCardView>(R.id.cd_submit)
        val textInputLayout = customView.findViewById<TextInputLayout>(R.id.tl_mobile)
        val title = customView.findViewById<MaterialTextView>(R.id.description)
        val progressbar = customView.findViewById<ProgressBar>(R.id.progressbar)
        val operatorCode = sharedPref.getOperatorCode()
        submitCardView.setOnClickListener {
            val mobileNumber = ediTextMobileNumber.text.toString()
            if (mobileNumber.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    AppStringConstant1.please_enter_mobile_number,
//                    getString(R.string.please_enter_mobile_number),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            progressbar.visibility = View.VISIBLE
            textInputLayout.visibility = View.GONE
            title.text = AppStringConstant1.please_wait_sending_the_pin
//                getString(R.string.please_wait_sending_the_pin)

            submitCardView.visibility = View.GONE
            lifecycleScope.launch {
                val userToken = getCurrentUserToken()
                val userId = getCurrentUserId()
                try {
                    val response = apolloClient(requireContext(), userToken!!).mutation(
                        PinAuthorisationMutation(
                            mobileNumber,
                            operatorCode,
                            false,
                            userId.toString()
                        )
                    ).execute()
                    if (response.hasErrors()) {
                        bottomsheetDialog.dismiss()
                        binding.root.snackbar(response.errors?.get(0)?.message.toString())
                    } else {
                        val data = response.data?.pinAuthorisation
                        if (data?.success == true) {
                            sharedPref.setOperatorCode(data.operationReference)
                            verifyPIN()
                        } else {
                            bottomsheetDialog.dismiss()
                            binding.root.snackbar(AppStringConstant1.sorry_pin_failed_to_send)
//                            binding.root.snackbar(getString(R.string.sorry_pin_failed_to_send))
                        }
                    }
                } catch (e: ApolloException) {
                    bottomsheetDialog.dismiss()
                    binding.root.snackbar("${e.message}")
                } catch (e: Exception) {
                    bottomsheetDialog.dismiss()
                    binding.root.snackbar("${e.message}")
                }
            }
        }
        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()
    }


    fun paymentIntentComplete(paymentResult: PaymentIntent) {
        Log.e("PaymentResponseSucess", paymentResult.paymentMethodId!!)
        Log.e("PaymentResponseSucess", paymentResult.id!!)
        showProgressView()
        Log.e("TAG","StripeData 6")

        lifecycleScope.launchWhenResumed {

            val userToken = getCurrentUserToken()!!
            val response = try {
                Log.e("TAG","StripeData 5")

                apolloClient(requireContext(), userToken).mutation(
                    StripePaymentSuccessMutation(paymentResult.id!!, paymentResult.status!!.code)
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloException ${e.message}")
                Log.e("TAG","apolloException ${e.message}")

//                binding.root.snackbar("Exception Stripe Create Intent ${e.message}")
                binding.root.snackbar(" ${e.message}")
                hideProgressView()
                Log.e("TAG","StripeData 8")

                return@launchWhenResumed
            }

            if (response.hasErrors()) {
                hideProgressView()
                Log.e("TAG","StripeData 7")

                val errorMessage = response.errors?.get(0)?.message
                Log.e("errorCreateIntent", "$errorMessage")
                if (errorMessage != null) {
                    binding.root.snackbar(errorMessage)
                }
            } else {

                hideProgressView()
                Log.e("TAG","StripeData 9")

                //                var   clientSecret =  response.data!!.stripeCreateIntent!!.clientSecret!!
                stripeBottomsheetDialog.dismiss()
//                var   clientSecret =  response.data!!.stripeCreateIntent!!.clientSecret!!
                Log.e("StripePaymentSuccess", Gson().toJson(response))

                onPaymentSuccess("in-app-purchase", paymentResult.id!!)


            }

        }

    }

    private fun startStripePayment(coins: Int, buyingPrice: Double, coinCurrency: String) {


        stripeBottomsheetDialog = BottomSheetDialog(requireContext())
        val customView = layoutInflater.inflate(R.layout.dialog_stripe_detail, null, false)
        val stripeCardInput = customView.findViewById<CardInputWidget>(R.id.stripeCardInput)
        val submitCardView = customView.findViewById<MaterialCardView>(R.id.cd_submit)
//        val title = customView.findViewById<MaterialTextView>(R.id.description)
        submitCardView.setOnClickListener {


            stripeCardInput.paymentMethodCreateParams?.let { params ->

                showProgressView()
                lifecycleScope.launchWhenResumed {


                    val userToken = getCurrentUserToken()!!
                    val response = try {
                        Log.e("TAG","StripeData ")
                        apolloClient(requireContext(), userToken).mutation(
                            StripeCreateIntentMutation(buyingPrice, coinCurrency, "card")
                        ).execute()
                    } catch (e: ApolloException) {
                        Timber.d("apolloException ${e.message}")
                        Log.e("TAG","StripeData 1")

                        binding.root.snackbar("${e.message}")
//                        binding.root.snackbar("Exception Stripe Create Intent ${e.message}")
                        hideProgressView()
                        Log.e("TAG","StripeData 3")
                        return@launchWhenResumed
                    }

                    if (response.hasErrors()) {
                        hideProgressView()
                        Log.e("TAG","StripeData 2")
                        val errorMessage = response.errors?.get(0)?.message
                        Log.e("errorCreateIntent", "$errorMessage")
                        if (errorMessage != null) {
                            binding.root.snackbar(errorMessage)
                        }
                    } else {
                        hideProgressView()

                        Log.e("TAG","StripeData 4")

                        MainActivity.getMainActivity()!!.showLoader();

                        var clientSecret = response.data!!.stripeCreateIntent!!.clientSecret!!
                        Log.e("StripeCreateIntent", Gson().toJson(response))

//                    stripeCardInput.paymentMethodCreateParams?.let { params ->

//                        Stripe stripe = new Stripe(this, publishable key);
//                  var      confirmPaymentIntentParams :       ConfirmPaymentIntentParams  = ConfirmPaymentIntentParams.create(clientSecret);

                         lifecycleScope.launch {

                            val confirmParams = ConfirmPaymentIntentParams
                                .createWithPaymentMethodCreateParams(params, clientSecret)

                            stripe?.confirmPayment(MainActivity.getMainActivity()!!, confirmParams)

//                            paymentLauncher.confirm(confirmParams)
                        }

                    }
                }

            }
        }
        stripeBottomsheetDialog.setContentView(customView)
        stripeBottomsheetDialog.show()

    }


    private fun verifyPIN() {
        val bottomsheetDialog = BottomSheetDialog(requireContext())
//        val customView = layoutInflater.inflate(R.layout.dialog_entermobilenumber, null, false)
        val customView = layoutInflater.inflate(R.layout.dialog_enterpin, null, false).apply {

        }

        val submitCardView = customView.findViewById<MaterialCardView>(R.id.cd_submit)
        val textInputLayout = customView.findViewById<TextInputLayout>(R.id.tl_mobile)
        val ediTextPIN = customView.findViewById<TextInputEditText>(ed_pin)
        val title = customView.findViewById<MaterialTextView>(R.id.description)
        val progressbar = customView.findViewById<ProgressBar>(R.id.progressbar)
        val operationReference = sharedPref.getOperatorReference()
        var userToken: String? = ""
        var chargingToken = ""
        lifecycleScope.launch {
            try {
                userToken = getCurrentUserToken()
                val response = apolloClient(requireContext(), userToken!!).query(
                    PaymentByOperationReferenceQuery(operationReference)
                ).execute()
                if (response.hasErrors()) {
                    bottomsheetDialog.show()
                    binding.root.snackbar(
                        "${AppStringConstant1.charging_token_exception} ${
                            response.errors?.get(
                                0
                            )?.message
                        }"
                    )
//                    binding.root.snackbar(
//                        "${getString(R.string.charging_token_exception)} ${
//                            response.errors?.get(
//                                0
//                            )?.message
//                        }"h
//                    )
                } else {
                    chargingToken =
                        response.data?.paymentByOperationReference?.chargingToken.toString()
                }
            } catch (e: ApolloException) {
                bottomsheetDialog.show()
//                binding.root.snackbar("${getString(R.string.charging_token_exception)} ${e.message}")
                binding.root.snackbar("${AppStringConstant1.charging_token_exception} ${e.message}")
            } catch (e: Exception) {
                bottomsheetDialog.show()

                binding.root.snackbar("${AppStringConstant1.charging_token_exception} ${e.message}")
//                binding.root.snackbar("${getString(R.string.charging_token_exception)} ${e.message}")
            }
        }
        submitCardView.setOnClickListener {
            val pin = ediTextPIN.text.toString()
            if (pin.isEmpty()) {
//                Toast.makeText(
//                    requireContext(),
//                    getString(R.string.please_enter_pin),
//                    Toast.LENGTH_LONG
//                ).show()

                Toast.makeText(
                    requireContext(),
                    AppStringConstant1.please_enter_pin,
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            progressbar.visibility = View.VISIBLE
            textInputLayout.visibility = View.GONE
            title.text =
                AppStringConstant1.please_wait_verifying_the_pin

//                getString(R.string.please_wait_verifying_the_pin)
            submitCardView.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    val response = apolloClient(requireContext(), userToken!!).mutation(
                        MobilePinInputMutation(chargingToken, pin.toInt())
                    ).execute()
                    if (response.hasErrors()) {
                        bottomsheetDialog.show()

                        binding.root.snackbar(
                            "${AppStringConstant1.pin_verification_successful_please_wait} ${
                                response.errors?.get(
                                    0
                                )?.message
                            }"
                        )
//                        binding.root.snackbar(
//                            "${getString(R.string.pin_verification_successful_please_wait)} ${
//                                response.errors?.get(
//                                    0
//                                )?.message
//                            }"
//                        )
                    } else {
                        title.text =
                            AppStringConstant1.pin_verification_successful_please_wait

//                            getString(R.string.pin_verification_successful_please_wait)
                        try {
                            val responseForPaymentOperation =
                                apolloClient(requireContext(), userToken!!).query(
                                    PaymentByOperationReferenceQuery(operationReference)
                                ).execute()
                            if (responseForPaymentOperation.hasErrors()) {
                                bottomsheetDialog.show()
                                binding.root.snackbar(
                                    "${AppStringConstant1.authorisation_exception} ${
                                        responseForPaymentOperation.errors?.get(
                                            0
                                        )?.message
                                    }"
                                )
//                                binding.root.snackbar(
//                                    "${getString(R.string.authorisation_exception)} ${
//                                        responseForPaymentOperation.errors?.get(
//                                            0
//                                        )?.message
//                                    }"
//                                )
                            } else {
                                when (responseForPaymentOperation.data?.paymentByOperationReference?.authorisationState) {
                                    "verified" -> {
                                        title.text =
                                            AppStringConstant1.fetching_charging_token_please_wait
//                                            getString(R.string.fetching_charging_token_please_wait)
                                        try {
                                            val responseForChargingToken =
                                                apolloClient(requireContext(), userToken!!).query(
                                                    PaymentByOperationReferenceQuery(
                                                        operationReference
                                                    )
                                                ).execute()
                                            if (responseForChargingToken.hasErrors()) {
                                                bottomsheetDialog.show()

                                                binding.root.snackbar(
                                                    "${AppStringConstant1.charging_token_exception} ${
                                                        responseForChargingToken.errors?.get(
                                                            0
                                                        )?.message
                                                    }"
                                                )

//                                                binding.root.snackbar(
//                                                    "${getString(R.string.charging_token_exception)} ${
//                                                        responseForChargingToken.errors?.get(
//                                                            0
//                                                        )?.message
//                                                    }"
//                                                )
                                            } else {
                                                val newChargingToken =
                                                    responseForChargingToken.data?.paymentByOperationReference?.chargingToken.toString()
                                                title.text =
//                                                    getString(R.string.charging_payment_please_wait)
                                                    AppStringConstant1.charging_payment_please_wait
                                                try {
                                                    val responseFromChargePayment = apolloClient(
                                                        requireContext(),
                                                        userToken!!
                                                    ).mutation(
                                                        ChargePaymentMutation(
                                                            amount,
                                                            newChargingToken,
                                                            "You bought coins from i69 app"
                                                        )
                                                    ).execute()
                                                    if (responseFromChargePayment.hasErrors()) {
                                                        bottomsheetDialog.show()


                                                        binding.root.snackbar(
                                                            "${AppStringConstant1.charging_payment_exception} ${
                                                                responseFromChargePayment.errors?.get(
                                                                    0
                                                                )?.message
                                                            }"
                                                        )

//                                                        binding.root.snackbar(
//                                                            "${getString(R.string.charging_payment_exception)} ${
//                                                                responseFromChargePayment.errors?.get(
//                                                                    0
//                                                                )?.message
//                                                            }"
//                                                        )
                                                    } else {
                                                        val status =
                                                            responseFromChargePayment?.data?.chargePayment?.success
                                                        if (status == true) {

                                                            onPaymentSuccess(
                                                                "in-app-purchase",
                                                                "${responseFromChargePayment.data?.chargePayment!!.id}"
                                                            )
                                                            bottomsheetDialog.show()

//                                                            binding.root.snackbar(getString(R.string.you_successfuly_bought_the_coins))
                                                            binding.root.snackbar(AppStringConstant1.you_successfuly_bought_the_coins)
                                                        } else {
                                                            bottomsheetDialog.show()
                                                            binding.root.snackbar(
                                                                "${AppStringConstant1.charging_payment_exception} ${
                                                                    responseFromChargePayment.errors?.get(
                                                                        0
                                                                    )?.message
                                                                }"
                                                            )
//                                                            binding.root.snackbar(
//                                                                "${getString(R.string.charging_payment_exception)} ${
//                                                                    responseFromChargePayment.errors?.get(
//                                                                        0
//                                                                    )?.message
//                                                                }"
//                                                            )
                                                        }
                                                    }
                                                } catch (e: ApolloException) {
                                                    bottomsheetDialog.show()
//                                                    binding.root.snackbar("${getString(R.string.charging_payment_exception)} ${e.message}")
                                                    binding.root.snackbar("${AppStringConstant1.charging_payment_exception} ${e.message}")
                                                } catch (e: Exception) {
                                                    bottomsheetDialog.show()
//                                                    binding.root.snackbar("${getString(R.string.charging_payment_exception)} ${e.message}")
                                                    binding.root.snackbar("${ AppStringConstant1.charging_payment_exception} ${e.message}")
                                                }
                                            }
                                        } catch (e: ApolloException) {
                                            bottomsheetDialog.show()

//                                            binding.root.snackbar("${getString(R.string.charging_token_exception)} ${e.message}")
                                            binding.root.snackbar("${ AppStringConstant1.charging_token_exception} ${e.message}")
                                        } catch (e: Exception) {
                                            bottomsheetDialog.show()
//                                            binding.root.snackbar("${getString(R.string.charging_token_exception)} ${e.message}")
                                            binding.root.snackbar("${ AppStringConstant1.charging_token_exception} ${e.message}")

                                        }
                                    }
                                    else -> {
                                        bottomsheetDialog.show()

                                        binding.root.snackbar("${AppStringConstant1.authorisation} ${responseForPaymentOperation.data?.paymentByOperationReference?.authorisationState}")
//                                        binding.root.snackbar("${getString(R.string.authorisation)} ${responseForPaymentOperation.data?.paymentByOperationReference?.authorisationState}")
                                    }

                                }
                            }
                        } catch (e: ApolloException) {
                            bottomsheetDialog.show()
//                            binding.root.snackbar("${getString(R.string.authorisation_exception)} ${e.message}")
                            binding.root.snackbar("${AppStringConstant1.authorisation_exception} ${e.message}")
                        } catch (e: Exception) {
                            bottomsheetDialog.show()
//                            binding.root.snackbar("${getString(R.string.authorisation_exception)} ${e.message}")
                            binding.root.snackbar("${AppStringConstant1.authorisation_exception} ${e.message}")
                        }
                    }
                } catch (e: ApolloException) {
                    bottomsheetDialog.show()

//                    binding.root.snackbar("${getString(R.string.pin_verification_exception)} ${e.message}")
                    binding.root.snackbar("${AppStringConstant1.pin_verification_exception} ${e.message}")
                } catch (e: Exception) {
                    bottomsheetDialog.show()
                    binding.root.snackbar("${AppStringConstant1.pin_verification_exception} ${e.message}")
//                    binding.root.snackbar("${getString(R.string.pin_verification_exception)} ${e.message}")
                }
            }
        }
        bottomsheetDialog.setContentView(customView)
        bottomsheetDialog.show()
    }

    override fun setupClickListeners() {
        binding.purchaseClose.setOnClickListener {
            //activity?.onBackPressed()

            findNavController().popBackStack()
//            findNavController().navigateUp()
        }
    }

    /* private fun showPrice(
         sku: Int,
         price: ItemPurchaseCoinsBinding,
         coins: Int,
         buyingPrice: Double,
         priceFirst: String,


         priceSecond: String,
         discountPrice: String,
         showDiscount: Boolean = true
     ) {
         price.numberOfCoins.text = "$coins"
         price.priceGoldCircle.setVisibleOrInvisible(showDiscount)
         price.price.text = priceFirst
         price.priceSmall.text = priceSecond
         if (!showDiscount) {
             price.price.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
             price.priceSmall.setTextColor(
                 ContextCompat.getColor(
                     requireContext(),
                     R.color.colorPrimary
                 )
             )
         }
         price.salePrice.text = Html.fromHtml("<strike>€${discountPrice}</strike>")
         price.root.setOnClickListener {
             amount = buyingPrice
             showBottomSheetForPaymentOptions(sku, coins, buyingPrice)
             //  dialog.show()
         }
     }*/

    private fun getSkuProductById(sku: Int) = when (sku) {
        1 -> com.i69.data.config.Constants.IN_APP_FIRST_TYPE
        2 -> com.i69.data.config.Constants.IN_APP_SECOND_TYPE
        3 -> com.i69.data.config.Constants.IN_APP_THIRD_TYPE
        4 -> com.i69.data.config.Constants.IN_APP_FOURTH_TYPE
        5 -> com.i69.data.config.Constants.IN_APP_FIFTH_TYPE
        6 -> com.i69.data.config.Constants.IN_APP_SIXTH_TYPE
        7 -> com.i69.data.config.Constants.IN_APP_SEVENTH_TYPE
        8 -> com.i69.data.config.Constants.IN_APP_EIGHTH_TYPE
        9 -> com.i69.data.config.Constants.IN_APP_NINETH_TYPE
        10 -> com.i69.data.config.Constants.IN_APP_TENTH_TYPE
        else -> com.i69.data.config.Constants.IN_APP_FIRST_TYPE
    }

    private fun initPayPal() {
        /*val payPalBtn = dialog.findViewById<PayPalButton>(R.id.payPalButton)
        //selectedPrice = 10.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            payPalBtn.setup(
                createOrder = CreateOrder { createOrderActions ->
                    val order = Order(
                        intent = OrderIntent.CAPTURE,
                        appContext = AppContext(
                            userAction = Constants.PAYPAL_USER_ACTION
                        ),
                        purchaseUnitList = listOf(
                            PurchaseUnit(
                                amount = Amount(
                                    currencyCode = Constants.PAYPAL_CURRENCY,
                                    value = selectedPrice.toString()
                                )
                            )
                        )
                    )
                    createOrderActions.create(order)
                },
                onApprove = OnApprove { approval ->
                    dialog.dismiss()
                    approval.orderActions.capture { captureOrderResult ->
                        Timber.d("CaptureOrderResult: $captureOrderResult")
                        onPaymentSuccess("paypal")
                    }
                },
                onCancel = OnCancel {
                    dialog.dismiss()
                    Timber.e("Buyer canceled the PayPal experience.")
                },
                onError = OnError { errorInfo ->
                    dialog.dismiss()
                    onFailureListener(errorInfo.reason)
                }
            )
        }*/
    }

    private fun makePurchase(sku: String) {
        viewModel.buySku(requireActivity(), sku)
    }


    private fun paypalcreateOrder(amount: Double, currency: String) {
        lifecycleScope.launch {
            val userToken = getCurrentUserToken()
            try {
                val response = apolloClient(requireContext(), userToken!!).mutation(
                    PaypalCreateOrderMutation(amount,currency)

                ).execute()
                Log.e("ddsfsd",response.toString())

                if (response.hasErrors()) {
                    Log.e("ascdsvsd",response.errors?.get(0)?.message.toString())

                    response.errors?.get(0)?.message
                        ?: getString(R.string.something_went_wrong_please_try_again_later)
//                        ?: "Something went wrong, Please try after sometime"

                    Toast.makeText(activity, getString(R.string.something_went_wrong_please_try_again_later), Toast.LENGTH_SHORT).show()

                } else {

                    paypalCreateOrderId = response.data?.paypalCreateOrder?.id.toString()

                    val intent = Intent(context,WebPaymentActivity::class.java)
                    intent.putExtra("url",
                        "https://www.paypal.com/checkoutnow?token=$paypalCreateOrderId"
                 //       "https://www.sandbox.paypal.com/checkoutnow?token=$paypalCreateOrderId"
                    )
                    intent.putExtra("id",paypalCreateOrderId)
                    startActivity(intent)
                    paymentBottomsheetDialog.dismiss()

                    Log.e("CreatePaypalOrderId", Gson().toJson(response.data?.paypalCreateOrder))

                }
            } catch (e: ApolloException) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")

            } catch (e: Exception) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")

            }
        }


    }


    private fun paypalCapturePayment(orderId: String) {

        Log.e("craetedOrderId", paypalCreateOrderId)
        Log.e("FromcraetedOrderId", orderId)
        lifecycleScope.launch {
            val userToken = getCurrentUserToken()
            try {
                val response = apolloClient(requireContext(), userToken!!).mutation(
                    PaypalCapturePaymentMutation(paypalCreateOrderId)
                ).execute()

                if (response.hasErrors()) {

                    var message = response.errors?.get(0)?.message
                        ?: AppStringConstant1.something_went_wrong_please_try_again_later
//                        ?: "Something went wrong, Please try after sometime"

                    Log.e("MyPaymentIdWrong", Gson().toJson(response.errors))
                    binding.root.snackbar(message)
                } else {

                    Log.e(
                        "CapturePaypalOrderId",
                        Gson().toJson(response.data?.paypalCapturePayment)
                    )
                    onPaymentSuccess("in-app-purchase", response.data?.paypalCapturePayment?.id!!)
                }
            } catch (e: ApolloException) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")

            } catch (e: Exception) {
                Log.d("PurchaseFragment", "Operators Exception ${e.message}")

            }
        }
    }


    public fun onPaymentSuccess(method: String, paymentId: String) {
        showProgressView()
        Log.e("TAG","StripeData 9")

        lifecycleScope.launch(Dispatchers.Main) {
            val userId = getCurrentUserId()!!
            val userToken = getCurrentUserToken()!!
            val purchaseRequest = PurchaseRequest(
                id = userId,
                currency = currentCurrency,
                method = method,
                coins = selectedCoins,
                money = selectedPrice,
                paymentMethod = paymentMeth,
                payment_id = paymentId
            )

            Log.e("MyPaymentId", paymentId)
            when (val response = viewModel.purchaseCoin(purchaseRequest, token = userToken)) {
                is Resource.Success -> {

                    Log.e("TAG","StripeData 10")

                    if (response.data?.data!!.success) {
                        Log.e("myPurchaseCoinResponce", Gson().toJson(response))
                        viewModel.loadCurrentUser(userId, token = userToken, true)
                        hideProgressView()

                        val successMsg = String.format(AppStringConstant1.congrats_purchase, selectedCoins)
                        binding.root.autoSnackbarOnTop(successMsg, Snackbar.LENGTH_LONG) {
                            moveToProfileScreen()
                        }
                    } else {
                        Log.e("myPurchaseCoinResponce", Gson().toJson(response))
//                        onFailureListener(getString(R.string.something_went_wrong))
                        hideProgressView()
                        onFailureListener(AppStringConstant1.something_went_wrong)

                    }
                }
                is Resource.Error -> onFailureListener(
                    response.message ?: "")

                else -> {
                    Log.e("TAG","StripeData 12")

                }
            }
        }
    }

    private fun moveToProfileScreen() {
        navController.navigate(com.i69.R.id.action_purchaseFragment_to_userProfileFragment)
    }

    public fun onGooglePaymentSuccess(method: String, paymentId: String) {
//        showProgressView()
        lifecycleScope.launch(Dispatchers.Main) {
            val userId = getCurrentUserId()!!
            val userToken = getCurrentUserToken()!!
            val purchaseRequest = PurchaseRequest(
                id = userId,
                currency = currentCurrency,
                method = method,
                coins = selectedCoins,
                money = selectedPrice,
                paymentMethod = paymentMeth,
                payment_id = paymentId
            )

            Log.e("MyPaymentId", paymentId)
            when (val response = viewModel.googleCreateOrder(purchaseRequest, token = userToken)) {
                is Resource.Success -> {
                    hideProgressView()
                    if (response.data?.data!!.success) {
                        Log.e("myPurchaseCoinResponce", Gson().toJson(response))
//                        viewModel.loadCurrentUser(userId, token = userToken, true)
//                        congratulationsToast(selectedCoins)
                    } else {
                        Log.e("myPurchaseCoinResponce", Gson().toJson(response))
//                        onFailureListener(getString(R.string.something_went_wrong))
                        onFailureListener(AppStringConstant1.something_went_wrong)

                    }
                }
                is Resource.Error -> onFailureListener(
                    response.message ?: "")
                else -> {}
            }
        }
    }

    private fun congratulationsToast(coins: Int) {
//        binding.root.snackbar(String.format(getString(R.string.congrats_purchase), coins))

        binding.root.snackbar(String.format(AppStringConstant1.congrats_purchase, coins))
    }

    private fun onFailureListener(error: String) {
        hideProgressView()
        Log.e("TAG","StripeData 11")

        Log.e("purchaseCoinResponError", error)
        Timber.e("${getString(R.string.something_went_wrong)} $error")
        binding.root.snackbar("${AppStringConstant1.something_went_wrong} $error")
//        binding.root.snackbar("${getString(R.string.something_went_wrong)} $error")
    }

    override fun onResume() {
        super.onResume()
        Log.e("time_2", "onResume")
        if(BillingDataSource.purchasesUpdated)
        {
            showProgressView()
            BillingDataSource.purchasesUpdated = false
        }

        if(WebPaymentActivity.IS_Done) {
            Log.e("resuke", WebPaymentActivity.paypalCapturePayment)
            onPaymentSuccess("in-app-purchase", WebPaymentActivity.paypalCapturePayment)
            WebPaymentActivity.IS_Done = false
        }else if(IS_Payment_Done) {
            Log.e("resuke", paypalCaptureId)
            Toast.makeText(requireContext(),"Coins Updated",Toast.LENGTH_SHORT).show()
            onPaymentSuccess("COINS",paypalCaptureId)
            IS_Payment_Done = false
        }
    }

    private fun requestPayment() {

        // Disables the button to prevent multiple clicks.
//        googlePayButton.isClickable = false

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        val dummyPriceCents = 100L
        val shippingCostCents = 900L
        val task = viewModel.getLoadPaymentDataTask(dummyPriceCents + shippingCostCents)

        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                completedTask.result.let(::handlePaymentSuccess)
            } else {
                when (val exception = completedTask.exception) {
                    is ResolvableApiException -> {
                        resolvePaymentForResult.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    }

                    is ApiException -> {
                        handleError(exception.statusCode, exception.message)
                    }

                    else -> {
                        handleError(
                            CommonStatusCodes.INTERNAL_ERROR, "Unexpected non API" +
                                    " exception when trying to deliver the task result to an activity!"
                        )
                    }
                }
            }

            // Re-enables the Google Pay payment button.
//            googlePayButton.isClickable = true
        }
    }

    // Handle potential conflict from calling loadPaymentData
    private val resolvePaymentForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK ->
                    result.data?.let { intent ->
                        PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                    }

                RESULT_CANCELED -> {
                    // The user cancelled the payment attempt
                }
            }
        }

    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     *
     * @param paymentData A response object returned by Google after a payer approves payment.
     * @see [Payment
     * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
     */
    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)
            Log.d("paymentMethodData", paymentMethodData.toString())

//            Toast.makeText(
//                this,
//                getString(R.string.payments_show_name, billingName),
//                Toast.LENGTH_LONG
//            ).show()

            // Logging token string.
            Log.d(
                "Google Pay token", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

            onPaymentSuccess("in-app-purchase", paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token"))
            onGooglePaymentSuccess("in-app-purchase", paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token"))
//            startActivity(Intent(this, CheckoutSuccessActivity::class.java))

        } catch (error: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $error")
        }
    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int, message: String?) {
        Log.e("Google Pay API error", "Error code: $statusCode, Message: $message")
    }


}
