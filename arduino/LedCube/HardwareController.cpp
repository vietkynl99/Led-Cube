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
    mFakePairMode = false;
    mBeepPlayingCount = 0;
    mBatteryLevel = -1;
    mAdcMode = ADC_MODE_NONE;
    setPairMode(false);
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

void HardwareController::loop()
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
    EEPROM.begin(EEPROM_SIZE_MAX);
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

    angleXOffset = MPU6050_OFFSET_X_DEFAULT;
    angleYOffset = MPU6050_OFFSET_Y_DEFAULT;
    angleZOffset = MPU6050_OFFSET_Z_DEFAULT;
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
        if ((unsigned long long)(millis() - fakePairTime) > BUTTON_LONG_PRESS_TIMEOUT)
        {
            LOG_SERVER("Turn off fake pair mode");
            mFakePairMode = 0;
        }
    }
    preFakePairMode = mFakePairMode;
}

void HardwareController::setPairMode(bool enable)
{
    mPairMode = enable;
}

void HardwareController::checkPairMode()
{
    static bool prePairMode = false;
    if (prePairMode != isPairingMode())
    {
        prePairMode = isPairingMode();
        LOG_SERVER("Pair mode changed to: %d", isPairingMode());
    }
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
    static bool oldState = false, newState = false;
    static unsigned long long time = 0;
    static unsigned long long risingTime = 0, preRisingTime = 0, lastRisingTime = 0;
    static bool hasLastPress = false, hasPrePress = false, hasShortPress = false, hasLongPress = false;
    static int buttonState = BUTTON_STATE_NONE, preButtonState = BUTTON_STATE_NONE;

    if (millis() > time)
    {
        time = millis() + BUTTON_SCAN_TIME;
        oldState = newState;
        newState = !digitalRead(BUTTON_PIN);
        preButtonState = buttonState;
        buttonState = BUTTON_STATE_NONE;

        // Rising edge
        if (!oldState && newState)
        {
            lastRisingTime = preRisingTime;
            preRisingTime = risingTime;
            risingTime = millis();
        }

        unsigned long long pressTime = millis() - risingTime;
        unsigned long long prePressTime = risingTime - preRisingTime;
        unsigned long long lastPressTime = preRisingTime - lastRisingTime;
        hasShortPress = oldState && !newState && pressTime > BUTTON_SHORT_PRESS_TIME_MIN && pressTime < BUTTON_SHORT_PRESS_TIME_MAX;
        hasLongPress = newState && pressTime > BUTTON_LONG_PRESS_TIME && pressTime < BUTTON_LONG_PRESS_TIMEOUT;
        hasPrePress = (hasShortPress || hasLongPress) && prePressTime > BUTTON_SHORT_PRESS_TIME_MIN && prePressTime < BUTTON_PRE_PRESS_TIME_MAX;
        hasLastPress = hasPrePress && lastPressTime > BUTTON_SHORT_PRESS_TIME_MIN && lastPressTime < BUTTON_PRE_PRESS_TIME_MAX;

        if (!hasLastPress && !hasPrePress && hasShortPress)
        {
            buttonState = BUTTON_STATE_SHORT_PRESSED;
        }
        else if (hasLongPress)
        {
            if (!hasLastPress && !hasPrePress)
            {
                buttonState = BUTTON_STATE_LONG_PRESSED;
            }
            else if (!hasLastPress && hasPrePress)
            {
                buttonState = BUTTON_STATE_PRESSED_AND_LONG_PRESSED;
            }
            else if (hasLastPress && hasPrePress)
            {
                buttonState = BUTTON_STATE_DOUBLE_PRESSED_AND_LONG_PRESSED;
            }
        }
        // State changed
        if (preButtonState != buttonState)
        {
            LOG_SYSTEM("Button state changed: %d", buttonState);
            switch (buttonState)
            {
            case BUTTON_STATE_SHORT_PRESSED:
                beep(1);
                LedManager::getInstance()->changeToNextType();
                break;
            case BUTTON_STATE_LONG_PRESSED:
                beep(2);
                LedManager::getInstance()->changeToNextSubType();
                break;
            case BUTTON_STATE_PRESSED_AND_LONG_PRESSED:
                beep(3);
                setPairMode(true);
                break;
            case BUTTON_STATE_DOUBLE_PRESSED_AND_LONG_PRESSED:
                beep(4, true);
                WifiMaster::getInstance()->resetWifiSettings();
                break;
            default:
                setPairMode(false);
                break;
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
                LOG_SENSOR("Battery: %d -> %.2fV (%d%%)", (int)result, voltage, mBatteryLevel);
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
float HardwareController::getAngleDegX()
{
    return mpuSensor->getAngleX() + angleXOffset;
}

float HardwareController::getAngleDegY()
{
    return mpuSensor->getAngleY() + angleYOffset;
}

float HardwareController::getAngleDegZ()
{
    return mpuSensor->getAngleZ() + angleZOffset;
}

float HardwareController::getAngleRadX()
{
    return getAngleDegX() * PI / 180;
}

float HardwareController::getAngleRadY()
{
    return getAngleDegY() * PI / 180;
}

float HardwareController::getAngleRadZ()
{
    return getAngleDegZ() * PI / 180;
}

float HardwareController::getGyroX()
{
    return mpuSensor->getGyroX() + MPU6050_OFFSET_GYRO_X;
}

float HardwareController::getGyroY()
{
    return mpuSensor->getGyroY() + MPU6050_OFFSET_GYRO_Y;
}

float HardwareController::getGyroZ()
{
    return mpuSensor->getGyroZ() + MPU6050_OFFSET_GYRO_Z;
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
        LOG_SENSOR("Error: ADC is not available. Current mode %d", mAdcMode);
        return;
    }
    mAdcMode = mode;
    LOG_SENSOR("ADC mode changed to %d", mAdcMode);
    digitalWrite(ADC_CTRL_PIN, mAdcMode == ADC_MODE_BATTERY);
}