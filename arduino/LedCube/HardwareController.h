#ifndef _HARDWARE_CONTROLLER_H
#define _HARDWARE_CONTROLLER_H

#include <EEPROM.h>
#include "VLog.h"
#include "WifiMaster.h"

// Press and hold for 3 seconds to reset wifi settings
#define RESET_WIFI_PIN 0
// Press and hold to switch to pair mode
#define PAIR_MODE_PIN 2

/* Button press time */
#define LONG_PRESS_TIME 3000UL
// (ms) Maximum time for pairing from the moment the pair button is pressed
#define PAIR_MODE_TIMEOUT 15000UL

/* EEPROM */
#define EEPROM_SIZE 16 // (bytes)

class HardwareController
{
private:
    static HardwareController *instance;
    bool pair_mode;
    bool fake_pair_mode;

private:
    HardwareController();
#if USE_SERIAL_DEBUG
    void initSerial();
#endif
    void initHardwarePin();
    void initEEPROM();
    void checkResetWifiButton();
    void checkPairButton();
    void checkFakePairMode();
    void checkPairMode();

public:
    static HardwareController *getInstance();
    void init();
    void turnOnFakePairMode();
    void process();
    bool isPairingMode();
};

#endif