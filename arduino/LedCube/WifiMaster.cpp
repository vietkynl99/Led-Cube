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
    AsyncWiFiManager::setAPInformation(AP_SSID, AP_PASSWORD);
    AsyncWiFiManager::setMDnsServerName(MDNS_HOSTNAME);

    AsyncWiFiManager::begin();
}

void WifiMaster::resetWifiSettings()
{
    LOG_WIFI("Reset WIFI settings");
    AsyncWiFiManager::resetSettings();
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
            HardwareController::getInstance()->beep(2);
        }
        pre_status = status;
    }
}

void WifiMaster::loop()
{
    AsyncWiFiManager::loop();
    checkWifiStatus();
}