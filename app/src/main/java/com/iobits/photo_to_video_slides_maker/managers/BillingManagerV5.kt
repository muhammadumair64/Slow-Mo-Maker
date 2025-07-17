package com.iobits.photo_to_video_slides_maker.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.activities.MainActivity
import com.iobits.photo_to_video_slides_maker.utils.Constants
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BillingManagerV5 @Inject constructor(val context: Context) {
    //    private static volatile BillingManagerV5 instance;
    private var retryCount = 0
    private var billingClient: BillingClient? = null
    private var productsAvailable: List<ProductDetails> = ArrayList()

    private fun inItBillingManagerV5() {
        //Initialize a BillingClient with PurchasesUpdatedListener onCreate method
        Log.d(TAG, "BillingManagerV5: init")
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener { billingResult: BillingResult, list: List<Purchase>? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase: Purchase in list) {
                        verifySubPurchase(purchase)
                    }
                }
            }.build()
        //start the connection after initializing the billing client
        establishConnection()
    }

    fun establishConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    showProducts()
                    oneTimePurchaseDetails(Constants.ITEM_SKU_PRO_USER_SUB)
                    // get history of purchases. if already have purchase items
                    val params =
                        QueryPurchaseHistoryParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()

                    billingClient?.queryPurchaseHistoryAsync(
                        params
                    ) { p0, p1 ->
//                        handlePurchaseHistory(p0, p1)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                retryCount += 1
                if (retryCount < 3)
                    Handler(Looper.getMainLooper()).postDelayed({ establishConnection() }, 10000)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun showProducts() {
        Log.d(TAG, "showProducts: ")
        val productList = ImmutableList.of(
            //Product 1
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),  //Product 2
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("get_premium")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient?.queryProductDetailsAsync(
            params
        ) { _: BillingResult?, prodDetailsList: List<ProductDetails> ->
            Log.d(TAG, "showProducts: queryProductDetailsAsync size" + prodDetailsList.size)
            productsAvailable = prodDetailsList
        }
    }

    private fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = ImmutableList.of(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    private fun verifySubPurchase(purchases: Purchase) {
        Log.d(TAG, "verifySubPurchase: ")
        val acknowledgePurchaseParams = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchases.purchaseToken)
            .build()

        if (purchases.products[0].equals(Constants.ITEM_SKU_REMOVE_ADS_ONLY))
            makeAppAdsFree()
        if (purchases.products[0].equals(Constants.ITEM_SKU_GET_PREMIUM))
            makeAppPremium()
        if (purchases.products[0].equals(Constants.ITEM_SKU_PRO_USER_SUB))
            makeAppPremium()



        Toast.makeText(
            MyApplication.mInstance,
            MyApplication.mInstance.getString(R.string.inapp_success_meaage),
            Toast.LENGTH_LONG
        ).show()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult: BillingResult ->
            Log.d(TAG, "verifySubPurchase: acknowledgePurchase")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "verifySubPurchase: 1")

//                PreferenceManager!!.put(PreferenceManager.Key.IS_APP_ADS_FREE, true)
                Log.d(TAG, "verifySubPurchase: 2")
            }
            Log.d(TAG, "Purchase Token: " + purchases.purchaseToken)
            Log.d(TAG, "Purchase Time: " + purchases.purchaseTime)
            Log.d(TAG, "Purchase OrderID: " + purchases.orderId)
        }
    }

    private fun thanksToast() {
        Toast.makeText(
            MyApplication.mInstance,
            "Thanks for your support",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun oneTimePurchase(activity: Activity, itemSkuId: String) {
        val productList = ImmutableList.of( //Product 1
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(itemSkuId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(
            params
        ) { _: BillingResult?, prodDetailsList: List<ProductDetails> ->
            Log.d(
                TAG,
                "showProduct one time purchase: queryProductDetailsAsync size" + prodDetailsList.size
            )
            if (prodDetailsList.isNotEmpty())
                launchPurchaseFlow(activity, prodDetailsList[0])
        }
    }



    fun subscription(activity: Activity) {
        val productList = mutableListOf( //Product 1
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.ITEM_SKU_PRO_USER_SUB)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(
            params
        ) { _: BillingResult?, prodDetailsList: List<ProductDetails> ->
            Log.d(
                TAG,
                "showProduct sub purchase:${Constants.ITEM_SKU_PRO_USER_SUB}: queryProductDetailsAsync size" + prodDetailsList.size
            )
            if (prodDetailsList.isNotEmpty()) {
                val productDetailsParamsList = mutableListOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(prodDetailsList[0])
                        .setOfferToken(prodDetailsList[0].subscriptionOfferDetails!![0].offerToken)
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                Log.d(
                    TAG,
                    "showProduct 2 sub purchase: queryProductDetailsAsync size" + prodDetailsList.size
                )
                val billingResult = billingClient?.launchBillingFlow(activity!!, billingFlowParams)
            }
        }
    }


    private fun handlePurchaseHistory(p0: BillingResult, p1: MutableList<PurchaseHistoryRecord>?) {
        Log.d(TAG, "onPurchaseHistoryResponse: $p0 and record list $p1")
        if (p1?.isNotEmpty() == true) {
            p1.forEach {
                if (it.products[0] == Constants.ITEM_SKU_REMOVE_ADS_ONLY)
                    makeAppAdsFree()
                if (it.products[0] == Constants.ITEM_SKU_GET_PREMIUM)
                    makeAppPremium()
            }
        }
    }

    fun makeAppPremium() {
        // ads free and premium features
        Log.d(TAG, "makeAppPremium: ")
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_ADS_FREE,
            true
        )
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_PREMIUM,
            true
        )
        Toast.makeText(
            MyApplication.mInstance,
            "Subscription activated, Enjoy! Please Restart the App",
            Toast.LENGTH_LONG
        ).show()

        reStart()

    }

    fun makeAppPremiumForCleanAds() {
        // ads free and premium features
        Log.d(TAG, "makeAppPremium: ")
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_ADS_FREE,
            true
        )
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_PREMIUM,
            true
        )
    }

    fun reStart(){
        // Create an Intent to start the new activity

        val intent: Intent = Intent(MyApplication.mInstance, MainActivity::class.java)

// Set flags to clear the back stack and start the activity

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

// Start the new activity

        MyApplication.mInstance.startActivity(intent)
    }

    private fun makeAppAdsFree() {
        // ads free only
        Log.d(TAG, "makeAppAdsFree: ")
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_ADS_FREE,
            true
        )

        Toast.makeText(
            MyApplication.mInstance,
            "Subscription activated, Enjoy! Please Restart the App",
            Toast.LENGTH_LONG
        ).show()
        reStart()
    }

    fun oneTimePurchaseDetails( itemSkuId: String) {
        try{
            val productList = mutableListOf( //Product 1
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(itemSkuId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            billingClient?.queryProductDetailsAsync(
                params
            ) { _: BillingResult?, prodDetailsList: List<ProductDetails> ->
                if(prodDetailsList.isNotEmpty()){
                    Log.d(
                        TAG,
                        "showProduct one time purchase: queryProductDetailsAsync size" + prodDetailsList.size
                    )
                    subPremiumPrice = prodDetailsList[0].subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice.toString()
                    subPremiumPriceAfterDiscount = prodDetailsList[0].subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(1)?.formattedPrice.toString()
                    Log.d(TAG,"price is $subPremiumPrice" )
                }
            }
        }
        catch (e:Exception){
            e.localizedMessage
        }
    }
    companion object {
        var subPremiumPrice = ""
        var subPremiumPriceAfterDiscount = ""
        private const val TAG = "BillingManagerV5"
    }

    /*    public static synchronized BillingManagerV5 getInstance() {
        if (instance == null) {
            instance = new BillingManagerV5();
        }
        return instance;
    }*/
    init {
        inItBillingManagerV5()
    }
}