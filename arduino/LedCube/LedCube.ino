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
	static String cmd;
	static long code;
	static String valueStr;

	if (SerialParser::run(&cmd, &code, &valueStr))
	{
		// Reset wifi settings
		if (cmd.equals("RSWIFI"))
		{
			wifiMaster->resetWifiSettings();
		}
		// Reset API KEY
		else if (cmd.equals("RSKEY"))
		{
			ServerManager::resetApiKey();
		}
		// Fake pair mode
		else if (cmd.equals("PAIR"))
		{
			hardwareController->turnOnFakePairMode();
		}
		// Start in Snake Game
		else if (cmd.equals("START"))
		{
			LedManager::getInstance()->command(COMMAND_GAME_START);
		}
		// Set dir in Snake Game
		else if (cmd.equals("DIR"))
		{
			SnakeGameManager::getInstance()->setDir(code, true);
		}
		// WF <ssid>,<password>
		else if (cmd.equals("WF"))
		{
			if (valueStr.length() > 0)
			{
				int index = valueStr.indexOf(',');
				if (index > 0)
				{
					String ssid = valueStr.substring(0, index);
					String password = valueStr.substring(index + 1);
					LOG_SYSTEM("Set wifi information: ssid: '%s', password: '%s'", ssid.c_str(), password.c_str());
					AsyncWiFiManager::setWifiInformation(ssid, password);
					ESP.restart();
				}
			}
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
