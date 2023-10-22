#ifndef _LED_MANAGER_H_
#define _LED_MANAGER_H_

#include <Adafruit_NeoPixel.h>
#include <arduinoFFT.h>
#include "VLog.h"
#include "PixelCoordinate.h"
#include "HardwareController.h"
#include "SnakeGameManager.h"
#include "Characters.h"

#define LED_TYPE            NEO_GRB + NEO_KHZ800
#define LED_DATA_PIN        D5

#define NUM_LEDS            384     // 6x8x8 leds
#define MATRIX_SIZE_1D      8       // 8x8 matrix
#define MATRIX_SIZE_2D      64      // 8x8
#define MATRIX_SIDES        6       // 6 sides

#define HUE_RED             0
#define HUE_GREEN           21845
#define HUE_BLUE            43690

#define RENDER_DELAY_TIME   10UL   // fps = 1/(RENDER_DELAY_TIME+10)

#define FFT_SAMPLE_FREQ     1000
#define FFT_SAMPLES         64
#define FFT_OUTPUT_SIZE     32      // FFT_SAMPLES / 2
#define FFT_BLOCK_OFFSET    2
#define FFT_BLOCK_SIZE      3       // <= (FFT_OUTPUT_SIZE - FFT_BLOCK_OFFSET) / MATRIX_SIZE_1D
#define FFT_SCALE_MIN       0.2
#define FFT_SCALE_MAX       2.0

#define RESTORE_PREVIOUS_DATA

#define DEFAULT_TYPE        OFF
#define DEFAULT_BRIGHTNESS  10
#define DEFAULT_SATURATION  100
#define DEFAULT_SENSITIVITY 50

#define PRIORITY_TEXT_SIZE  32

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
        SNAKE,
        EFFECT_MAX
    };

    enum SubEffectType
    {
        SUB_TYPE_NONE,
        // Music type
        MUSIC_SUB_TYPE_MIN,
        MUSIC_SUB_TYPE_1_SIDE,
        MUSIC_SUB_TYPE_4_SIDES,
        MUSIC_SUB_TYPE_FULL_SIDES,
        MUSIC_SUB_TYPE_MAX,
        // Snake type
        SNAKE_SUB_TYPE_MIN,
        SNAKE_SUB_TYPE_1_SIDE,
        SNAKE_SUB_TYPE_FULL_SIDES,
        SNAKE_SUB_TYPE_MAX,
        // End
        SUB_EFFECT_MAX
    };

    enum PriorityType
    {
        PRIORITY_TYPE_NONE,
        PRIORITY_TYPE_WRONG_WARNING,
        PRIORITY_TYPE_SCROLL_TEXT,
        PRIORITY_TYPE_ATTENTION_SIDES
    };

    static LedManager *instance;
    Adafruit_NeoPixel *strip;
    bool mRender;
    int mType;
    int mBrightness;
    int mSaturation;
    int mSensitivity;
    float mScale;
    int mSubType;
    uint16_t mGHue;
    uint16_t mDHue;
    bool mfirstTime;
    int mPriorityMode;
    bool mPriorityModeFirstTime;
    char mPriorityText[PRIORITY_TEXT_SIZE];
    uint16_t mPriorityTextHue;
    int mPriorityParam1;
    int mPriorityParam2;
    arduinoFFT *FFT;

private:
    LedManager();

public:
    static LedManager *getInstance();
    void init();
    void setType(int type, bool force = false);
    void setSubType(int subType, bool force = false);
    void setBrightness(int brightness, bool force = false);
    void setSaturation(int saturation, bool force = false);
    void setSensitivity(int sensitivity, bool force = false);
    void setHue(uint16_t hue, bool force = false);
    void setDeviation(int deviation, bool force = false);
    void command(int commandType);
    void loop();
    void changeToNextType();
    void changeToNextSubType();

    void showPriorityWrongWarning();
    void showPriorityText(const char *text, bool vertical = true, uint16_t hue = HUE_RED);
    void showPriorityNumber(int number, bool vertical = true, uint16_t hue = HUE_RED);
    void showPriorityAttention(bool isFullMode);

private:
    bool isEffectEnable(int effectType);
    void readPreviousEEPROMData(int &data, int address, int minValue, int maxValue, int defaultValue);
    void readPreviousEEPROMData(uint16_t &data, int address);
    void restoreSettings();
    
    void showCharacterVertically(char character, uint16_t hue, int offset = 0);
    void showCharacterOnTop(char character, uint16_t hue, int offset = 0);
    void showCharacterVerticallyCenter(char character, uint16_t hue);
    void showString(char *str, uint16_t hue, int offset, bool vertical);
    void setPriorityMode(int mode);

    void resetEffect();
    void renderHandler();
    void priorityModeHandler();
    void rgbEffectHandler();
#ifdef ENABLE_MPU6050_SENSOR
    void gravityEffectHandler();
#endif
    void musicEffectHandler();
    void snakeEffectHandler();

    void fillColor(uint16_t hue, uint8_t sat = 255, uint8_t val = 255);
    void fillRainbowColor(uint16_t startHue, uint16_t dHue, uint8_t sat = 255, uint8_t val = 255);
    void turnOff();
    void setLedPosition(int position, bool enable, uint16_t hue, uint8_t sat = 255, uint8_t val = 255);
    void setLedCoordinates(int x, int y, int z, bool enable, uint16_t hue, uint8_t sat = 255, uint8_t val = 255);
};

#endif