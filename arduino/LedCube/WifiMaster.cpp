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
    wifiManager = new ESP_WiFiManager_Lite();

    // Set customized AP SSID and PWD
    wifiManager->setConfigPortal(AP_SSID, AP_PASSWORD);

    // Optional to change default AP IP(192.168.4.1) and channel(10)
    // wifiManager->setConfigPortalIP(IPAddress(192, 168, 120, 1));
    wifiManager->setConfigPortalChannel(0);

    // Set customized DHCP HostName
    wifiManager->begin(DHCP_HOSTNAME);
    // Or use default Hostname "ESP32-WIFI-XXXXXX"
    // wifiManager->begin();
}

void WifiMaster::resetWifiSettings()
{
    LOG_WIFI("Reset WIFI settings");
    wifiManager->resetAndEnterConfigPortal();
    // ESP.restart();
}

void WifiMaster::checkWifiStatus()
{
    static int pre_status = -1;
    int status = WiFi.status();
    if (status != pre_status)
    {
        LOG_WIFI("WiFi status changed to %d", status);
        if (status == WL_CONNECTED)
        {
            HardwareController::getInstance()->beep(1);
        }
        else if (pre_status < 0 || pre_status == WL_CONNECTED)
        {
            if (wifiManager->isConfigMode())
            {
                LOG_WIFI("Switch to config mode");
            }
            HardwareController::getInstance()->beep(2);
        }
        pre_status = status;
    }
}

void WifiMaster::process()
{
    wifiManager->run();
    checkWifiStatus();
}