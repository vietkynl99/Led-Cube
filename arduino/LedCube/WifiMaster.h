#ifndef _WIFI_MASTER_H_
#define _WIFI_MASTER_H_

#include <Arduino.h>
#include <EEPROM.h>
#include <AsyncWiFiManager.h>
#include "HardwareController.h"
#include "VLog.h"

/* AP mode */
#define AP_SSID                 "Led Cube"
#define AP_PASSWORD             "12345678"

#define MDNS_HOSTNAME           "ledcube"

class WifiMaster
{
private:
    static WifiMaster* instance;

private:
    WifiMaster();

public:
    static WifiMaster* getInstance();
    void printConnectedWifiInfo();
    void init();
    void resetWifiSettings();
    void checkWifiStatus();
    void loop();
};

#endif