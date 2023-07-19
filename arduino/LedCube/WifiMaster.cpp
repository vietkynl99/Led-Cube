#include <Arduino.h>
#include "WifiMaster.h"

WifiMaster *WifiMaster::instance = nullptr;

WifiMaster *WifiMaster::getInstance()
{
    if (instance == nullptr)
    {
        instance = new WifiMaster();
    }
    return instance;
}

WifiMaster::WifiMaster()
{
}

void WifiMaster::printConnectedWifiInfo()
{
    LOG_WIFI("SSID: '%s' IP: '%s'", WiFi.SSID().c_str(), WiFi.localIP().toString().c_str());
}

void WifiMaster::init()
{
    WiFi.mode(WIFI_STA); // explicitly set mode, esp defaults to STA+AP
    delay(500);

    LOG_SYSTEM("Starting");

    // wm.resetSettings(); // wipe settings

#if WM_NONBLOCKING_MODE
    wm.setConfigPortalBlocking(false);
#endif

    // custom menu via array or vector
    // menu tokens, "wifi","wifinoscan","info","param","close","sep","erase","restart","exit" (sep is seperator) (if param is in menu, params will not show up in wifi page!)
    std::vector<const char *> menu = {"wifi", "param", "sep", "restart", "exit"};
    wm.setMenu(menu);

    // set dark theme
    // wm.setClass("invert");

    // set static ip
    //  wm.setSTAStaticIPConfig(IPAddress(10,0,1,99), IPAddress(10,0,1,1), IPAddress(255,255,255,0)); // set static ip,gw,sn
    //  wm.setShowStaticFields(true); // force show static ip fields
    //  wm.setShowDnsFields(true);    // force show dns field always

    // wm.setConnectTimeout(20); // how long to try to connect for before continuing
    wm.setConfigPortalTimeout(30); // auto close configportal after n seconds
    // wm.setCaptivePortalEnable(false); // disable captive portal redirection
    // wm.setAPClientCheck(true); // avoid timeout if client connected to softap

    // wifi scan settings
    // wm.setRemoveDuplicateAPs(false); // do not remove duplicate ap names (true)
    // wm.setMinimumSignalQuality(20);  // set min RSSI (percentage) to show in scans, null = 8%
    // wm.setShowInfoErase(false);      // do not show erase button on info page
    // wm.setScanDispPerc(true);       // show RSSI as percentage not graph icons

    // wm.setBreakAfterConfig(true);   // always exit configportal even if wifi save fails

    // res = wm.autoConnect(); // auto generated AP name from chipid
    // res = wm.autoConnect("AutoConnectAP"); // anonymous ap
    bool res = wm.autoConnect(AP_SSID, AP_PASSWORD); // password protected ap

    if (!res)
    {
        LOG_WIFI("Failed to connect or hit timeout");
        // ESP.restart();
    }
    else
    {
        // if you get here you have connected to the WiFi
        LOG_WIFI("Wifi connected!!!");
        printConnectedWifiInfo();
    }
}

void WifiMaster::resetWifiSettings()
{
    LOG_WIFI("Reset WIFI settings");
    wm.resetSettings();
    ESP.restart();

    // start portal w delay
    LOG_WIFI("Starting config portal");
    wm.setConfigPortalTimeout(120);

    if (!wm.startConfigPortal("OnDemandAP", "password"))
    {
        LOG_WIFI("Failed to connect or hit timeout");
        delay(3000);
        // ESP.restart();
    }
    else
    {
        // if you get here you have connected to the WiFi
        LOG_WIFI("Wifi connected!!!");
        printConnectedWifiInfo();
    }
}

void WifiMaster::process()
{
#if WM_NONBLOCKING_MODE
    // Avoid delays() in loop when non-blocking and other long running code
    wm.process();
#endif
}