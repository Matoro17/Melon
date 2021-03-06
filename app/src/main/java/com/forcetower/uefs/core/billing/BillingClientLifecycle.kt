/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.forcetower.uefs.feature.shared.SingleLiveEvent
import timber.log.Timber

class BillingClientLifecycle(
    private val application: Application
) : LifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener {

    val purchaseUpdateEvent = SingleLiveEvent<List<Purchase>>()
    val purchases = MutableLiveData<List<Purchase>>()
    val state = MutableLiveData<Boolean>()

    lateinit var billingClient: BillingClient

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        billingClient = BillingClient.newBuilder(application.applicationContext).setListener(this).build()
        if (!billingClient.isReady) {
            Timber.d("Starting Connection...")
            billingClient.startConnection(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        if (billingClient.isReady) {
            Timber.d("Closing connection")
            billingClient.endConnection()
        }
    }

    override fun onBillingSetupFinished(billingResponseCode: Int) {
        Timber.d("onBillingSetupFinished: $billingResponseCode")
        if (billingResponseCode == BillingClient.BillingResponse.OK) {
            state.postValue(true)
            // The billing client is ready. You can query purchases here.
            updatePurchases()
        }
    }

    override fun onBillingServiceDisconnected() {
        Timber.d("onBillingServiceDisconnected")
        state.postValue(false)
        // TODO: Try connecting again with exponential backoff.
        // billingClient.startConnection(this)
    }

    fun startConnection() {
        billingClient.startConnection(this)
    }

    fun querySkuDetails(skus: List<String>): LiveData<SkuDetailsResult> {
        val result = MutableLiveData<SkuDetailsResult>()
        val params = SkuDetailsParams.newBuilder()
                .setSkusList(skus)
                .setType(BillingClient.SkuType.INAPP)
                .build()

        billingClient.querySkuDetailsAsync(params) { responseCode, details ->
            val value = SkuDetailsResult(responseCode, details)
            result.postValue(value)
        }
        return result
    }

    fun consumeToken(token: String): LiveData<SkuConsumeResult> {
        val result = MutableLiveData<SkuConsumeResult>()

        billingClient.consumeAsync(token) { responseCode, purchaseToken ->
            Timber.d("Code: $responseCode :: Token: $purchaseToken")
            result.postValue(SkuConsumeResult(responseCode, purchaseToken))
        }
        return result
    }

    fun updatePurchases() {
        if (!billingClient.isReady) {
            Timber.e("BillingClient is not ready to query for existing purchases")
        }
        val result = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        if (result == null) {
            Timber.i("Update purchase: Null purchase result")
            handlePurchases(null)
        } else {
            if (result.purchasesList == null) {
                Timber.i("Update purchase: Null purchase list")
                handlePurchases(null)
            } else {
                handlePurchases(result.purchasesList)
            }
        }
    }

    /**
     * Called by the Billing Library when new purchases are detected.
     */
    @SuppressLint("BinaryOperationInTimber")
    override fun onPurchasesUpdated(responseCode: Int, purchasesList: List<Purchase>?) {
        Timber.d("onPurchasesUpdated, response code: $responseCode")
        when (responseCode) {
            BillingClient.BillingResponse.OK -> {
                if (purchasesList == null) {
                    Timber.d("Purchase update: No purchases")
                    handlePurchases(null)
                } else {
                    handlePurchases(purchasesList)
                }
            }
            BillingClient.BillingResponse.USER_CANCELED -> {
                Timber.i("User canceled the purchase")
            }
            BillingClient.BillingResponse.ITEM_ALREADY_OWNED -> {
                Timber.i("The user already owns this item")
            }
            BillingClient.BillingResponse.DEVELOPER_ERROR -> {
                Timber.e("Developer error means that Google Play does not recognize the " +
                        "configuration. If you are just getting started, make sure you have " +
                        "configured the application correctly in the Google Play Console. " +
                        "The SKU product ID must match and the APK you are using must be " +
                        "signed with release keys.")
            }
            else -> {
                Timber.e("See error code in BillingClient.BillingResponse: $responseCode")
            }
        }
    }

    /**
     * Check if purchases have changed before updating other part of the app.
     */
    private fun handlePurchases(purchasesList: List<Purchase>?) {
        if (isUnchangedPurchaseList(purchasesList)) {
            Timber.d("Same ${purchasesList?.size} purchase(s), no need to post an update to the live data")
        } else {
            Timber.d("Handling ${purchasesList?.size} purchase(s)")
            updatePurchases(purchasesList)
        }
    }

    private fun isUnchangedPurchaseList(purchasesList: List<Purchase>?): Boolean {
        return false
    }

    private fun updatePurchases(purchasesList: List<Purchase>?) {
        Timber.i("updatePurchases: ${purchasesList?.size} purchase(s)")
        purchaseUpdateEvent.postValue(purchasesList)
        purchases.postValue(purchasesList)
    }

    fun launchBillingFlow(activity: Activity, params: BillingFlowParams): Int {
        val sku = params.sku
        val oldSku = params.oldSku
        Timber.i("Launching billing flow wth sku: $sku, oldSkus: $oldSku")

        if (!billingClient.isReady) {
            Timber.e("BillingClient is not ready to start billing flow")
        }

        val responseCode = billingClient.launchBillingFlow(activity, params)
        Timber.d("Launch Billing Flow Response Code: $responseCode")
        return responseCode
    }
}