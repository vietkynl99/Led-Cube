#ifndef _HARDWARE_CONTROLLER_H
#define _HARDWARE_CONTROLLER_H

#include <Arduino.h>
#include <EEPROM.h>
#include <DHT_Async.h>
#include <MPU6050_tockn.h>
#include <Wire.h>
#include "VLog.h"
#include "WifiMaster.h"
#include "ServerManager.h"

/*
Use a single button to control the system:
Long press about 3s: Switch to Pair mode
Press and then long press for about 3s: Reset wifi settings
*/

#define ENABLE_BATTERY_MEASUREMENT
#define ENABLE_DHT11_SENSOR
// #define ENABLE_MPU6050_SENSOR

#define ADC_PIN         A0
#define ADC_CTRL_PIN    D3
#define BUTTON_PIN      D4
#define BUZZER_PIN      D6
#define DHT11_PIN       D7

#define BUTTON_SCAN_TIME                20UL    // ms
#define BUTTON_SHORT_PRESS_TIME_MIN     50UL  // ms
#define BUTTON_SHORT_PRESS_TIME_MAX     300UL  // ms
#define BUTTON_LONG_PRESS_TIME          3000UL  // ms
#define BUTTON_DOUBLE_LONG_PRESS_TIME   1000UL  // ms
#define BUTTON_PAIR_MODE_TIMEOUT        15000UL // ms

#define BUZZER_OUTPUT_TIME              60UL    // ms
#define BUZZER_PWM_MAX                  150

#define DHT11_SCAN_TIME                 120000UL // ms

#define BATTERY_SCAN_TIME               60000UL // ms
#define BATTERY_MEASURE_COUNT_MAX       20
#define BATTERY_MAX_VOLTAGE             8.4     // V
#define BATTERY_MIN_VOLTAGE             6.4     // V

#define EEPROM_ADDR_API_KEY             0
#define EEPROM_ADDR_LED_TYPE            4
#define EEPROM_ADDR_LED_BRIGHTNESS      8
#define EEPROM_ADDR_LED_SATURATION      12
#define EEPROM_ADDR_LED_SENSITIVITY     16
#define EEPROM_ADDR_LED_SUB_TYPE        20
#define EEPROM_ADDR_LED_GHUE            24
#define EEPROM_ADDR_LED_DHUE            28
#define EEPROM_SIZE_MAX                 32      // bytes

#define EEPROM_SET_DATA(ADDR, VALUE)    {EEPROM.put(ADDR, VALUE); EEPROM.commit();}
#define EEPROM_GET_DATA(ADDR, VALUE)    {EEPROM.get(ADDR, VALUE);}

#define ADC_MODE_NONE       0
#define ADC_MODE_MIC        1
#define ADC_MODE_BATTERY    2

#define MPU6050_OFFSET_X_DEFAULT 1
#define MPU6050_OFFSET_Y_DEFAULT 12
#define MPU6050_OFFSET_Z_DEFAULT 0

class HardwareController
{
private:
    static HardwareController *instance;
    bool mPairMode;
    bool mFakePairMode;
    int mBeepPlayingCount;
    int mAdcMode;

    int mBatteryLevel;

#ifdef ENABLE_DHT11_SENSOR
    DHT_Async *dhtSensor;
    int mTemperature;
    int mHumidity;
#endif

#ifdef ENABLE_MPU6050_SENSOR
    MPU6050 *mpuSensor;
    float angleXOffset;
    float angleYOffset;
    float angleZOffset;
#endif

private:
    HardwareController();
#if USE_SERIAL_DEBUG
    void initSerial();
#endif
    void initHardwarePin();
    void initEEPROM();
    void initSensors();
    void fakePairModeHandler();
    void checkPairMode();
    void setBeepState(bool state);
    void beepHandler();
    void buttonHandler();
    void measureBattery();
#ifdef ENABLE_DHT11_SENSOR
    void processDHT();
#endif
#ifdef ENABLE_MPU6050_SENSOR
    void processMPU();
#endif

public:
    static HardwareController *getInstance();
    void init();
    void loop();
    void turnOnFakePairMode();
    bool isPairingMode();
    void beep(int count, bool blocking = false);
    String getSensorsData();
#ifdef ENABLE_MPU6050_SENSOR
    float getAngleDegX();
    float getAngleDegY();
    float getAngleDegZ();
    float getAngleRadX();
    float getAngleRadY();
    float getAngleRadZ();
#endif
    int getAdc();
    int getAdcMode();
    bool isAdcAvailable();
    void changeAdcMode(int mode);
};

#endif