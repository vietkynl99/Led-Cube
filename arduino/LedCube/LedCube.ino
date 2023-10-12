#include <UART_Debug.h>
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
	static char FC[20];
	static long code;

	if (UART_Debug(FC, &code))
	{
		// Reset wifi settings
		if (!strcmp(FC, "RSWIFI"))
		{
			wifiMaster->resetWifiSettings();
		}
		// Reset API KEY
		else if (!strcmp(FC, "RSKEY"))
		{
			ServerManager::resetApiKey();
		}
		// Fake pair mode
		else if (!strcmp(FC, "PAIR"))
		{
			hardwareController->turnOnFakePairMode();
		}
		// Start in Snake Game
		else if (!strcmp(FC, "START"))
		{
			LedManager::getInstance()->command(COMMAND_GAME_START);
		}
		// Set dir in Snake Game
		else if (!strcmp(FC, "DIR"))
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
