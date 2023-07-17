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
    pairMode = false;
    fakePairMode = false;
    beepPlayingCount = 0;
}

void HardwareController::init()
{
    initHardwarePin();
#if USE_SERIAL_DEBUG
    initSerial();
#endif
    initEEPROM();
}

void HardwareController::process()
{
    buttonHandler();
    beepHandler();
    fakePairModeHandler();
    checkPairMode();
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
    pinMode(BUTTON_PIN, INPUT_PULLUP);
    pinMode(BUZZER_PIN, OUTPUT);
}

void HardwareController::initEEPROM()
{
    EEPROM.begin(EEPROM_SIZE);
}

void HardwareController::turnOnFakePairMode()
{
    // make fake pair mode within 15s
    LOG_SERVER("Turn on fake pair mode");
    fakePairMode = 1;
}

void HardwareController::fakePairModeHandler()
{
    static unsigned long long fakePairTime = 0;
    static bool preFakePairMode = false;
    if (!preFakePairMode && fakePairMode)
    {
        fakePairTime = millis();
    }
    if (fakePairMode)
    {
        if ((unsigned long long)(millis() - fakePairTime) > BUTTON_PAIR_MODE_TIMEOUT)
        {
            LOG_SERVER("Turn off fake pair mode");
            fakePairMode = 0;
        }
    }
    preFakePairMode = fakePairMode;
}

void HardwareController::checkPairMode()
{
    static bool prePairMode = false;
    bool pairMode = isPairingMode();
    if (prePairMode != pairMode)
    {
        LOG_SERVER("Pair mode changed to: %d", pairMode);
        if (pairMode)
        {
            beep(1);
        }
    }
    prePairMode = pairMode;
}

void HardwareController::setBeepState(bool state)
{
    analogWrite(BUZZER_PIN, state? BUZZER_PWM_MAX : 0);
}

void HardwareController::beepHandler()
{
    static bool pinState = false;
    static unsigned long long time = 0;

    if (beepPlayingCount > 0)
    {
        if ((unsigned long long)(millis() - time) > BUZZER_OUTPUT_TIME)
        {
            time = millis();
            pinState ^= 1;
            setBeepState(pinState);
            if (!pinState)
            {
                beepPlayingCount--;
            }
        }
    }
    else if (pinState)
    {
        pinState = false;
        setBeepState(pinState);
    }
}

void HardwareController::buttonHandler()
{
    static bool oldState = false;
    static bool newState = false;
    static unsigned long long time = 0;
    static unsigned long long risingTime = 0, pressTime = 0, lastPressTime = 0;

    if ((unsigned long long)(millis() - time) > BUTTON_SCAN_TIME)
    {
        time = millis();
        oldState = newState;
        newState = !digitalRead(BUTTON_PIN);

        // Rising edge
        if (!oldState && newState)
        {
            lastPressTime = risingTime;
            risingTime = millis();
        }
        // Falling edge
        if (oldState && !newState)
        {
            pairMode = false;
        }
        // ON state
        if (newState)
        {
            pressTime = millis() - risingTime;
            // Press then long press
            if (risingTime - lastPressTime < BUTTON_DOUBLE_LONG_PRESS_TIME)
            {
                if (pressTime > BUTTON_LONG_PRESS_TIME)
                {
                    beep(3, true);
                    WifiMaster::getInstance()->resetWifiSettings();
                }
            }
            // Only long press
            else
            {
                pairMode = pressTime > BUTTON_LONG_PRESS_TIME && pressTime < BUTTON_PAIR_MODE_TIMEOUT;
            }
        }
    }
}

bool HardwareController::isPairingMode()
{
    return pairMode || fakePairMode;
}

void HardwareController::beep(int count, bool blocking)
{
    if (blocking)
    {
        bool pinState = false;
        for (int i = 0; i < 2 * count; i++)
        {
            pinState ^= 1;
            setBeepState(pinState);
            delay(BUZZER_OUTPUT_TIME);
        }
    }
    else
    {
        if (beepPlayingCount <= 0)
        {
            beepPlayingCount = count;
        }
    }
}