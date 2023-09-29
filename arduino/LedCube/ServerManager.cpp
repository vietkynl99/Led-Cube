#include "ServerManager.h"

WebSocketsServer *ServerManager::webSocket;
long ServerManager::apiKey = 0;
int ServerManager::batteryLevel = 25;

void ServerManager::init()
{
    loadApiKeyFromEEPROM();

    if (MDNS.begin(mDNS_DOMAIN))
    {
        LOG_SYSTEM("Start mDNS responder at ws://%s.local:%d/ws", mDNS_DOMAIN, WEB_SERVER_PORT);
    }
    else
    {
        LOG_SYSTEM("Error setting up MDNS responder!");
    }

    webSocket = new WebSocketsServer(WEB_SERVER_PORT);

    webSocket->begin();
    webSocket->onEvent(onSocketEvent);

    MDNS.addService("ws", "tcp", WEB_SERVER_PORT);
    LOG_SERVER("Server started at port %d", WEB_SERVER_PORT);
}

String ServerManager::generateJson(String key, String value)
{
    StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;

    jsonDoc[key] = value;
    serializeJson(jsonDoc, json);
    return json;
}

void ServerManager::sendResponse(uint8_t id, int type, String data)
{
    static StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;

    jsonDoc["name"] = DEVICE_NAME;
    jsonDoc["type"] = type;
    jsonDoc["data"] = data;
    serializeJson(jsonDoc, json);

    LOG_SERVER("sendResponse %s", json.c_str());
    webSocket->sendTXT(id, json);
}

void ServerManager::sendResponse(uint8_t id, int type, String dataKey, String dataValue)
{
    String data = "";
    if (!dataKey.isEmpty() && !dataValue.isEmpty())
    {
        data = generateJson(dataKey, dataValue);
    }
    sendResponse(id, type, data);
}

void ServerManager::sendResponse(uint8_t id, int type)
{
    sendResponse(id, type, "");
}


void ServerManager::pairDevice(uint8_t id, long oldKey)
{
    if (oldKey == apiKey && apiKey >= API_KEY_MIN && apiKey <= API_KEY_MAX)
    {
        LOG_SERVER("Device is paired!!! Ignored Request!")
        sendResponse(id, EVENT_RESPONSE_PAIR_DEVICE_PAIRED);
    }
    else if (!HardwareController::getInstance()->isPairingMode())
    {
        LOG_SERVER("Ignored Request!")
        sendResponse(id, EVENT_RESPONSE_PAIR_DEVICE_IGNORED);
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
        sendResponse(id, EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL, "apiKey", String(apiKey));
        HardwareController::getInstance()->beep(1);
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
    if (jsonDoc["cmd"].is<int>())
    {
        LedManager::getInstance()->command(jsonDoc["cmd"]);
    }
}

void ServerManager::onSocketEvent(uint8_t id, WStype_t type, uint8_t *payload, size_t length)
{

    switch (type)
    {
    case WStype_DISCONNECTED:
    {
        LOG_SERVER("[%u] Disconnected!", id);
        break;
    }
    case WStype_CONNECTED:
    {
        IPAddress ip = webSocket->remoteIP(id);
        LOG_SERVER("[%u] Connected from %d.%d.%d.%d url: %s", id, ip[0], ip[1], ip[2], ip[3], payload);
        break;
    }
    case WStype_TEXT:
    {
        LOG_SERVER("[%u] get Text: %s", id, payload);
        String message = String((char *)payload);
        if (message.isEmpty())
        {
            return;
        }

        StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
        DeserializationError error = deserializeJson(jsonDoc, message);
        if (error)
        {
            LOG_SERVER("Cannot deserialize json");
        }
        else
        {
            long key = -1;
            int type = -1;
            String data = "";

            if (jsonDoc["key"].is<long>())
            {
                key = jsonDoc["key"];
            }
            if (jsonDoc["type"].is<int>())
            {
                type = jsonDoc["type"];
            }
            if (jsonDoc["data"].is<String>())
            {
                data = jsonDoc["data"].as<String>();
            }
            if (key >= 0 && type >= 0)
            {
                handleRequest(id, key, type, data);
            }
            else
            {
                LOG_SERVER("Invalid message");
            }
        }
        break;
    }
    default:
        break;
    }
}

void ServerManager::handleRequest(uint8_t id, long key, int type, String data)
{
    bool keyIsValid = key > 0 && key == apiKey;

    LOG_SERVER("Received a request: id[%d] key[%d] type[%d] data[%s] -> keyValid[%d]", id, key, type, data.c_str(), keyIsValid);

    /* Handle event */
    // If type=EVENT_REQUEST_PAIR_DEVICE we don't need to check key
    if (type == EVENT_REQUEST_PAIR_DEVICE)
    {
        pairDevice(id, key);
        return;
    }

    // Check key is valid
    if (!keyIsValid)
    {
        sendResponse(id, EVENT_RESPONSE_INVALID_KEY);
        return;
    }

    // Other event
    switch (type)
    {
    case EVENT_REQUEST_CHECK_CONNECTION:
    {
        sendResponse(id, EVENT_RESPONSE_UPDATE_DATA, HardwareController::getInstance()->getSensorsData());
        break;
    }
    case EVENT_REQUEST_SEND_DATA:
    {
        dataProcessing(data);
        sendResponse(id, EVENT_RESPONSE_GET_DATA_SUCCESSFUL, HardwareController::getInstance()->getSensorsData());
        break;
    }
    default:
    {
        sendResponse(id, EVENT_RESPONSE_CHECK_CONNECTION, "pair", keyIsValid ? "1" : "0");
        break;
    }
    }
}

void ServerManager::process()
{
    MDNS.update();
    webSocket->loop();
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