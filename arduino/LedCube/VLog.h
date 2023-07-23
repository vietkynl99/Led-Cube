#ifndef _VLOG_H_
#define _VLOG_H_

#define USE_SERIAL_DEBUG 1

#if USE_SERIAL_DEBUG
#define LOG(TAG, ...) VLog::print(TAG, __VA_ARGS__);
#define LOG_WIFI(...) LOG("WIFI", __VA_ARGS__)
#define LOG_SYSTEM(...) LOG("SYSTEM", __VA_ARGS__)
#define LOG_SERVER(...) LOG("SERVER", __VA_ARGS__)
#define LOG_LED(...) LOG("LED", __VA_ARGS__)
#else
#define LOG(TAG, MSG)
#define LOG_WIFI(MSG)
#define LOG_SYSTEM(MSG)
#define LOG_SERVER(MSG)
#endif

class VLog
{
public:
    static void print(const char *tag, const char *pFormat, ...);
};
#endif