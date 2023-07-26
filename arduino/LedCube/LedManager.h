#ifndef _LED_MANAGER_H_
#define _LED_MANAGER_H_

#include <Adafruit_NeoPixel.h>
#include <arduinoFFT.h>
#include "VLog.h"
#include "PixelCoordinate.h"
#include "HardwareController.h"

#define LED_TYPE            NEO_GRB + NEO_KHZ800
#define LED_DATA_PIN        D5

#define NUM_LEDS            384     // 6x8x8 leds
#define MATRIX_SIZE_1D      8       // 8x8 matrix
#define MATRIX_SIZE_2D      64      // 8x8
#define MATRIX_SIDES        6       // 6 sides

#define HUE_RED             0
#define HUE_GREEN           21845
#define HUE_BLUE            43690

#define FFT_SAMPLE_FREQ     1000
#define FFT_SAMPLES         64
#define FFT_OUTPUT_SIZE     32      // FFT_SAMPLES / 2
#define FFT_BLOCK_OFFSET    2
#define FFT_BLOCK_SIZE      3       // <= (FFT_OUTPUT_SIZE - FFT_BLOCK_OFFSET) / MATRIX_SIZE_1D
#define FFT_SCALE           1.2

#define RESTORE_PREVIOUS_DATA

class LedManager
{
private:
    enum EffectType
    {
        OFF,
        RGB,
        MUSIC,
        WAVE,
        FLASH,
        GRAVITY,
        EFFECT_MAX
    };

    static LedManager *instance;
    Adafruit_NeoPixel *strip;
    int mType;
    int mBrightness;

    arduinoFFT *FFT;

private:
    LedManager();

public:
    static LedManager *getInstance();
    void init();
    void process();
    void restoreSettings();
    void setType(int type);
    void setBrightness(int brightness);
    void rgbEffectHandler();
#ifdef ENABLE_MPU6050_SENSOR
    void gravityEffectHandler();
#endif
    void musicEffectHandler();
    void fillColor(uint16_t hue, uint8_t sat = 255, uint8_t val = 255);
    void fillRainbowColor(uint16_t startHue, uint16_t dHue, uint8_t sat = 255, uint8_t val = 255);
    void turnOff();
    void setLed(int position, bool enable, uint32_t color);
    void setLed(int x, int y, int z, bool enable, uint32_t color);
};

#endif