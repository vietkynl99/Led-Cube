#include <Arduino.h>
#include "LedManager.h"

LedManager *LedManager::instance = nullptr;

LedManager *LedManager::getInstance()
{
    if (instance == nullptr)
    {
        instance = new LedManager();
    }
    return instance;
}

LedManager::LedManager()
{
    mType = DEFAULT_TYPE;
    mBrightness = DEFAULT_BRIGHTNESS;
    mSensitivity = DEFAULT_SENSITIVITY;
    mSaturation = DEFAULT_SATURATION;
    mSubType = NONE;
    mGHue = HUE_GREEN;
    mDHue = 0;
    strip = new Adafruit_NeoPixel(NUM_LEDS, LED_DATA_PIN, LED_TYPE);
}

void LedManager::init()
{
    strip->begin();
    turnOff();

#ifdef RESTORE_PREVIOUS_DATA
    restoreSettings();
#endif
    setType(mType, true);
    setBrightness(mBrightness, true);
    setSaturation(mSaturation, true);
    setSensitivity(mSensitivity, true);
    setSubType(mSubType, true);
    setHue(mGHue, true);
    setDeviation(mDHue, true);
    LOG_LED("type: %d, subType: %d, brightness: %d, sensitivity: %d", mType, mSubType, mBrightness, mSensitivity);

    FFT = new arduinoFFT();
}

void LedManager::readPreviousEEPROMData(int &data, int address, int minValue, int maxValue, int defaultValue)
{
    EEPROM_GET_DATA(address, data);

    if (data < minValue || data > maxValue)
    {
        data = defaultValue;
        EEPROM_SET_DATA(address, data);
    }
}

void LedManager::readPreviousEEPROMData(uint16_t &data, int address)
{
    EEPROM_GET_DATA(address, data);
}

void LedManager::restoreSettings()
{
    readPreviousEEPROMData(mType, EEPROM_ADDR_LED_TYPE, OFF, EFFECT_MAX - 1, DEFAULT_TYPE);
    readPreviousEEPROMData(mBrightness, EEPROM_ADDR_LED_BRIGHTNESS, 0, 100, DEFAULT_BRIGHTNESS);
    readPreviousEEPROMData(mSaturation, EEPROM_ADDR_LED_SATURATION, 0, 100, DEFAULT_SATURATION);
    readPreviousEEPROMData(mSensitivity, EEPROM_ADDR_LED_SENSITIVITY, 0, 100, DEFAULT_SENSITIVITY);
    readPreviousEEPROMData(mSubType, EEPROM_ADDR_LED_SUB_TYPE, 0, 100, 0);
    readPreviousEEPROMData(mGHue, EEPROM_ADDR_LED_GHUE);
    readPreviousEEPROMData(mDHue, EEPROM_ADDR_LED_DHUE);
}

void LedManager::setType(int type, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mType != type)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_TYPE, type);
    }
#endif

    if (mType != type || force)
    {
        mType = type;
        LOG_LED("Change type: %d, subType: %d, brightness: %d, sensitivity: %d", mType, mSubType, mBrightness, mSensitivity);

        if (mType != MUSIC)
        {
            if (HardwareController::getInstance()->getAdcMode() == ADC_MODE_MIC)
            {
                HardwareController::getInstance()->changeAdcMode(ADC_MODE_NONE);
            }
        }
        if (mType == MUSIC)
        {
            if (mSubType < MUSIC_TYPE_1_SIDE || mSubType > MUSIC_TYPE_FULL_SIDES)
            {
                setSubType(MUSIC_TYPE_1_SIDE);
            }
        }
        turnOff();
    }
}

void LedManager::setSubType(int subType, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mSubType != subType)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_SUB_TYPE, mSubType);
    }
#endif

    if (mSubType != subType || force)
    {
        mSubType = subType;
        turnOff();
    }
}

void LedManager::setBrightness(int brightness, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mBrightness != brightness)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_BRIGHTNESS, brightness);
    }
#endif

    if (mBrightness != brightness || force)
    {
        mBrightness = brightness;
        strip->setBrightness(mBrightness);
    }
}

void LedManager::setSaturation(int saturation, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mSaturation != saturation)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_SATURATION, saturation);
    }
#endif

    if (mSaturation != saturation || force)
    {
        mSaturation = saturation;
    }
}

void LedManager::setSensitivity(int sensitivity, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mSensitivity != sensitivity)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_SENSITIVITY, sensitivity);
    }
#endif

    if (mSensitivity != sensitivity || force)
    {
        mSensitivity = sensitivity;

        if (mType == MUSIC)
        {
            mScale = mSensitivity * (FFT_SCALE_MAX - FFT_SCALE_MIN) / 100.0 + FFT_SCALE_MIN;
        }
    }
}

void LedManager::setHue(uint16_t hue, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mGHue != hue)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_GHUE, hue);
    }
#endif

    if (mGHue != hue || force)
    {
        mGHue = hue;
    }
}

void LedManager::setDeviation(int deviation, bool force)
{
#ifdef RESTORE_PREVIOUS_DATA
    if (mDHue != deviation)
    {
        EEPROM_SET_DATA(EEPROM_ADDR_LED_DHUE, deviation);
    }
#endif

    if (mDHue != deviation || force)
    {
        mDHue = deviation;
    }
}

void LedManager::process()
{
    switch (mType)
    {
    case RGB:
    {
        rgbEffectHandler();
        break;
    }
#ifdef ENABLE_MPU6050_SENSOR
    case GRAVITY:
    {
        gravityEffectHandler();
        break;
    }
#endif
    case MUSIC:
    {
        musicEffectHandler();
        break;
    }
    default:
    {
        break;
    }
    }
}

void LedManager::rgbEffectHandler()
{
    static unsigned long long time = 0;
    static uint16_t hue = 0;
    static int x, y, z;
    static int a = 1, b = 1, c = 1;
    static int d = -(a * x + b * y + c * z);
    static int dHue = 100;

    if ((unsigned long long)(millis() - time) > 10UL)
    {
        time = millis();
        hue += dHue;
        for (int i = 0; i < NUM_LEDS; i++)
        {
            if (PixelCoordinate::getDescartesPositions(i, &x, &y, &z))
            {
                int distance = a * x + b * y + c * z;
                setLed(i, 1, strip->ColorHSV(distance * 500 + hue, mSaturation));
            }
        }
        strip->show();
    }
}

#ifdef ENABLE_MPU6050_SENSOR
void LedManager::gravityEffectHandler()
{
    static unsigned long long time = 0;
    static uint16_t hue = 0;
    static int dHue = 100;
    static int x, y, z;
    static int pointX = 4, pointY = 4, pointZ = 4;

    if ((unsigned long long)(millis() - time) > 10UL)
    {
        time = millis();
        float angleX = HardwareController::getInstance()->getAngleX();
        float angleY = HardwareController::getInstance()->getAngleY();
        float angleZ = HardwareController::getInstance()->getAngleZ();
        float offset = -angleX * pointX - angleY * pointY - angleZ * pointZ;

        hue = HUE_BLUE;
        for (int i = 0; i < NUM_LEDS; i++)
        {
            if (PixelCoordinate::getDescartesPositions(i, &x, &y, &z))
            {
                bool enable = angleX * x + angleY * y + angleZ * z + offset > 0;
                setLed(i, enable, strip->ColorHSV(hue, mSaturation));
            }
            hue += 50;
        }
        strip->show();
    }
}
#endif

void LedManager::musicEffectHandler()
{
    static unsigned long long time = 0, time2 = 0, time3 = 0, time4 = 0;
    static int x, y, z;
    static int index = 0;
    static int scale[MATRIX_SIZE_1D] = {26, 28, 30, 32, 34, 36, 38, 40};
    static int fftLevel[MATRIX_SIZE_1D] = {0, 0, 0, 0, 0, 0, 0, 0};
    static double fftData[FFT_SAMPLES], fftVReal[FFT_SAMPLES], fftVImag[FFT_SAMPLES];

    // Change ADC mode to MIC after every 100ms
    if (HardwareController::getInstance()->getAdcMode() != ADC_MODE_MIC)
    {
        if ((unsigned long long)(millis() - time4) > 100UL)
        {
            time4 = millis();
            if (HardwareController::getInstance()->isAdcAvailable())
            {
                LOG_LED("Change ADC mode to MUSIC");
                HardwareController::getInstance()->changeAdcMode(ADC_MODE_MIC);
            }
        }
        return;
    }

    if ((unsigned long long)(millis() - time3 > 1))
    {
        index = (index + 1) % FFT_SAMPLES;
        fftData[index] = HardwareController::getInstance()->getAdc();
        time3 = millis();
    }

    if ((unsigned long long)(millis() - time2 > 50UL))
    {
        for (int i = 0; i < FFT_SAMPLES; i++)
        {
            fftVReal[i] = fftData[i];
            fftVImag[i] = 0;
        }
        FFT->Windowing(fftVReal, FFT_SAMPLES, FFT_WIN_TYP_HAMMING, FFT_FORWARD);
        FFT->Compute(fftVReal, fftVImag, FFT_SAMPLES, FFT_FORWARD);
        FFT->ComplexToMagnitude(fftVReal, fftVImag, FFT_SAMPLES);

        for (int i = 0; i < MATRIX_SIZE_1D; i++)
        {
            fftLevel[i] = 0;
            for (int j = FFT_BLOCK_OFFSET + i * FFT_BLOCK_SIZE; j < FFT_BLOCK_OFFSET + (i + 1) * FFT_BLOCK_SIZE; j++)
            {
                fftLevel[i] += fftVReal[j];
            }
            fftLevel[i] *= mScale / scale[i];
        }
        // LOG_LED("fft -> %d %d %d %d %d %d %d %d", fftLevel[0], fftLevel[1], fftLevel[2], fftLevel[3], fftLevel[4], fftLevel[5], fftLevel[6], fftLevel[7]);
        time2 = millis();
    }

    if ((unsigned long long)(millis() - time) > 10UL)
    {
        for (int i = 0; i < NUM_LEDS; i++)
        {
            int side = i / MATRIX_SIZE_2D;
            if (mSubType == MUSIC_TYPE_FULL_SIDES ||
                (mSubType == MUSIC_TYPE_4_SIDES && (side != 0 && side != 4)) ||
                (mSubType == MUSIC_TYPE_1_SIDE && side == 1))
            {
                if (PixelCoordinate::getDescartesPositions(i, &x, &y, &z))
                {
                    int level = z;
                    int pos = 0; // 0-7
                    switch (side)
                    {
                    case 0:
                    case 4:
                        level = x;
                        pos = y - 1;
                        break;
                    case 1:
                        pos = y - 1;
                        break;
                    case 2:
                        pos = x - 1;
                        break;
                    case 3:
                        pos = MATRIX_SIZE_1D - y;
                        break;
                    case 5:
                        pos = MATRIX_SIZE_1D - x;
                        break;
                    default:
                        break;
                    }
                    setLed(i, level <= fftLevel[pos], strip->ColorHSV(mGHue + mDHue * level + mDHue * (x + y) / 3, mSaturation));
                }
            }
        }
        strip->show();
        time = millis();
    }
}

void LedManager::changeToNextType()
{
    setType((mType + 1) % EFFECT_MAX);
}

void LedManager::fillColor(uint16_t hue, uint8_t sat, uint8_t val)
{
    for (int i = 0; i < NUM_LEDS; i++)
    {
        strip->setPixelColor(i, strip->gamma32(strip->ColorHSV(hue, sat, val)));
    }
    strip->show();
}

void LedManager::fillRainbowColor(uint16_t startHue, uint16_t dHue, uint8_t sat, uint8_t val)
{
    for (int i = 0; i < NUM_LEDS; i++)
    {
        strip->setPixelColor(i, strip->gamma32(strip->ColorHSV(startHue, sat, val)));
        startHue += dHue;
    }
    strip->show();
}

void LedManager::turnOff()
{
    fillColor(0, 0, 0);
}

void LedManager::setLed(int position, bool enable, uint32_t color)
{
    if (position >= 0 && position < NUM_LEDS)
    {
        strip->setPixelColor(position, enable ? strip->gamma32(color) : 0);
    }
}

void LedManager::setLed(int x, int y, int z, bool enable, uint32_t color)
{
    int position = PixelCoordinate::getArrayPosition(x, y, z);
    if (position >= 0)
    {
        setLed(position, enable, color);
    }
}