#include <Arduino.h>
#include "ServiceManager.h"

ServiceManager::ServiceManager()
{
}

void ServiceManager::init()
{
    timeClient = new NTPClient(ntpUDP);
    timeClient->begin();
#ifdef NTP_TIME_OFFSET
    timeClient->setTimeOffset(NTP_TIME_OFFSET);
#endif
#ifdef NTP_UPDATE_INTERVAL
    timeClient->setUpdateInterval(NTP_UPDATE_INTERVAL);
#endif
}

void ServiceManager::updateRealTime()
{
    timeClient->update();
}

String ServiceManager::getRealTime()
{
    if (timeClient->isTimeSet())
    {
        return timeClient->getFormattedTime();
    }
    else
    {
        return "";
    }
}