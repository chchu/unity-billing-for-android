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

import com.unity3d.player.UnityPlayer;

/**
 * UnityへのCallbackクラス.
 */
public class BillingPluginCallback {
	private String mGameObject;

	public BillingPluginCallback(final String gameObject) {
		mGameObject = gameObject;
	}

	/**
	 * 初期化処理の結果を通知する.
	 * @param success true 成功, false 失敗.
	 */
	public void initMessage(boolean success) {
		UnityPlayer.UnitySendMessage(mGameObject, "InitMessage", Boolean.toString(success));
	}

	/**
	 * 購入処理の結果を通知する.
	 * @param productId アイテムID.
	 * @param success true 成功, false 失敗.
	 */
	public void purchaseMessage(String productId, boolean success) {
		purchaseMessage(productId, success, "");
	}

	/**
	 * 購入処理の結果を通知する.
	 * @param productId アイテムID
	 * @param success true 成功, false 失敗.
	 * @param payload verify用文字列.
	 */
	public void purchaseMessage(String productId, boolean success, String payload) {
		UnityPlayer.UnitySendMessage(mGameObject, "PurchaseMessage", productId + "," + Boolean.toString(success) + "," + payload);
	}

	/**
	 * 購入処理の結果を通知する.(非同期)
	 * @param productId アイテムID
	 * @param success true 成功, false 失敗.
	 */
	public void asyncPurchaseMessage(final String productId, final boolean success) {
		asyncPurchaseMessage(productId, success, "");
	}

	/**
	 * 購入処理の結果を通知する.(非同期)
	 * @param productId アイテムID
	 * @param success true 成功, false 失敗.
	 * @param payload verify用文字列.
	 */
	public void asyncPurchaseMessage(final String productId, final boolean success, final String payload) {
		(new Thread(new Runnable() {

			@Override
			public void run() {
				UnityPlayer.UnitySendMessage(mGameObject, "PurchaseMessage", productId + "," + Boolean.toString(success) + "," + payload);
			}
		})).start();
	}
}
