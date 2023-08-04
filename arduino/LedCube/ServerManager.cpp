#include "ServerManager.h"

ESP8266WebServer ServerManager::server;
long ServerManager::apiKey = 0;
int ServerManager::batteryLevel = 25;

void ServerManager::init()
{
    loadApiKeyFromEEPROM();

    if (MDNS.begin(mDNS_DOMAIN))
    {
        LOG_SYSTEM("Start mDNS responder at %s.local", mDNS_DOMAIN);
    }
    else
    {
        LOG_SYSTEM("Error setting up MDNS responder!");
    }

    // Register callback function for http request
    server.on("/", handleRequest);
    // Start server
    server.begin(WEB_SERVER_PORT);

    MDNS.addService("http", "tcp", WEB_SERVER_PORT);
    LOG_SERVER("Server started at port %d", WEB_SERVER_PORT);
}

void ServerManager::checkWifiStatus()
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

String ServerManager::generateJson(String key, String value)
{
    StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;

    jsonDoc[key] = value;
    serializeJson(jsonDoc, json);
    return json;
}

void ServerManager::sendResponse(int type, String data)
{
    static StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;

    jsonDoc["name"] = DEVICE_NAME;
    jsonDoc["type"] = type;
    jsonDoc["data"] = data;
    serializeJson(jsonDoc, json);

    LOG_SERVER("sendResponse %s", json.c_str());
    server.send(200, "application/json", json);
}

void ServerManager::sendResponse(int type, String dataKey, String dataValue)
{
    String data = "";
    if (!dataKey.isEmpty() && !dataValue.isEmpty())
    {
        data = generateJson(dataKey, dataValue);
    }
    sendResponse(type, data);
}

void ServerManager::sendResponse(int type)
{
    sendResponse(type, "");
}

void ServerManager::sendInvalidResponse()
{
    server.send(200, "text/plain", ".");
}

void ServerManager::pairDevice(long oldKey)
{
    if (oldKey == apiKey && apiKey >= API_KEY_MIN && apiKey <= API_KEY_MAX)
    {
        LOG_SERVER("Device is paired!!! Ignored Request!")
        sendResponse(EVENT_RESPONSE_PAIR_DEVICE_PAIRED);
    }
    else if (!HardwareController::getInstance()->isPairingMode())
    {
        LOG_SERVER("Ignored Request!")
        sendResponse(EVENT_RESPONSE_PAIR_DEVICE_IGNORED);
    }
    else
    {
        LOG_SERVER("Pairing new device...")
        // Generate new key
        long newKey = 0;
        while (newKey < API_KEY_MIN || newKey > API_KEY_MAX || newKey == apiKey)
        {
            newKey = random(API_KEY_MIN, API_KEY_MAX);
        }
        apiKey = newKey;
        saveApiKeyToEEPROM();
        LOG_SERVER("Paired new device, apiKey: %d", apiKey);
        sendResponse(EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL, "apiKey", String(apiKey));
    }
}

void ServerManager::dataProcessing(String data)
{
    if (data.isEmpty())
    {
        return;
    }

    StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    deserializeJson(jsonDoc, data);

    if (jsonDoc["type"].is<int>())
    {
        LedManager::getInstance()->setType(jsonDoc["type"]);
    }
    if (jsonDoc["mode"].is<int>())
    {
        LedManager::getInstance()->setSubType(jsonDoc["mode"]);
    }
    if (jsonDoc["brightness"].is<int>())
    {
        LedManager::getInstance()->setBrightness(jsonDoc["brightness"]);
    }
    if (jsonDoc["saturation"].is<int>())
    {
        LedManager::getInstance()->setSaturation(jsonDoc["saturation"]);
    }
    if (jsonDoc["hue"].is<uint16_t>())
    {
        LedManager::getInstance()->setHue(jsonDoc["hue"]);
    }
    if (jsonDoc["deviation"].is<uint16_t>())
    {
        LedManager::getInstance()->setDeviation(jsonDoc["deviation"]);
    }
    if (jsonDoc["sensitivity"].is<int>())
    {
        LedManager::getInstance()->setSensitivity(jsonDoc["sensitivity"]);
    }
}

void ServerManager::handleRequest()
{
    bool validResponse = true;
    bool keyIsValid = false;
    String keyStr = server.arg("key");
    long key = keyStr.toInt();
    if (key > 0 && key == apiKey)
    {
        keyIsValid = true;
    }
    if (keyStr.isEmpty())
    {
        validResponse = false;
    }

    int type = -1;
    String typeStr = server.arg("type");
    int typeInt = typeStr.toInt();
    if (typeInt >= EVENT_NONE)
    {
        type = typeInt;
    }
    if (typeStr.isEmpty() || type < 0)
    {
        validResponse = false;
    }

    String data = server.arg("data");

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
        sendResponse(EVENT_RESPONSE_UPDATE_DATA, HardwareController::getInstance()->getSensorsData());
        break;
    }
    case EVENT_REQUEST_SEND_DATA:
    {
        dataProcessing(data);
        sendResponse(EVENT_RESPONSE_GET_DATA_SUCCESSFUL, HardwareController::getInstance()->getSensorsData());
        break;
    }
    default:
    {
        sendResponse(EVENT_RESPONSE_CHECK_CONNECTION, "pair", keyIsValid ? "1" : "0");
        break;
    }
    }
}

void ServerManager::process()
{
    checkWifiStatus();
    MDNS.update();
    server.handleClient();
}

void ServerManager::saveApiKeyToEEPROM()
{
    EEPROM_SET_DATA(EEPROM_ADDR_API_KEY, apiKey);
}

void ServerManager::loadApiKeyFromEEPROM()
{
    EEPROM_GET_DATA(EEPROM_ADDR_API_KEY, apiKey);
    LOG_SYSTEM("Read EEPROM -> API KEY: %d", apiKey);
}

void ServerManager::resetApiKey()
{
    LOG_SERVER("Reset API Key");
    apiKey = 0;
    saveApiKeyToEEPROM();
}