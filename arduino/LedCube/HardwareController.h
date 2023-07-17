#ifndef _HARDWARE_CONTROLLER_H
#define _HARDWARE_CONTROLLER_H

#include <EEPROM.h>
#include "VLog.h"
#include "WifiMaster.h"

/*
Use a single button to control the system:
Long press about 3s: Switch to Pair mode
Press and then long press for about 3s: Reset wifi settings
*/

// vol=(adc-255)/55

#define BUTTON_PIN D4
#define BUZZER_PIN D6

#define BUTTON_SCAN_TIME                20UL        // ms
#define BUTTON_LONG_PRESS_TIME          3000UL      // ms
#define BUTTON_DOUBLE_LONG_PRESS_TIME   1000UL      // ms
#define BUTTON_PAIR_MODE_TIMEOUT        15000UL     // ms

#define BUZZER_OUTPUT_TIME              60UL        // ms
#define BUZZER_PWM_MAX                  150

#define EEPROM_SIZE 16 // bytes

class HardwareController
{
private:
    static HardwareController *instance;
    bool pairMode;
    bool fakePairMode;
    int beepPlayingCount;

private:
    HardwareController();
#if USE_SERIAL_DEBUG
    void initSerial();
#endif
    void initHardwarePin();
    void initEEPROM();
    void fakePairModeHandler();
    void checkPairMode();
    void setBeepState(bool state);
    void beepHandler();
    void buttonHandler();

public:
    static HardwareController *getInstance();
    void init();
    void process();
    void turnOnFakePairMode();
    bool isPairingMode();
    void beep(int count, bool blocking=false);
};

#endif