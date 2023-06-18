#ifndef _WIFI_MASTER_H_
#define _WIFI_MASTER_H_

#include <EEPROM.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <WiFiManager.h>
#include <ESP8266WebServer.h>
#include "VLog.h"

// Wifimanager can run in a blocking mode or a non blocking mode
// Be sure to know how to process loops with no delay() if using non blocking
#define WM_NONBLOCKING_MODE 1

/* AP mode */
#define AP_SSID "Kynl Led Cube"
#define AP_PASSWORD "12345678"

class WifiMaster
{
private:
    WiFiManager wm;

public:
    WifiMaster();
    void printConnectedWifiInfo();
    void init();
    void resetWifiSettings();
    void process();
};

#endif