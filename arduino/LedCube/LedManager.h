#ifndef _LED_MANAGER_H_
#define _LED_MANAGER_H_

#include <FastLED.h>

#define LED_TYPE WS2812B
#define LED_COLOR_ORDER GRB
#define LED_DATA_PIN 0

#define MATRIX_SIZE 8 // 8x8 matrix
#define NUM_LEDS 384  // 6x8x8 leds

class LedManager
{
private:
    static LedManager *instance;
    CRGB leds[NUM_LEDS];

private:
    LedManager();

public:
    static LedManager *getInstance();
    void init();
};

#endif