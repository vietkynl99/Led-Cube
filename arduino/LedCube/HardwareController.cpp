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
    mBatteryLevel = -1;
    mAdcMode = ADC_MODE_NONE;
}

void HardwareController::init()
{
    initHardwarePin();
#if USE_SERIAL_DEBUG
    initSerial();
#endif
    initEEPROM();
    initSensors();
}

void HardwareController::process()
{
    buttonHandler();
    beepHandler();
    fakePairModeHandler();
    checkPairMode();
#ifdef ENABLE_BATTERY_MEASUREMENT
    measureBattery();
#endif
#ifdef ENABLE_DHT11_SENSOR
    processDHT();
#endif
#ifdef ENABLE_MPU6050_SENSOR
    processMPU();
#endif
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
    pinMode(ADC_CTRL_PIN, OUTPUT);

    digitalWrite(ADC_CTRL_PIN, LOW);
}

void HardwareController::initEEPROM()
{
    EEPROM.begin(EEPROM_SIZE);
}

void HardwareController::initSensors()
{
#ifdef ENABLE_DHT11_SENSOR
    dhtSensor = new DHT_Async(DHT11_PIN, DHT_TYPE_11);
    mTemperature = -1;
    mHumidity = -1;
#endif

#ifdef ENABLE_MPU6050_SENSOR
    mpuSensor = new MPU6050(Wire);
    Wire.begin();
    mpuSensor->begin();
    // mpuSensor->calcGyroOffsets(true);

    angleXOffset = 0;
    angleYOffset = 0;
    angleZOffset = 0;
#endif
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
    static unsigned long long time = 0, startTime = 0;
    static bool isFirstTime = true;
    static int measureCount = 0;
    static float result = 0;
    static bool isRunning = false;

    if (!isRunning)
    {
        if ((isFirstTime || (unsigned long long)(millis() - time) > BATTERY_SCAN_TIME))
        {
            if (!isAdcAvailable())
            {
                time = millis();
            }
            else
            {
                changeAdcMode(ADC_MODE_BATTERY);
                isRunning = true;
                measureCount = BATTERY_MEASURE_COUNT_MAX;
                result = 0;
                startTime = millis();
            }
        }
    }
    else
    {
        if ((unsigned long long)(millis() - startTime) > 10UL)
        {
            startTime = millis();
            result += analogRead(ADC_PIN);
            measureCount--;
            if (measureCount <= 0)
            {
                changeAdcMode(ADC_MODE_NONE);
                isFirstTime = false;
                isRunning = false;
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
}

#ifdef ENABLE_DHT11_SENSOR
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
#endif

#ifdef ENABLE_MPU6050_SENSOR
void HardwareController::processMPU()
{
    mpuSensor->update();
}
#endif

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

String HardwareController::getSensorsData()
{
    StaticJsonDocument<JSON_BYTE_MAX> jsonDoc;
    String json;
    jsonDoc["bat"] = mBatteryLevel;
#ifdef ENABLE_DHT11_SENSOR
    jsonDoc["hum"] = mHumidity;
    jsonDoc["temp"] = mTemperature;
#endif

    serializeJson(jsonDoc, json);
    return json;
}

#ifdef ENABLE_MPU6050_SENSOR
float HardwareController::getAngleX()
{
    return mpuSensor->getAngleX() + angleXOffset;
}

float HardwareController::getAngleY()
{
    return mpuSensor->getAngleY() + angleYOffset;
}

float HardwareController::getAngleZ()
{
    return mpuSensor->getAngleZ() + angleZOffset;
}
#endif

int HardwareController::getAdc()
{
    return analogRead(ADC_PIN);
}

int HardwareController::getAdcMode()
{
    return mAdcMode;
}

bool HardwareController::isAdcAvailable()
{
    return mAdcMode == ADC_MODE_NONE;
}

void HardwareController::changeAdcMode(int mode)
{
    if (!isAdcAvailable() && mode != ADC_MODE_NONE)
    {
        LOG_SYSTEM("Error: ADC is not available. Current mode %d", mAdcMode);
        return;
    }
    mAdcMode = mode;
    LOG_SYSTEM("ADC mode changed to %d", mAdcMode);
    digitalWrite(ADC_CTRL_PIN, mAdcMode == ADC_MODE_BATTERY);
}