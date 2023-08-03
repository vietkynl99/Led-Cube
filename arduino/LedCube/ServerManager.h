#ifndef _SERVER_MANAGER_H
#define _SERVER_MANAGER_H

#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include "VLog.h"
#include "HardwareController.h"
#include "LedManager.h"

/* API KEY */
#define API_KEY_MIN 10000000UL
#define API_KEY_MAX 99999999UL

/* Device name */
#define DEVICE_NAME "ledCube"

/* WebServer Port */
#define WEB_SERVER_PORT 51308

/* mDNS domain name */
// the fully-qualified domain name is "ledCube.local"
#define mDNS_DOMAIN DEVICE_NAME

#define JSON_BYTE_MAX 200

enum EventType
{
    EVENT_NONE = 100,
    /* Request from App to ESP */
    EVENT_REQUEST_CHECK_CONNECTION = 200,
    EVENT_REQUEST_PAIR_DEVICE,
    EVENT_REQUEST_SEND_DATA,
    /* Response from ESP to App */
    EVENT_RESPONSE_CHECK_CONNECTION = 300,
    EVENT_RESPONSE_INVALID_KEY,
    EVENT_RESPONSE_PAIR_DEVICE_IGNORED,
    EVENT_RESPONSE_PAIR_DEVICE_PAIRED,
    EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL,
    EVENT_RESPONSE_GET_DATA_SUCCESSFUL,
    EVENT_RESPONSE_UPDATE_DATA
};

class ServerManager
{
public:
    static ESP8266WebServer server;
    static long apiKey;
    static int batteryLevel;

    static void init();
    static void checkWifiStatus();
    static String generateJson(String key, String value);
    static void sendResponse(int type, String data);
    static void sendResponse(int type, String dataKey, String dataValue);
    static void sendResponse(int type);
    static void sendInvalidResponse();
    static void pairDevice(long oldKey);
    static void dataProcessing(String data);
    static void handleRequest();
    static void process();

    static void saveApiKeyToEEPROM();
    static void loadApiKeyFromEEPROM();
    static void resetApiKey();
};

#endif