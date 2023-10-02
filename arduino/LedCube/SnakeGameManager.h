#ifndef _SNAKE_GAME_MANAGER_H_
#define _SNAKE_GAME_MANAGER_H_

#include "VLog.h"
#include "PixelCoordinate.h"
#include "HardwareController.h"
#include "LedManager.h"

#define DATA_SIZE_MAX       384 // NUM_LEDS

#define START_DEFAULT_X     0
#define START_DEFAULT_Y     4
#define START_DEFAULT_Z     4

enum NextModeCode {
    NEXT_MOVE_CODE_NONE,
    NEXT_MOVE_CODE_GAME_OVER,
    NEXT_MOVE_CODE_WIN_GAME,
    NEXT_MOVE_CODE_NORMAL,
    NEXT_MOVE_CODE_PLUS,
    NEXT_MOVE_CODE_MAX
};

class SnakeGameManager
{
private:
    static SnakeGameManager *instance;
    int mDataArray[DATA_SIZE_MAX];
    int mLength;
    int mLengthMax;
    int mFristIndex;
    int mLastIndex;
    int mDirX;
    int mDirY;
    int mDirZ;
    int mX;
    int mY;
    int mZ;
    int mTargetX;
    int mTargetY;
    int mTargetZ;

private:
    SnakeGameManager();

public:
    static SnakeGameManager *getInstance();
    void setGameLevel(int level);
    void startGame();
    void resetGame();
    void setDir(int dirX, int dirY, int dirZ);
    int nextMove(int &setX, int &setY, int &setZ, int &clearX, int &clearY, int &clearZ);
    void command(int command);
    void getCurrentPosition(int &x, int &y, int &z);
    void getTargetPosition(int &x, int &y, int &z);

private:
    void setLengthMax(int max);
    int getLength();
    bool add(int value);
    int pop();
    bool isFull();
    bool isExists(int value);
    int generateRandomUnvailableValue(int min, int max);
    int getPanelPosition(int x, int y, int z);
};

#endif