/**
 * WiFiManager advanced, contains advanced configurartion options
 * Implements RESET_WIFI_PIN button press, press for ondemand configportal, hold for 3 seconds for reset settings.
 */

#include <UART_Debug.h>
#include "VLog.h"
#include "WifiMaster.h"
#include "ServerManager.h"
#include "ServiceManager.h"

/* Hardware pin*/
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

WifiMaster wifiMaster;
ServiceManager serviceManager;

bool pair_mode = false, pair_mode_fake = false;

// ............................................................................................................................
void checkResetWifiButton()
{
	static bool status_old = 0, status_new = 0;
	static unsigned long long state_changed_time = 0;

	status_old = status_new;
	status_new = !digitalRead(RESET_WIFI_PIN);
	if (status_old != status_new)
	{
		state_changed_time = millis();
	}

	if (status_new)
	{
		if ((unsigned long long)(millis() - state_changed_time) > LONG_PRESS_TIME)
		{
			LOG_WIFI("Reset button pressed");
			wifiMaster.resetWifiSettings();
		}
	}
}

void checkPairButton()
{
	static bool status_old = 0, status_new = 0;
	static unsigned long long pair_start_time = 0, pair_time = 0;

	status_old = status_new;
	status_new = !digitalRead(PAIR_MODE_PIN);
	if (status_old != status_new)
	{
		pair_start_time = millis();
	}

	if (status_new)
	{
		pair_time = millis() - pair_start_time;
		pair_mode = (pair_time > LONG_PRESS_TIME) && (pair_time < PAIR_MODE_TIMEOUT);
	}
	else
	{
		pair_mode = false;
	}
}

void fakePairMode()
{
	static unsigned long long fake_pair_time = 0;
	// make fake pair mode within 15s
	LOG_SERVER("Turn on fake pair mode");
	pair_mode_fake = 1;
	fake_pair_time = millis();
	if ((unsigned long long)(millis() - fake_pair_time) > PAIR_MODE_TIMEOUT)
	{
		LOG_SERVER("Turn off fake pair mode");
		pair_mode_fake = 0;
	}
}

bool isPairingMode()
{
	return pair_mode || pair_mode_fake;
}

void checkPairMode()
{
	static bool pre_pair_mode = false;
	bool pair_mode = isPairingMode();
	if (pre_pair_mode != pair_mode)
	{
		LOG_SERVER("Pair mode changed to: %d", pair_mode);
		ServerManager::isPairMode = pair_mode;
	}
	pre_pair_mode = pair_mode;
}

void checkWifiStatus()
{
	static int pre_status = -1;
	int status = WiFi.status();
	if (status != pre_status)
	{
		LOG_WIFI("WiFi status changed to %d", status);
	}
	pre_status = status;
}

#if USE_SERIAL_DEBUG
void debugHandler()
{
	static char FC[20];
	static long code;

	if (UART_Debug(FC, &code))
	{
		// Reset wifi settings
		if (!strcmp(FC, "RSWIFI"))
		{
			wifiMaster.resetWifiSettings();
		}
		// Reset API KEY
		else if (!strcmp(FC, "RSKEY"))
		{
			ServerManager::resetApiKey();
		}
		// Fake pair mode
		else if (!strcmp(FC, "PAIR"))
		{
			fakePairMode();
		}
	}
}

void initSerial()
{
	Serial.begin(115200);
	delay(2000);
	Serial.println();
}
#endif

void initHardwarePin()
{
	pinMode(RESET_WIFI_PIN, INPUT_PULLUP);
	pinMode(PAIR_MODE_PIN, INPUT_PULLUP);
}

void initEEPROM()
{
	EEPROM.begin(EEPROM_SIZE);
}

// ............................................................................................................................

void setup()
{
	initHardwarePin();
#if USE_SERIAL_DEBUG
	initSerial();
#endif
	initEEPROM();
	wifiMaster.init();
	ServerManager::init();
	serviceManager.init();
}

void loop()
{
#if USE_SERIAL_DEBUG
	debugHandler();
#endif

	checkResetWifiButton();
	checkPairButton();
	checkPairMode();
	checkWifiStatus();

	wifiMaster.process();
	serviceManager.updateRealTime();
	ServerManager::handleClient();
}
