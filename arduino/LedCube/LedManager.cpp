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
    mType = OFF;
    mBrightness = 0;
    strip = new Adafruit_NeoPixel(NUM_LEDS, LED_DATA_PIN, LED_TYPE);
}

void LedManager::init()
{
    strip->begin();
    turnOff();

#ifdef RESTORE_PREVIOUS_DATA
    restoreSettings();
#endif

    FFT = new arduinoFFT();

    // test
    setType(MUSIC);
}

void LedManager::restoreSettings()
{
    EEPROM_GET_DATA(EEPROM_ADDR_LED_TYPE, mType);
    EEPROM_GET_DATA(EEPROM_ADDR_LED_BRIGHTNESS, mBrightness);
    if (mType < OFF || mType >= EFFECT_MAX)
    {
        mType = OFF;
        EEPROM_SET_DATA(EEPROM_ADDR_LED_TYPE, mType);
    }
    if (mBrightness < 0 || mBrightness > 100)
    {
        mBrightness = 0;
        EEPROM_SET_DATA(EEPROM_ADDR_LED_BRIGHTNESS, mBrightness);
    }
    strip->setBrightness(mBrightness);
    LOG_LED("type: %d, brightness: %d", mType, mBrightness);
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

void LedManager::setType(int type)
{
    if (mType != type)
    {
        mType = type;
        if (mType != MUSIC)
        {
            if (HardwareController::getInstance()->getAdcMode() == ADC_MODE_MIC)
            {
                HardwareController::getInstance()->changeAdcMode(ADC_MODE_NONE);
            }
        }
        turnOff();
#ifdef RESTORE_PREVIOUS_DATA
        EEPROM_SET_DATA(EEPROM_ADDR_LED_TYPE, mType);
#endif
    }
}

void LedManager::setBrightness(int brightness)
{
    if (mBrightness != brightness)
    {
        mBrightness = brightness;
        strip->setBrightness(mBrightness);
#ifdef RESTORE_PREVIOUS_DATA
        EEPROM_SET_DATA(EEPROM_ADDR_LED_BRIGHTNESS, mBrightness);
#endif
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
                setLed(i, 1, strip->ColorHSV(distance * 500 + hue));
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
                setLed(i, enable, strip->ColorHSV(hue));
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
    static uint16_t hue = 0;
    static int x, y, z;
    static int pointX = 4, pointY = 4, pointZ = 4;
    static int dHue = 100;
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
            fftLevel[i] *= FFT_SCALE / scale[i];
        }
        // LOG_LED("fft -> %d %d %d %d %d %d %d %d", fftLevel[0], fftLevel[1], fftLevel[2], fftLevel[3], fftLevel[4], fftLevel[5], fftLevel[6], fftLevel[7]);
        time2 = millis();
    }

    if ((unsigned long long)(millis() - time) > 10UL)
    {
        hue = HUE_BLUE;
        for (int i = MATRIX_SIZE_2D; i < 2 * MATRIX_SIZE_2D; i++)
        {
            if (PixelCoordinate::getDescartesPositions(i, &x, &y, &z))
            {
                setLed(i, (y >= 1 && y <= MATRIX_SIZE_1D) ? z <= fftLevel[y - 1] + 1 : 0, strip->ColorHSV(hue));
            }
            hue += dHue;
        }
        strip->show();
        time = millis();
    }
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