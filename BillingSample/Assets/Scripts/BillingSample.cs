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

/**
 * Billingサンプル用.アプリ内課金サンプルのTrivialDriveをUnityに移植した.
 */
public class BillingSample : MonoBehaviour
{
    public Texture tex_buy_gas;
    public Texture tex_drive;
    public Texture tex_free;
    public Texture tex_gas0;
    public Texture tex_gas1;
    public Texture tex_gas2;
    public Texture tex_gas3;
    public Texture tex_gas4;
    public Texture tex_gas_inf;
    public Texture tex_infinite_gas;
    public Texture tex_premium;
    public Texture tex_title;
    public Texture tex_upgrade_app;
    public Texture tex_wait;

    private int mTank;

    private bool mPremium;
    private bool mInfinite;

    private const string SKU_GAS = "gas";
    private const string SKU_PREMIUM = "premium";
    private const string SKU_INFINITE = "infinite_gas";

    private string gasProductDetail = "";
    private string premiumProductDetail = "";
    private string infiniteProductDetail = "";

    private const int TANK_MAX = 4;

    private bool mShowLabel;
    private string mErrorMessage = "";

    public string publicKey = "";

    void Start()
    {
        LoadData();

        // 最初にコールバックの設定と初期化処理を実行.結果はコールバックに通知される.
        GetComponent<BillingPlugin>().SetCallback(Callback);
        GetComponent<BillingPlugin>().Init(publicKey, SKU_GAS + "," + SKU_PREMIUM, SKU_INFINITE);
    }

    void Update()
    {
        if (Application.platform == RuntimePlatform.Android)
        {
            if (Input.GetKey(KeyCode.Escape))
            {
                Application.Quit();
            }
        }
    }

    private void LoadData()
    {
        mTank = PlayerPrefs.GetInt(SKU_GAS, 2);
    }

    private void SaveData()
    {
        PlayerPrefs.SetInt(SKU_GAS, mTank);
    }

    /**
     * Java層からの結果を受け取る.
     */
    public void Callback(string action, bool success, string productId)
    {
        Debug.Log("BillingSample Callback called" + action);

        switch (action)
        {
        case "Init":
            if (success)
            {
                InitBillingData();
            }
            else
            {
                SetPopupLabel(true, "In app billing plugin iniialize error");
            }
            break;
        case "Purchase":
            if (success)
            {
                if (productId.Contains(SKU_GAS))
                {
                    mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
                    SaveData();
                    SetPopupLabel(true, "You filled 1/4 tank. Your tank is now " + mTank.ToString() + "/4 full!");
                }
                else if (productId.Contains(SKU_PREMIUM))
                {
                    mPremium = true;
                    SetPopupLabel(true, "Thank you for upgrading to premium!");
                }
                else if (productId.Contains(SKU_INFINITE))
                {
                    mInfinite = true;
                    mTank = TANK_MAX;
                    SetPopupLabel(true, "Thank you for subscribing to infinite gas!");
                }
                else
                {
                    SetPopupLabel(true, "Product ID Not Found : " + productId);
                }
            }
            else
            {
            }
            break;
        }
    }

    /**
     * 初期化の完了後に購入済みデータを復旧.課金アイテム情報を取得.
     */
    private void InitBillingData()
    {
        if (GetComponent<BillingPlugin>().GetPurchaseData(SKU_PREMIUM, false))
        {
            mPremium = true;
        }
        if (GetComponent<BillingPlugin>().GetPurchaseData(SKU_INFINITE, false))
        {
            mInfinite = true;
            mTank = TANK_MAX;
        }
        if (GetComponent<BillingPlugin>().GetPurchaseData(SKU_GAS, true))
        {
            mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
            SaveData();
            SetPopupLabel(true, "You filled 1/4 tank. Your tank is now " + mTank.ToString() + "/4 full!");
        }

        gasProductDetail = GetComponent<BillingPlugin>().GetProductTitle(SKU_GAS) + "\n" +
                           GetComponent<BillingPlugin>().GetProductDescription(SKU_GAS) + "\n" +
                           GetComponent<BillingPlugin>().GetProductPrice(SKU_GAS) + "\n";

        infiniteProductDetail = GetComponent<BillingPlugin>().GetProductTitle(SKU_INFINITE) + "\n" +
                                GetComponent<BillingPlugin>().GetProductDescription(SKU_INFINITE) + "\n" +
                                GetComponent<BillingPlugin>().GetProductPrice(SKU_INFINITE) + "\n";

        premiumProductDetail = GetComponent<BillingPlugin>().GetProductTitle(SKU_PREMIUM) + "\n" +
                               GetComponent<BillingPlugin>().GetProductDescription(SKU_PREMIUM) + "\n" +
                               GetComponent<BillingPlugin>().GetProductPrice(SKU_PREMIUM) + "\n";
    }

    void OnGUI()
    {

        int width = Screen.width;
        int height = Screen.height;

        if (mShowLabel)
        {
            // 何かのメッセージを表示.
            ShowPopupLabel(width, height);
        }
        else
        {
            // 通常時.
            GUI.Label(new Rect(width * 0.375f, height * 0f, width * 0.25f, height * 0.125f), tex_title);

            ShowCarLabel(width, height);

            ShowGasLabel(width, height);

            if (GUI.Button(new Rect(width * 0.25f, height * 0.5f, width * 0.25f, height * 0.25f), tex_drive))
            {
                Drive();
            }

            if (GUI.Button(new Rect(width * 0.5f, height * 0.5f, width * 0.25f, height * 0.25f), tex_buy_gas))
            {
                if (mInfinite)
                {
                    SetPopupLabel(true, "No need! You're subscribed to infinite gas. Isn't that awesome?");
                }
                else
                {
                    if (mTank >= TANK_MAX)
                    {
                        SetPopupLabel(true, "Your tank is full. Drive around a bit!");
                    }
                    else
                    {
                        bool result = GetComponent<BillingPlugin>().Purchase(SKU_GAS, true, "");
                        if (!result)
                        {
                            SetPopupLabel(true, "Error Gas Purchase");
                        }
                    }
                }
            }
            if (!mPremium)
            {
                if (GUI.Button(new Rect(width * 0.25f, height * 0.75f, width * 0.25f, height * 0.25f), tex_upgrade_app))
                {
                    bool result = GetComponent<BillingPlugin>().Purchase(SKU_PREMIUM, false, "");
                    if (!result)
                    {
                        SetPopupLabel(true, "Error Premium Purchase");
                    }
                }
                GUI.Label(new Rect(width * 0.5f, height * 0.5f, width * 0.25f, height * 0.25f), gasProductDetail);
            }
            GUI.Label(new Rect(width * 0.25f, height * 0.75f, width * 0.25f, height * 0.25f), premiumProductDetail);
            if (GUI.Button(new Rect(width * 0.5f, height * 0.75f, width * 0.25f, height * 0.25f), tex_infinite_gas))
            {
                if (mInfinite)
                {
                    GetComponent<BillingPlugin>().StartPlayStore();
                }
                else
                {
                    bool result = GetComponent<BillingPlugin>().PurchaseSubscription(SKU_INFINITE, "");
                    if (!result)
                    {
                        SetPopupLabel(true, "Error Infinite_gas Purchase");
                    }
                }
            }
            GUI.Label(new Rect(width * 0.5f, height * 0.75f, width * 0.25f, height * 0.25f), infiniteProductDetail);
        }
    }

    private void ShowCarLabel(int width, int height)
    {
        if (mPremium)
        {
            GUI.Label(new Rect(width * 0.375f, height * 0.125f, width * 0.25f, height * 0.25f), tex_premium);
        }
        else
        {
            GUI.Label(new Rect(width * 0.375f, height * 0.125f, width * 0.25f, height * 0.25f), tex_free);
        }
    }

    private void ShowGasLabel(int width, int height)
    {
        Texture gasTexture = null;
        if (mInfinite)
        {
            gasTexture = tex_gas_inf;
        }
        else
        {
            switch (mTank)
            {
            case 0:
                gasTexture = tex_gas0;
                break;
            case 1:
                gasTexture = tex_gas1;
                break;
            case 2:
                gasTexture = tex_gas2;
                break;
            case 3:
                gasTexture = tex_gas3;
                break;
            case 4:
                gasTexture = tex_gas4;
                break;
            default:
                gasTexture = tex_gas0;
                break;
            }
        }
        GUI.Label(new Rect(width * 0.375f, height * 0.375f, width * 0.25f, height * 0.125f), gasTexture);
    }

    private void SetPopupLabel(bool error, string message)
    {
        mShowLabel = error;
        mErrorMessage = message;
    }

    private void ShowPopupLabel(int width, int height)
    {
        GUI.Box(new Rect(width * 0.25f, height * 0.25f, width * 0.5f, height * 0.5f), mErrorMessage);
        if (GUI.Button(new Rect(width * 0.375f, height * 0.4f, width * 0.25f, height * 0.25f), "OK"))
        {
            SetPopupLabel(false, "");
        }
    }

    private void Drive()
    {
        Debug.Log("Drive button clicked.");
        if (!mInfinite && mTank <= 0)
        {
            SetPopupLabel(true, "Oh, no! You are out of gas! Try buying some!");
        }
        else
        {
            if (!mInfinite)
                --mTank;
            SaveData();
            SetPopupLabel(true, "Vroooom, you drove a few miles.");
        }
    }
}
