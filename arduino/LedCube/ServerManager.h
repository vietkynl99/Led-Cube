#ifndef _SERVER_MANAGER_H
#define _SERVER_MANAGER_H

#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <WebSocketsServer.h>
#include <Hash.h>
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

enum CommandType
{
    COMMAND_GAME_RIGHT,
    COMMAND_GAME_UP,
    COMMAND_GAME_LEFT,
    COMMAND_GAME_DOWN,
    COMMAND_GAME_START,
    COMMAND_MAX
};

class ServerManager
{
public:
    static WebSocketsServer *webSocket;
    static long apiKey;
    static int batteryLevel;

    static void init();
    static String generateJson(String key, String value);
    static void sendResponse(uint8_t id, int type, String data);
    static void sendResponse(uint8_t id, int type, String dataKey, String dataValue);
    static void sendResponse(uint8_t id, int type);
    static void pairDevice(uint8_t id, long oldKey);
    static void dataProcessing(String data);
    static void onSocketEvent(uint8_t id, WStype_t type, uint8_t *payload, size_t length);
    static void handleRequest(uint8_t id, long key, int type, String data);
    static void process();

    static void saveApiKeyToEEPROM();
    static void loadApiKeyFromEEPROM();
    static void resetApiKey();
};

#endif