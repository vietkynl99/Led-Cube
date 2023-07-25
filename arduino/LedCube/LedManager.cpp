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
    mType = 0;
    mBrightness = -1;
    strip = new Adafruit_NeoPixel(NUM_LEDS, LED_DATA_PIN, LED_TYPE);
}

void LedManager::init()
{
    strip->begin();
    strip->setBrightness(20);
    strip->show();

    turnOff();
}

void LedManager::process()
{
    static unsigned long long time = 0;
    static uint16_t hue = 0;
    static int x, y, z;
    static int a = 1, b = 1, c = 1;
    static int pointX = 4, pointY = 4, pointZ = 4;
    static int d = -(a * x + b * y + c * z);
    static int dHue = 100;

    if ((unsigned long long)(millis() - time) > 10UL)
    {
        time = millis();
        switch (mType)
        {
        case RGB:
        {
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
            break;
        }
        case GRAVITY:
        {
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
            break;
        }
        default:
        {
            break;
        }
        }
    }
}

void LedManager::setType(int type)
{
    if (mType != type)
    {
        mType = type;
        turnOff();
    }
}

void LedManager::setBrightness(int brightness)
{
    if (mBrightness != brightness)
    {
        mBrightness = brightness;
        strip->setBrightness(mBrightness);
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