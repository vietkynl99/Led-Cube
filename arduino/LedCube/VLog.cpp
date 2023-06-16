#include <Arduino.h>
#include "VLog.h"

void VLog::print(const char *tag, const char *pFormat, ...)
{
    static char mStringBuffer[256];

    va_list pVlist;
    va_start(pVlist, pFormat);
    vsnprintf(mStringBuffer, sizeof(mStringBuffer) - 1, pFormat, pVlist);
    va_end(pVlist);
    Serial.print("[");
    Serial.print(tag);
    Serial.print("] ");
    Serial.println(mStringBuffer);
}