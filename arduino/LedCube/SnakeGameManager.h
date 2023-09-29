#ifndef _SNAKE_GAME_MANAGER_H_
#define _SNAKE_GAME_MANAGER_H_

#include "VLog.h"
#include "PixelCoordinate.h"
#include "HardwareController.h"
#include "LedManager.h"

#define DATA_SIZE_MAX 384 // NUM_LEDS

class SnakeGameManager
{
private:
    static SnakeGameManager *instance;
    int mDataArray[DATA_SIZE_MAX];
    int mLength;
    int mLengthMax;
    int mFristIndex;
    int mLastIndex;

private:
    SnakeGameManager();

public:
    static SnakeGameManager *getInstance();
    void init();
    void setLengthMax(int max);
    int getLength();
    bool add(int value);
    int pop();
    bool isFull();
    bool isExists(int value);
    int generateRandomUnvailableValue(int min, int max);
};

#endif