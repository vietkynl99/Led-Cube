#ifndef _SERVICE_MANAGER_H_
#define _SERVICE_MANAGER_H_

#include <NTPClient.h>
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

#define NTP_TIME_OFFSET (25200)          // UTC+7 (Vietnam)
// #define NTP_UPDATE_INTERVAL (15 * 60000) // ms

class ServiceManager
{
public:
    WiFiUDP ntpUDP;
    NTPClient *timeClient;

public:
    ServiceManager();
    void init();
    void updateRealTime();

    String getRealTime();
};

#endif