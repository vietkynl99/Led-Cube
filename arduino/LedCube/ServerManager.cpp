#include <Arduino.h>
#include "ServerManager.h"

ESP8266WebServer ServerManager::server;
long ServerManager::apiKey = 0;
bool ServerManager::isPairMode = false;
int ServerManager::batteryLevel = 25;

void ServerManager::init()
{

    loadApiKeyFromEEPROM();

    WiFi.hostname(HOST_NAME);
    // Register callback function for http request
    ServerManager::server.on("/", ServerManager::handleRequest);
    // Start ServerManager::server
    ServerManager::server.begin(80);
    LOG_SERVER("Server started");
}

String ServerManager::generateJson(String key, String value)
{
    StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;

    jsonDoc[key] = value;
    serializeJson(jsonDoc, json);
    return json;
}

void ServerManager::sendResponse(int type, String dataKey, String dataValue)
{
    static StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json = "", data = "";

    if (!dataKey.isEmpty() && !dataValue.isEmpty())
    {
        data = generateJson(dataKey, dataValue);
    }

    LOG_SERVER("sendResponse type[%d] data[%s]", type, data.c_str());
    jsonDoc["name"] = DEVICE_NAME;
    jsonDoc["type"] = type;
    jsonDoc["data"] = data;

    serializeJson(jsonDoc, json);
    ServerManager::server.send(200, "application/json", json);
}

void ServerManager::sendInvalidResponse()
{
    ServerManager::server.send(200, "text/plain", ".");
}

void ServerManager::pairDevice(long oldKey)
{
    if (oldKey == ServerManager::apiKey && ServerManager::apiKey >= API_KEY_MIN && ServerManager::apiKey <= API_KEY_MAX)
    {
        LOG_SERVER("Device is paired!!! Ignored Request!")
        sendResponse(EVENT_RESPONSE_PAIR_DEVICE_PAIRED);
    }
    else if (!ServerManager::isPairMode)
    {
        LOG_SERVER("Ignored Request!")
        sendResponse(EVENT_RESPONSE_PAIR_DEVICE_IGNORED);
    }
    else
    {
        LOG_SERVER("Pairing new device...")
        // Generate new key
        long newKey = 0;
        while (newKey < API_KEY_MIN || newKey > API_KEY_MAX || newKey == ServerManager::apiKey)
        {
            newKey = random(API_KEY_MIN, API_KEY_MAX);
        }
        ServerManager::apiKey = newKey;
        saveApiKeyToEEPROM();
        LOG_SERVER("Paired new device, ServerManager::apiKey: %d", ServerManager::apiKey);
        sendResponse(EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL, "apiKey", String(ServerManager::apiKey));
    }
}

void ServerManager::handleRequest()
{
    bool validResponse = true;
    bool keyIsValid = false;
    long key = -1;
    String keyStr = ServerManager::server.arg("key");
    int keyInt = keyStr.toInt();
    if (keyInt > 0)
    {
        key = keyInt;
        if (key == ServerManager::apiKey)
        {
            keyIsValid = true;
        }
    }
    if (keyStr.isEmpty() || key < 0)
    {
        validResponse = false;
    }

    int type = -1;
    String typeStr = ServerManager::server.arg("type");
    int typeInt = typeStr.toInt();
    if (typeInt >= EVENT_NONE)
    {
        type = typeInt;
    }
    if (typeStr.isEmpty() || type < 0)
    {
        validResponse = false;
    }

    String data = ServerManager::server.arg("data");

    LOG_SERVER("Received a request: key[%d] type[%d] data[%s] -> valid[%d] keyValid[%d]", key, type, data.c_str(), validResponse, keyIsValid);

    if (!validResponse)
    {
        sendInvalidResponse();
        return;
    }

    /* Handle event */
    // If type=EVENT_REQUEST_PAIR_DEVICE we don't need to check key
    if (type == EVENT_REQUEST_PAIR_DEVICE)
    {
        pairDevice(key);
        return;
    }

    // Check key is valid
    if (!keyIsValid)
    {
        sendResponse(EVENT_RESPONSE_INVALID_KEY);
        return;
    }

    // Other event
    switch (type)
    {
    case EVENT_REQUEST_CHECK_CONNECTION:
    {
        sendResponse(EVENT_RESPONSE_UPDATE_DATA, "batteryLevel", String(ServerManager::batteryLevel));
        break;
    }
    case EVENT_REQUEST_SEND_DATA:
    {
        LOG_SERVER("Got data!");
        // TODO: use data to update cube effect state
        sendResponse(EVENT_RESPONSE_GET_DATA_SUCCESSFUL);
        break;
    }
    default:
    {
        sendResponse(EVENT_RESPONSE_CHECK_CONNECTION, "pair", keyIsValid ? "1" : "0");
        break;
    }
    }
}

void ServerManager::handleClient()
{
    ServerManager::server.handleClient();
}

void ServerManager::setPairMode(bool pairMode)
{
    ServerManager::isPairMode = pairMode;
}

void ServerManager::saveApiKeyToEEPROM()
{
    EEPROM.put(0, ServerManager::apiKey);
    EEPROM.commit();
}

void ServerManager::loadApiKeyFromEEPROM()
{
    EEPROM.get(0, ServerManager::apiKey);
    LOG_SYSTEM("Read EEPROM -> API KEY: %d", ServerManager::apiKey);
}

void ServerManager::resetApiKey()
{
    LOG_SERVER("Reset API Key");
    ServerManager::apiKey = 0;
    saveApiKeyToEEPROM();
}