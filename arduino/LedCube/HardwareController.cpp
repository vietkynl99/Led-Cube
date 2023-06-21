#include <Arduino.h>
#include "HardwareController.h"


HardwareController *HardwareController::instance = nullptr;

HardwareController *HardwareController::getInstance()
{
    if (instance == nullptr)
    {
        instance = new HardwareController();
    }
    return instance;
}

HardwareController::HardwareController()
{
    pair_mode = false;
    fake_pair_mode = false;
}

void HardwareController::init()
{
    initHardwarePin();
#if USE_SERIAL_DEBUG
    initSerial();
#endif
    initEEPROM();
}

#if USE_SERIAL_DEBUG
void HardwareController::initSerial()
{
    Serial.begin(115200);
    delay(2000);
    Serial.println();
}
#endif

void HardwareController::initHardwarePin()
{
    pinMode(RESET_WIFI_PIN, INPUT_PULLUP);
    pinMode(PAIR_MODE_PIN, INPUT_PULLUP);
}

void HardwareController::initEEPROM()
{
    EEPROM.begin(EEPROM_SIZE);
}

void HardwareController::checkResetWifiButton()
{
    static bool status_old = 0, status_new = 0;
    static unsigned long long state_changed_time = 0;

    status_old = status_new;
    status_new = !digitalRead(RESET_WIFI_PIN);
    if (status_old != status_new)
    {
        state_changed_time = millis();
    }

    if (status_new)
    {
        if ((unsigned long long)(millis() - state_changed_time) > LONG_PRESS_TIME)
        {
            LOG_WIFI("Reset button pressed");
            WifiMaster::getInstance()->resetWifiSettings();
        }
    }
}

void HardwareController::checkPairButton()
{
    static bool status_old = 0, status_new = 0;
    static unsigned long long pair_start_time = 0, pair_time = 0;

    status_old = status_new;
    status_new = !digitalRead(PAIR_MODE_PIN);
    if (status_old != status_new)
    {
        pair_start_time = millis();
    }

    if (status_new)
    {
        pair_time = millis() - pair_start_time;
        pair_mode = (pair_time > LONG_PRESS_TIME) && (pair_time < PAIR_MODE_TIMEOUT);
    }
    else
    {
        pair_mode = false;
    }
}

void HardwareController::turnOnFakePairMode()
{
    // make fake pair mode within 15s
    LOG_SERVER("Turn on fake pair mode");
    fake_pair_mode = 1;
}

void HardwareController::checkFakePairMode()
{
    static unsigned long long fake_pair_time = 0;
    static bool pre_fake_pair_mode = false;
    if (!pre_fake_pair_mode && fake_pair_mode)
    {
        fake_pair_time = millis();
    }
    if (fake_pair_mode)
    {
        if ((unsigned long long)(millis() - fake_pair_time) > PAIR_MODE_TIMEOUT)
        {
            LOG_SERVER("Turn off fake pair mode");
            fake_pair_mode = 0;
        }
    }
    pre_fake_pair_mode = fake_pair_mode;
}

void HardwareController::checkPairMode()
{
    static bool pre_pair_mode = false;
    bool pair_mode = isPairingMode();
    if (pre_pair_mode != pair_mode)
    {
        LOG_SERVER("Pair mode changed to: %d", pair_mode);
    }
    pre_pair_mode = pair_mode;
}

void HardwareController::process()
{
    checkResetWifiButton();
	checkPairButton();
	checkFakePairMode();
	checkPairMode();
}

bool HardwareController::isPairingMode()
{
    return pair_mode || fake_pair_mode;
}