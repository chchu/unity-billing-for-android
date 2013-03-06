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

import java.util.Arrays;
import java.util.List;

import com.redoceanred.android.billing.util.IabHelper;
import com.redoceanred.android.billing.util.IabResult;
import com.redoceanred.android.billing.util.Inventory;
import com.redoceanred.android.billing.util.Purchase;
import com.redoceanred.android.billing.util.SkuDetails;
import com.redoceanred.unity.android.activity.BillingNativeActivity;
import com.unity3d.player.UnityPlayer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * アプリ内課金Plugin.
 * 
 * @author keiichiro
 */
public class BillingPlugin {

	private static final String TAG = BillingPlugin.class.getSimpleName();
	/**
	 * Unity層へのCallbackクラス.
	 */
	private BillingPluginCallback mCallBack;
	/**
	 * 課金アイテムのID.
	 */
	private String mProductId;

	/**
	 * プラグインの内部状態.
	 */
	private enum PurchaseState {
		/**
		 * 初期化中.
		 */
		Initialize,
		/**
		 * アイドル.
		 */
		Idle,
		/**
		 * 消費アイテム購入処理中.
		 */
		ConsumablePurchase,
		/**
		 * 非消費アイテム購入処理中.
		 */
		NoConSumablePurchase,
		/**
		 * サブスクリプション(期間)アイテム購入処理中.
		 */
		SubscriptionPurchase,
		/**
		 * {@link BillingPlugin}getPurchaseData()にて消費アイテムの課金情報を復帰中.(消費処理中)
		 */
		ConsumeRestorePurchaseData
	}

	/**
	 * プラグインの内部状態.
	 */
	private PurchaseState mPurchaseState = PurchaseState.Initialize;

	/**
	 * プラグインの初期化を実行.
	 * 
	 * @param publicKey
	 *            アプリのイセンスキー.
	 * @param inapp
	 *            In-app ProductsのID文字列.
	 *            ex gas,premium
	 * @param inapp
	 *            Subscription ProductsのID文字列.
	 *            ex infinite_gas
	 * @param gameObject
	 *            Pluginを使用しているGame Object名.Callback対象となる.
	 */
	public void initPlugin(String publicKey, String inapp, String subs, String gameObject) {
		Log.d(TAG, "Called initPlugin " + inapp + " " + subs +  gameObject);
		
		mCallBack = new BillingPluginCallback(gameObject);
		if (publicKey == null || publicKey.equals("")) {
			mCallBack.initMessage(false);
		} else {
			initBilling(publicKey, inapp, subs);
		}
	}

	/**
	 * 通常アイテムの購入処理を実行する.
	 * 
	 * @param productId
	 *            課金アイテムのID.
	 * @param consume
	 *            true 消費アイテム, false 非消費アイテム.
	 * @param payload
	 *            verify用文字列.
	 * @return true 成功, false 失敗.
	 */
	public boolean purchaseInApp(String productId, boolean consume, String payload) {
		Log.d(TAG, "Called purchase : " + productId + "  consume : " + consume);
		if (!mPurchaseState.equals(PurchaseState.Idle)) {
			// Processing Purchase or initialize. 何か処理中の場合は購入処理不可.
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
	
	/**
	 * Subscriptionアイテム(定期購入)のサポート.
	 * @return true サポート,false 非サポート.
	 */
	public boolean getSubscriptionsSupported() {
		if (mPurchaseState.equals(PurchaseState.Initialize)) {
			return false;
		}
		return mHelper.subscriptionsSupported();
	}

	/**
	 * Subscriptionアイテムの購入処理を実行する.
	 * 
	 * @param productId
	 *            課金アイテムのID.
	 * @param payload
	 *            verify用文字列.
	 * @return true 成功, false 失敗.
	 */
	public boolean purchaseSubscription(String productId, String payload) {
		if (!mPurchaseState.equals(PurchaseState.Idle)) {
			// Processing Purchase.
			return false;
		}
		if (!mHelper.subscriptionsSupported()) {
			// Subscription Not Support.
			return false;
		}
		
		mPurchaseState = PurchaseState.SubscriptionPurchase;
		mProductId = productId;
		mHelper.launchPurchaseFlow(UnityPlayer.currentActivity, productId, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, mPurchaseFinishedListener, payload);
		return true;
	}

	/**
	 * 初期化処理後にアプリの購入済みアイテム情報を取得する.
	 * 
	 * @param productId
	 *            課金アイテムのID.
	 * @param consume
	 *            true 消費アイテム, false 非消費アイテム.
	 * @return true 成功, false 失敗.
	 */
	public boolean getPurchaseData(String productId, boolean consume) {
		if (!mPurchaseState.equals(PurchaseState.Idle)) {
			return false;
		}

		final Purchase purchase = mInventory.getPurchase(productId);
		if (purchase != null) {
			if (consume) {
				UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						mHelper.consumeAsync(purchase, mConsumeFinishedListener);
					}
				});
				mPurchaseState = PurchaseState.ConsumeRestorePurchaseData;
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * InAppBillingServiceのアイテム情報を取得する.
	 * @param productId 課金アイテムのID.
	 * @return アイテム情報(Json).
	 *         ref. http://developer.android.com/google/play/billing/billing_reference.html#getSkuDetails
	 */
	public String getProductDetail(String productId) {
		SkuDetails d = mInventory.getSkuDetails(productId);
		if (d != null) {
			return d.getJson();
		} else {
			return "";
		}
	}

	/**
	 * アイテムのPriceを取得する.
	 * @param productId 課金アイテムのID.
	 * @return Price.
	 */
	public String getProductPrice(String productId) {
		SkuDetails d = mInventory.getSkuDetails(productId);
		if (d != null) {
			return d.getPrice();
		} else {
			return "";
		}
	}

	/**
	 * アイテムのTitleを取得する.
	 * @param productId 課金アイテムのID.
	 * @return Title.
	 */
	public String getProductTitle(String productId) {
		SkuDetails d = mInventory.getSkuDetails(productId);
		if (d != null) {
			return d.getTitle();
		} else {
			return "";
		}
	}

	/**
	 * アイテムのDescriptionを取得する.
	 * @param productId 課金アイテムのID.
	 * @return Description.
	 */
	public String getProductDescription(String productId) {
		SkuDetails d = mInventory.getSkuDetails(productId);
		if (d != null) {
			return d.getDescription();
		} else {
			return "";
		}
	}

	/**
	 * Playストアアプリを起動する.Subscription解約時に起動する.
	 */
	public void startPlayStore() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + UnityPlayer.currentActivity.getPackageName()));
		try {
			UnityPlayer.currentActivity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, e.toString());
		}
	}

	/**
	 * Acvitityからの実行結果をhandleする.
	 */
	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		return mHelper.handleActivityResult(requestCode, resultCode, data);
	}

	/**
	 * リリース処理を実行する.
	 */
	public void dispose() {
		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}

	// 以下Billingサンプルからの転載.
	private IabHelper mHelper;
	private Inventory mInventory;
	static final int RC_REQUEST = 10001;

	public void initBilling(String publicKey, final String inappSkus, final String subsSkus) {
		mHelper = new IabHelper(UnityPlayer.currentActivity, publicKey);
		mHelper.enableDebugLogging(true);

		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					mCallBack.initMessage(false);
					return;
				}
				// In App アイテムIDをListに変換.
				List<String> inappArray = null;
				if (inappSkus != null) {
					inappArray = Arrays.asList(inappSkus.split(","));
				}
				// Subscription アイテムIDをListに変換.
				List<String> subsArray = null;
				if (subsSkus != null) {
					subsArray = Arrays.asList(subsSkus.split(","));
				}
				// アイテム情報を取得.
				mHelper.queryInventoryAsync(true, inappArray, subsArray, mGotInventoryListener);
			}
		});
	}

	// 初期化完了通知.
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			if (result.isFailure()) {
				mCallBack.initMessage(false);
				return;
			}
			
			// 情報を内部に保持する.
			mInventory = inventory;
			initState();

			if (UnityPlayer.currentActivity instanceof BillingNativeActivity) {
				// ActivityにPluginのインスタンスを登録.
				BillingNativeActivity nativeActivity = (BillingNativeActivity) UnityPlayer.currentActivity;
				nativeActivity.setBillingPlugin(BillingPlugin.this);
			}

			mCallBack.initMessage(true);
		}
	};

	// 購入完了イベントの通知.
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
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

			if (mPurchaseState.equals(PurchaseState.ConsumablePurchase)) {
				// 消費アイテムの場合はGoogle側の消費を行う.
				mHelper.consumeAsync(purchase, mConsumeFinishedListener);
			} else {
				// purcase success event.
				mCallBack.purchaseMessage(purchase.getSku(), true, purchase.getDeveloperPayload());
				initState();
			}
		}
	};

	// 消費完了イベントの通知.
	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			if (mPurchaseState.equals(PurchaseState.ConsumeRestorePurchaseData)) {
				initState();
				return;
			}

			if (result.isSuccess()) {
				// purcase success event.
				mCallBack.purchaseMessage(purchase.getSku(), true, purchase.getDeveloperPayload());
			} else {
				// purcase failed event.
				mCallBack.purchaseMessage(purchase.getSku(), false);
			}
			initState();
		}
	};
	
	// 状態を初期化.
	private void initState() {
		mPurchaseState = PurchaseState.Idle;
		mProductId = null;
	}
}
