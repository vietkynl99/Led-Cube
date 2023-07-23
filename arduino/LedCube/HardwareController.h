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

// #define ENABLE_DHT11_SENSOR

#define ADC_PIN A0
#define ADC_CTRL_PIN D3
#define BUTTON_PIN D4
#define BUZZER_PIN D6
#define DHT11_PIN D7

#define BUTTON_SCAN_TIME 20UL                // ms
#define BUTTON_LONG_PRESS_TIME 3000UL        // ms
#define BUTTON_DOUBLE_LONG_PRESS_TIME 1000UL // ms
#define BUTTON_PAIR_MODE_TIMEOUT 15000UL     // ms

#define BUZZER_OUTPUT_TIME 60UL // ms
#define BUZZER_PWM_MAX 150

#define DHT11_SCAN_TIME 120000UL // ms

#define BATTERY_SCAN_TIME 60000UL // ms
#define BATTERY_MEASURE_COUNT_MAX 10
#define BATTERY_MAX_VOLTAGE 8.4 // V
#define BATTERY_MIN_VOLTAGE 6.4 // V

#define EEPROM_SIZE 16 // bytes

class HardwareController
{
private:
    static HardwareController *instance;
    bool mPairMode;
    bool mFakePairMode;
    int mBeepPlayingCount;
    bool isMeasuringBattery;

    DHT_Async *dhtSensor;
    int mTemperature;
    int mHumidity;
    int mBatteryLevel;

    MPU6050 *mpuSensor;
    float angleXOffset;
    float angleYOffset;
    float angleZOffset;

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
    void processDHT();
    void processMPU();

public:
    static HardwareController *getInstance();
    void init();
    void process();
    void turnOnFakePairMode();
    bool isPairingMode();
    void beep(int count, bool blocking = false);
    String getSensorsData();
    float getAngleX();
    float getAngleY();
    float getAngleZ();
};

#endif