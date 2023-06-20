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
}

void LedManager::init()
{
    pinMode(LED_DATA_PIN, OUTPUT);
    FastLED.addLeds<LED_TYPE, LED_DATA_PIN, LED_COLOR_ORDER>(leds, NUM_LEDS);
    // FastLED.setCorrection(TypicalLEDStrip(LED_TYPE), TypicalSMD5050);
}