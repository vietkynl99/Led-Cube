#ifndef _LED_MANAGER_H_
#define _LED_MANAGER_H_

#include <Adafruit_NeoPixel.h>

#define LED_TYPE NEO_GRB + NEO_KHZ800
#define LED_DATA_PIN D5

#define MATRIX_SIZE 8 // 8x8 matrix
#define NUM_LEDS 384  // 6x8x8 leds

class LedManager
{
private:
    enum EffectType {
        OFF,
        RGB,
        MUSIC,
        WAVE,
        FLASH
    };

    static LedManager *instance;
    Adafruit_NeoPixel *strip;
    int mType;
    int mBrightness;

private:
    LedManager();

public:
    static LedManager *getInstance();
    void init();
    void process();
    void setType(int type);
    void setBrightness(int brightness);
    void fillColor(uint16_t hue, uint8_t sat = 255, uint8_t val = 255);
    void fillRainbowColor(uint16_t startHue, uint16_t dHue, uint8_t sat = 255, uint8_t val = 255);
    void turnOff();
};

#endif