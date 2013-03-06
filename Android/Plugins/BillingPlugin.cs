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
using UnityEngine;
using System.Collections;

public class BillingPlugin : MonoBehaviour
{
    public delegate void Callback(string acition, bool success, string payload);
    private Callback callback;
#if UNITY_ANDROID
    private AndroidJavaObject androidPlugin;
#endif
    /**
     * プラグインの初期化.Callbackが設定されていた場合はCallbackに結果を通知する.
     */
    public void Init(string publicKey, string inapp, string subs)
    {
        Debug.Log("BillingPlugin Init called ");
#if UNITY_ANDROID
        Debug.Log("BillingPlugin Init called android");
        androidPlugin = new AndroidJavaObject("com.redoceanred.unity.android.BillingPlugin");
        androidPlugin.Call("initPlugin", publicKey, inapp, subs, gameObject.name);
#endif
    }

    /**
     * コールバック(delegate)メソッドを設定する.
     */
    public void SetCallback(Callback cb)
    {
        Debug.Log("BillingPlugin SetCallback called");
        callback = cb;
    }

    /**
     * 購入処理を実行する.Callbackが設定されていた場合はCallbackに結果を通知する.
     * @return true 購入処理開始. false 購入処理失敗.
     */
    public bool Purchase(string name, bool consume, string payload)
    {
        bool result = false;
#if UNITY_ANDROID
        result = androidPlugin.Call<bool>("purchaseInApp", name, consume, payload);
#endif
        Debug.Log("BillingPlugin purchaseInApp result = " + result);
        return result;
    }

    /**
     * Subscriptionアイテム(定期購入)のサポートを取得.
     * @return true サポート,false 非サポート.
     */
    public bool GetSubscriptionSupported()
    {
        bool result = false;
#if UNITY_ANDROID
        result = androidPlugin.Call<bool>("getSubscriptionsSupported");
#endif
        return result;
    }

    /**
     * 購入処理(Subscription)を実行する.Callbackが設定されていた場合はCallbackに結果を通知する.
     * @return true 購入処理開始. false 購入処理失敗.
     */
    public bool PurchaseSubscription(string name, string payload)
    {
        bool result = false;
#if UNITY_ANDROID
        result = androidPlugin.Call<bool>("purchaseSubscription", name, payload);
#endif
        Debug.Log("BillingPlugin purchaseSubscription result = " + result);
        return result;
    }

    /**
     * 既に購入済みのデータを取得する.Initが完了したあとに呼び出す,
     * @return true 購入済み. false 未購入.
     */
    public bool GetPurchaseData(string name, bool consume)
    {
        bool result = false;
#if UNITY_ANDROID
        result = androidPlugin.Call<bool>("getPurchaseData", name, consume);
#endif
        Debug.Log("BillingPlugin GetPurchaseData result = " + result);
        return result;
    }

    /**
     * アイテムの詳細情報を取得する.
     * @param name 課金アイテムのID.
     * @return アイテム情報.
     */
    public string GetProductData(string name)
    {
        string result = "";
#if UNITY_ANDROID
        result = androidPlugin.Call<string>("getProductDetail", name);
#endif
        return result;
    }

    /**
     * アイテムのPriceを取得する.
     * @param name 課金アイテムのID.
     * @return Price.
     */
    public string GetProductPrice(string name)
    {
        string result = "";
#if UNITY_ANDROID
        result = androidPlugin.Call<string>("getProductPrice", name);
#endif
        return result;
    }

    /**
     * アイテムのTitleを取得する.
     * @param name 課金アイテムのID.
     * @return Title.
     */
    public string GetProductTitle(string name)
    {
        string result = "";
#if UNITY_ANDROID
        result = androidPlugin.Call<string>("getProductTitle", name);
#endif
        return result;
    }

    /**
     * アイテムのDescriptionを取得する.
     * @param name 課金アイテムのID.
     * @return Description.
     */
    public string GetProductDescription(string name)
    {
        string result = "";
#if UNITY_ANDROID
        result = androidPlugin.Call<string>("getProductDescription", name);
#endif
        return result;
    }

    /**
     * Playストアアプリを起動する.Subscription解約時に起動する.
     */
    public void StartPlayStore()
    {
#if UNITY_ANDROID
        androidPlugin.Call("startPlayStore");
#endif
    }

    /**
     * Verify処理.未実装.
     */
    private bool VerifyDeveloperPayload(string payload)
    {
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        return true;
    }

    /**
     * Callback処理.Java層から呼び出される.初期化処理の結果が通知される.
     * actionには"Init", successには成否が通知.
     */
    public void InitMessage(string message)
    {
        Debug.Log("InitMessage Called " + message);
        if (callback != null)
        {
            callback("Init", bool.Parse(message), "");
        }
    }

    /**
     * Callback処理.Java層から呼び出される.購入処理の結果が通知される.
     * actionには"Purchase", successには成否, productIdには購入したアイテムのIDが通知される.
     */
    public void PurchaseMessage(string message)
    {
        Debug.Log("PurchaseMessage Called " + message);
        if (callback != null)
        {
            string[] delimiter = { "," };
            string[] splitMessage = message.Split(delimiter, System.StringSplitOptions.None);

            if (bool.Parse(splitMessage[1]))
            {
                if (VerifyDeveloperPayload(splitMessage[2]))
                {
                    callback("Purchase", true, splitMessage[0]);
                }
                else
                {
                    callback("Purchase", false, splitMessage[0]);
                }
            }
            else
            {
                callback("Purchase", false, splitMessage[0]);
            }
        }
    }
}
