#include <SerialParser.h>
#include "VLog.h"
#include "HardwareController.h"
#include "WifiMaster.h"
#include "ServerManager.h"
#include "ServiceManager.h"
#include "LedManager.h"

HardwareController *hardwareController;
WifiMaster *wifiMaster;
ServiceManager *serviceManager;
LedManager *ledManager;

#if USE_SERIAL_DEBUG
void debugHandler()
{
	static char cmd[20];
	static long code;

	if (SerialParser::run(cmd, code))
	{
		// Reset wifi settings
		if (!strcmp(cmd, "RSWIFI"))
		{
			wifiMaster->resetWifiSettings();
		}
		// Reset API KEY
		else if (!strcmp(cmd, "RSKEY"))
		{
			ServerManager::resetApiKey();
		}
		// Fake pair mode
		else if (!strcmp(cmd, "PAIR"))
		{
			hardwareController->turnOnFakePairMode();
		}
		// Start in Snake Game
		else if (!strcmp(cmd, "START"))
		{
			LedManager::getInstance()->command(COMMAND_GAME_START);
		}
		// Set dir in Snake Game
		else if (!strcmp(cmd, "DIR"))
		{
			SnakeGameManager::getInstance()->setDir(code, true);
		}
	}
}
#endif

void setup()
{
	hardwareController = HardwareController::getInstance();
	ledManager = LedManager::getInstance();
	wifiMaster = WifiMaster::getInstance();
	serviceManager = ServiceManager::getInstance();

	hardwareController->init();
	ledManager->init();
	wifiMaster->init();
	ServerManager::init();
	// serviceManager->init();

#if USE_SERIAL_DEBUG
	SerialParser::setFeedbackEnable(true);
#endif

	LOG_SYSTEM("Start main loop");
}

void loop()
{
#if USE_SERIAL_DEBUG
	debugHandler();
#endif

	hardwareController->loop();
	ledManager->loop();
	wifiMaster->loop();
	// serviceManager->updateRealTime();
	ServerManager::loop();
}
