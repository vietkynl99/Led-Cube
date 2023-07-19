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
    mPairMode = false;
    mFakePairMode = false;
    mBeepPlayingCount = 0;
    mTemperature = -1;
    mHumidity = -1;
    mBatteryLevel = -1;
    isMeasuringBattery = false;
    dhtSensor = new DHT_Async(DHT11_PIN, DHT_TYPE_11);
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
    measureBattery();
    processDHT();
}

#if USE_SERIAL_DEBUG
void HardwareController::initSerial()
{
    Serial.begin(115200);
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
    mFakePairMode = 1;
}

void HardwareController::fakePairModeHandler()
{
    static unsigned long long fakePairTime = 0;
    static bool preFakePairMode = false;
    if (!preFakePairMode && mFakePairMode)
    {
        fakePairTime = millis();
    }
    if (mFakePairMode)
    {
        if ((unsigned long long)(millis() - fakePairTime) > BUTTON_PAIR_MODE_TIMEOUT)
        {
            LOG_SERVER("Turn off fake pair mode");
            mFakePairMode = 0;
        }
    }
    preFakePairMode = mFakePairMode;
}

void HardwareController::checkPairMode()
{
    static bool prePairMode = false;
    bool pairMode = isPairingMode();
    if (prePairMode != pairMode)
    {
        LOG_SERVER("Pair mode changed to: %d", pairMode);
        if (mPairMode)
        {
            beep(1);
        }
    }
    prePairMode = pairMode;
}

void HardwareController::setBeepState(bool state)
{
    analogWrite(BUZZER_PIN, state ? BUZZER_PWM_MAX : 0);
}

void HardwareController::beepHandler()
{
    static bool pinState = false;
    static unsigned long long time = 0;

    if (mBeepPlayingCount > 0)
    {
        if ((unsigned long long)(millis() - time) > BUZZER_OUTPUT_TIME)
        {
            time = millis();
            pinState ^= 1;
            setBeepState(pinState);
            if (!pinState)
            {
                mBeepPlayingCount--;
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
            mPairMode = false;
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
                mPairMode = pressTime > BUTTON_LONG_PRESS_TIME && pressTime < BUTTON_PAIR_MODE_TIMEOUT;
            }
        }
    }
}

void HardwareController::measureBattery()
{
    static unsigned long long time = 0, scanTime = 0;
    static bool isFirstTime = true;
    static int measureCount = 0;
    static float result = 0;
    if (!isMeasuringBattery && (isFirstTime || (unsigned long long)(millis() - time) > BATTERY_SCAN_TIME))
    {
        isMeasuringBattery = true;
        measureCount = BATTERY_MEASURE_COUNT_MAX;
        result = 0;
        scanTime = millis();
    }
    if (isMeasuringBattery && (unsigned long long)(millis() - scanTime) > 10UL)
    {
        scanTime = millis();
        result += analogRead(ADC_PIN);
        measureCount--;
        if (measureCount <= 0)
        {
            isFirstTime = false;
            isMeasuringBattery = false;
            time = millis();
            result = result / BATTERY_MEASURE_COUNT_MAX;
            float voltage = (result - 255) / 55;
            mBatteryLevel = (voltage - BATTERY_MIN_VOLTAGE) * 100 / (BATTERY_MAX_VOLTAGE - BATTERY_MIN_VOLTAGE);
            if (mBatteryLevel > 100)
            {
                mBatteryLevel = 100;
            }
            if (mBatteryLevel < 0)
            {
                mBatteryLevel = 0;
            }
            LOG_SYSTEM("Battery: %d -> %.2fV (%d%%)", (int)result, voltage, mBatteryLevel);
        }
    }
}

void HardwareController::processDHT()
{
    static float temp, hum;
    static unsigned long long time = 0;
    static bool isFirstTime = true;
    if (isFirstTime || (unsigned long long)(millis() - time) > DHT11_SCAN_TIME)
    {
        if (dhtSensor->measure(&temp, &hum))
        {
            isFirstTime = false;
            time = millis();
            mTemperature = round(temp);
            mHumidity = round(hum);
            LOG_SYSTEM("DHT11 -> temp: %d, humidity: %d", mTemperature, mHumidity);
        }
    }
}

bool HardwareController::isPairingMode()
{
    return mPairMode || mFakePairMode;
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
        if (mBeepPlayingCount <= 0)
        {
            mBeepPlayingCount = count;
        }
    }
}

String HardwareController::getAllData() {
    StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;
    jsonDoc["bat"] = mBatteryLevel;
    jsonDoc["hum"] = mHumidity;
    jsonDoc["temp"] = mTemperature;

    serializeJson(jsonDoc, json);
    return json;
}