/*
 * Copyright 2013 nishino.keiichiro@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redoceanred.unity.android;

import com.redoceanred.android.billing.util.IabHelper;
import com.redoceanred.android.billing.util.IabResult;
import com.redoceanred.android.billing.util.Inventory;
import com.redoceanred.android.billing.util.Purchase;
import com.redoceanred.unity.android.activity.BillingNativeActivity;
import com.unity3d.player.UnityPlayer;

import android.content.Intent;
import android.util.Log;

class BillingPluginCallback {
	private String mGameObject;

	public BillingPluginCallback(final String gameObject) {
		mGameObject = gameObject;
	}

	public void initMessage(boolean success) {
		UnityPlayer.UnitySendMessage(mGameObject, "InitMessage", Boolean.toString(success));
	}

	public void purchaseMessage(String productId, boolean success) {
		purchaseMessage(productId, success, "");
	}

	public void purchaseMessage(String productId, boolean success, String payload) {
		UnityPlayer.UnitySendMessage(mGameObject, "PurchaseMessage", productId + "," + Boolean.toString(success) + "," + payload);
	}

	public void asyncPurchaseMessage(final String productId, final boolean success) {
		asyncPurchaseMessage(productId, success, "");
	}

	public void asyncPurchaseMessage(final String productId, final boolean success, final String payload) {
		(new Thread(new Runnable() {

			@Override
			public void run() {
				UnityPlayer.UnitySendMessage(mGameObject, "PurchaseMessage", productId + "," + Boolean.toString(success) + "," + payload);
			}
		})).start();
	}
}

public class BillingPlugin {

	private static final String TAG = BillingPlugin.class.getSimpleName();
	private BillingPluginCallback mCallBack;
	private String mProductId;

	private enum PurchaseState {
		Initialize, Idle, ConsumablePurchase, NoConSumablePurchase, SubscriptionPurchase
	}

	private PurchaseState mPurchaseState = PurchaseState.Initialize;

	public void initPlugin(String publicKey, String gameObject) {
		Log.e(TAG, "Called initPlugin " + gameObject);
		initBilling(publicKey);
		mCallBack = new BillingPluginCallback(gameObject);
	}

	public boolean purchaseInApp(String productId, boolean consume, String payload) {
		Log.e(TAG, "Called purchase : " + productId + "  consume : " + consume);
		if (!mPurchaseState.equals(PurchaseState.Idle)) {
			// Processing Purchase or initialize.
			return false;
		}

		if (consume) {
			mPurchaseState = PurchaseState.ConsumablePurchase;
		} else {
			mPurchaseState = PurchaseState.NoConSumablePurchase;
		}
		mProductId = productId;
		mHelper.launchPurchaseFlow(UnityPlayer.currentActivity, productId, RC_REQUEST, mPurchaseFinishedListener, payload);
		return true;
	}

	public boolean purchaseSubscription(String productId, String payload) {
		if (!mHelper.subscriptionsSupported()) {
			// Subscription Not Support.
			mCallBack.asyncPurchaseMessage(productId, false);
			return false;
		}
		if (!mPurchaseState.equals(PurchaseState.Idle)) {
			// Processing Purchase.
			mCallBack.asyncPurchaseMessage(productId, false);
			return false;
		}

		mPurchaseState = PurchaseState.SubscriptionPurchase;
		mProductId = productId;
		mHelper.launchPurchaseFlow(UnityPlayer.currentActivity, productId, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, mPurchaseFinishedListener, payload);
		return true;
	}

	public String getPurchaseData(String productId, boolean consume) {
		if (!mPurchaseState.equals(PurchaseState.Idle)) {
			return "";
		}

		Purchase purchase = mInventory.getPurchase(productId);
		if (purchase != null) {
			if (consume) {
				mHelper.consumeAsync(purchase, mConsumeFinishedListener);
				mPurchaseState = PurchaseState.ConsumablePurchase;
			}
			return productId + "," + Boolean.toString(true) + "," + purchase.getDeveloperPayload();
		} else {
			return productId + "," + Boolean.toString(false);
		}
	}

	private void initState() {
		mPurchaseState = PurchaseState.Idle;
		mProductId = null;
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		return mHelper.handleActivityResult(requestCode, resultCode, data);
	}

	public void dispose() {
		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}

	private IabHelper mHelper;
	private Inventory mInventory;

	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;

	public void initBilling(String publicKey) {
		// Create the helper, passing it our context and the public key to
		// verify signatures with
		Log.d(TAG, "Creating IAB helper.");
		mHelper = new IabHelper(UnityPlayer.currentActivity, publicKey);

		// enable debug logging (for a production application, you should set
		// this to false).
		mHelper.enableDebugLogging(true);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					mCallBack.initMessage(false);
					return;
				}

				// Hooray, IAB is fully set up. Now, let's get an inventory of
				// stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});
	}

	// Listener that's called when we finish querying the items and
	// subscriptions we own
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");
			if (result.isFailure()) {
				return;
			}
			Log.d(TAG, "Query inventory was successful.");

			mInventory = inventory;
			initState();

			if (UnityPlayer.currentActivity instanceof BillingNativeActivity) {
				BillingNativeActivity nativeActivity = (BillingNativeActivity) UnityPlayer.currentActivity;
				nativeActivity.setBillingPlugin(BillingPlugin.this);
			}

			mCallBack.initMessage(true);
			Log.d(TAG, "Initial inventory query finished; enabling main UI.");
		}
	};

	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
			if (result.isFailure()) {
				String sku = null;
				if (purchase == null) {
					sku = mProductId;
				} else {
					sku = purchase.getSku();
				}
				mCallBack.purchaseMessage(sku, false);
				initState();
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (mPurchaseState.equals(PurchaseState.ConsumablePurchase)) {
				mHelper.consumeAsync(purchase, mConsumeFinishedListener);
			} else {
				// purcase success event.
				mCallBack.purchaseMessage(purchase.getSku(), true, purchase.getDeveloperPayload());
				initState();
			}
		}
	};

	// Called when consumption is complete
	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

			if (result.isSuccess()) {
				// purcase success event.
				mCallBack.purchaseMessage(purchase.getSku(), true, purchase.getDeveloperPayload());
			} else {
				// purcase failed event.
				mCallBack.purchaseMessage(purchase.getSku(), false);
			}
			initState();
			Log.d(TAG, "End consumption flow.");
		}
	};
}
