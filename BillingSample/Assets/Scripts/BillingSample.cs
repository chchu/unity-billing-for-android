using UnityEngine;
using System.Collections;

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

    void OnGUI()
    {
        int width = Screen.width;
        int height = Screen.height;
        if (GUI.Button(new Rect(0, height * 0.5f, width * 0.25f, height * 0.25f), tex_buy_gas))
        {
            GetComponent<BillingPlugin>().Purchase("gas", true, "");
        }

    }
}
