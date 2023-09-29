#ifndef _WIFI_MASTER_H_
#define _WIFI_MASTER_H_

#include <Arduino.h>
#include <EEPROM.h>
#include "HardwareController.h"
#include <ESP_WiFiManager_Lite.h>
#include "VLog.h"

/* AP mode */
#define AP_SSID                 "Led Cube"
#define AP_PASSWORD             "12345678"

#define DHCP_HOSTNAME           "Led-Cube"

class WifiMaster
{
private:
    static WifiMaster* instance;
    ESP_WiFiManager_Lite *wifiManager;

private:
    WifiMaster();

public:
    static WifiMaster* getInstance();
    void printConnectedWifiInfo();
    void init();
    void resetWifiSettings();
    void checkWifiStatus();
    void process();
};

#endif