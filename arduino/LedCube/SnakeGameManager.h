#ifndef _SNAKE_GAME_MANAGER_H_
#define _SNAKE_GAME_MANAGER_H_

#include <Arduino.h>
#include <avr/pgmspace.h>
#include "VLog.h"
#include "PixelCoordinate.h"
#include "HardwareController.h"
#include "LedManager.h"

#define DATA_SIZE_MAX       384 // NUM_LEDS

#define START_DEFAULT_X     4
#define START_DEFAULT_Y     4
#define START_DEFAULT_Z     9

#define GYRO_THRESHOLD      250

enum NextModeCode
{
    NEXT_MOVE_CODE_NONE,
    NEXT_MOVE_CODE_GAME_OVER,
    NEXT_MOVE_CODE_WIN_GAME,
    NEXT_MOVE_CODE_NORMAL,
    NEXT_MOVE_CODE_PLUS,
    NEXT_MOVE_CODE_MAX
};

enum DirMode
{
    DIR_MODE_NONE,
    DIR_MODE_X_DEC,
    DIR_MODE_Y_DEC,
    DIR_MODE_Z_DEC,
    DIR_MODE_X_INC,
    DIR_MODE_Y_INC,
    DIR_MODE_Z_INC,
    DIR_MODE_MAX
};

enum GameMode
{
    GAME_MODE_1_SIDE,
    GAME_MODE_FULL_SIDES
};

PROGMEM const unsigned char progmemMatrixDir[6][6] =
    {{DIR_MODE_Y_DEC, DIR_MODE_Y_INC, DIR_MODE_X_INC, DIR_MODE_X_DEC, DIR_MODE_NONE, DIR_MODE_NONE},  // z = 0
     {DIR_MODE_NONE, DIR_MODE_NONE, DIR_MODE_Z_DEC, DIR_MODE_Z_INC, DIR_MODE_Y_DEC, DIR_MODE_Y_INC},  // x = 0
     {DIR_MODE_Z_INC, DIR_MODE_Z_DEC, DIR_MODE_NONE, DIR_MODE_NONE, DIR_MODE_X_INC, DIR_MODE_X_DEC},  // y = 0
     {DIR_MODE_NONE, DIR_MODE_NONE, DIR_MODE_Z_INC, DIR_MODE_Z_DEC, DIR_MODE_Y_INC, DIR_MODE_Y_DEC},  // x = 9
     {DIR_MODE_Y_INC, DIR_MODE_Y_DEC, DIR_MODE_X_DEC, DIR_MODE_X_INC, DIR_MODE_NONE, DIR_MODE_NONE},  // z = 9
     {DIR_MODE_Z_DEC, DIR_MODE_Z_INC, DIR_MODE_NONE, DIR_MODE_NONE, DIR_MODE_X_DEC, DIR_MODE_X_INC}}; // y = 9

class SnakeGameManager
{
private:
    static SnakeGameManager *instance;
    int mDataArray[DATA_SIZE_MAX];
    int mLength;
    int mLengthMax;
    int mFristIndex;
    int mLastIndex;
    int mDir;
    int mDirX;
    int mDirY;
    int mDirZ;
    int mX;
    int mY;
    int mZ;
    int mTargetX;
    int mTargetY;
    int mTargetZ;
    int mGameMode;

private:
    SnakeGameManager();

public:
    static SnakeGameManager *getInstance();
    void setGameMode(int mode);
    void startGame();
    void resetGame();
    void setDir(int dir, bool force = false);
#ifdef ENABLE_MPU6050_SENSOR
    void handleDirByMpu();
#endif
    int nextMove(int &setX, int &setY, int &setZ, int &clearX, int &clearY, int &clearZ);
    void command(int command);
    void getCurrentPosition(int &x, int &y, int &z);
    void getTargetPosition(int &x, int &y, int &z);

private:
    void setLengthMax(int max);
    bool add(int value);
    int pop();
    bool isFull();
    bool isExists(int value);
    int generateRandomPosition();
    void generateFirstTargetPosition();
    void setDirAxis(int dirX, int dirY, int dirZ);
    void detectCurrentDir();
    bool caculateNextDir(int &axis1, int &axis2, int &axist3, int &dir1, int &dir2, int &dir3);
};

#endif