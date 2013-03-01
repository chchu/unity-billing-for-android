using UnityEngine;
using System.Collections;

using Callback = System.Action<string>;

public class BillingPlugin : MonoBehaviour
{
    public string publicKey = "";
    private AndroidJavaObject androidPlugin;

    private Callback callback;
    // Use this for initialization
    void Start ()
    {
        Debug.Log("BillingPlugin Start called ");
        androidPlugin = new AndroidJavaObject("com.redoceanred.unity.android.BillingPlugin");
        androidPlugin.Call("initPlugin", publicKey, gameObject.name);
    }

    public void SetCallback(Callback cb = null)
    {
        Debug.Log("BillingPlugin SetCallback called");
        callback = cb;
    }

    public void Purchase(string name, bool consume, string payload)
    {
        bool result = androidPlugin.Call<bool>("purchaseInApp", name, consume, payload);
        Debug.Log("BillingPlugin purchaseInApp result = " + result);
    }

    // Callback.
    public void InitMessage(string message)
    {
        Debug.Log("InitMessage Called " + message);
    }

    public void PurchaseMessage(string message)
    {
        Debug.Log("PurchaseMessage Called " + message);
    }
}
