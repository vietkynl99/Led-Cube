#include <Arduino.h>
#include "SnakeGameManager.h"

SnakeGameManager *SnakeGameManager::instance = nullptr;

SnakeGameManager *SnakeGameManager::getInstance()
{
    if (instance == nullptr)
    {
        instance = new SnakeGameManager();
    }
    return instance;
}

SnakeGameManager::SnakeGameManager()
{
    resetGame();
}

void SnakeGameManager::resetGame()
{
    mLength = 0;
    mFristIndex = 0;
    mLastIndex = 0;
    mLengthMax = DATA_SIZE_MAX;
    mDirX = 0;
    mDirY = 0;
    mDirZ = 0;
    mX = START_DEFAULT_X;
    mY = START_DEFAULT_Y;
    mZ = START_DEFAULT_Z;
}

void SnakeGameManager::setLengthMax(int max)
{
    mLengthMax = max;
}

int SnakeGameManager::getLength()
{
    return mLength;
}

bool SnakeGameManager::add(int value)
{
    if (isFull())
    {
        return false;
    }
    mLength++;
    mLastIndex = (mLastIndex + 1) % DATA_SIZE_MAX;
    mDataArray[mLastIndex] = value;
    return true;
}

int SnakeGameManager::pop()
{
    if (mLength <= 0)
    {
        return -1;
    }
    mLength--;
    mFristIndex = (mFristIndex + 1) % DATA_SIZE_MAX;
    return mDataArray[mFristIndex];
}

bool SnakeGameManager::isFull()
{
    return mLength >= mLengthMax;
}

bool SnakeGameManager::isExists(int value)
{
    if (mFristIndex <= mLastIndex)
    {
        for (int i = mFristIndex; i <= mLastIndex; i++)
        {
            if (mDataArray[i] == value)
            {
                return true;
            }
        }
    }
    else
    {
        for (int i = 0; i <= mFristIndex; i++)
        {
            if (mDataArray[i] == value)
            {
                return true;
            }
        }
        for (int i = mLastIndex; i <= mLengthMax; i++)
        {
            if (mDataArray[i] == value)
            {
                return true;
            }
        }
    }
    return false;
}

int SnakeGameManager::generateRandomUnvailableValue(int min, int max)
{
    if (isFull())
    {
        return -1;
    }
    for (int i = 0; i < mLengthMax * 100; i++)
    {
        int value = random(min, max);
        if (!isExists(value))
        {
            return value;
        }
    }
    return -1;
}

int SnakeGameManager::getPanelPosition(int x, int y, int z)
{
    return (y - 1) * MATRIX_SIZE_1D + z - 1;
}

void SnakeGameManager::startGame()
{
    resetGame();
    setLengthMax(MATRIX_SIZE_2D);
    setDir(0, 0, 0);

    // start position
    int panelPosition = getPanelPosition(mX, mY, mZ);
    SnakeGameManager::getInstance()->add(panelPosition);

    // Generate frist target position
    mTargetX = 0;
    while (1)
    {
        mTargetY = random(1, MATRIX_SIZE_1D);
        mTargetZ = random(1, MATRIX_SIZE_1D);
        if (pow(mX - mTargetX, 2) + pow(mY - mTargetY, 2) + pow(mZ - mTargetZ, 2) > 3)
        {
            break;
        }
    }
}

void SnakeGameManager::setDir(int dirX, int dirY, int dirZ)
{
    mDirX = dirX;
    mDirY = dirY;
    mDirZ = dirZ;
}

int SnakeGameManager::nextMove(int &setX, int &setY, int &setZ, int &clearX, int &clearY, int &clearZ)
{
    if (!mDirX && !mDirY && !mDirZ)
    {
        return NEXT_MOVE_CODE_NONE;
    }
    if (mDirX)
    {
        mX += mDirX;
        if (mX > MATRIX_SIZE_1D)
        {
            mX = 1;
        }
        if (mX < 1)
        {
            mX = MATRIX_SIZE_1D;
        }
    }
    if (mDirY)
    {
        mY += mDirY;
        if (mY > MATRIX_SIZE_1D)
        {
            mY = 1;
        }
        if (mY < 1)
        {
            mY = MATRIX_SIZE_1D;
        }
    }
    if (mDirZ)
    {
        mZ += mDirZ;
        if (mZ > MATRIX_SIZE_1D)
        {
            mZ = 1;
        }
        if (mZ < 1)
        {
            mZ = MATRIX_SIZE_1D;
        }
    }
    int panelPosition = getPanelPosition(mX, mY, mZ);
    if (isExists(panelPosition))
    {
        LOG_GAME("Game over!");
        return NEXT_MOVE_CODE_GAME_OVER;
    }
    if (mX == mTargetX && mY == mTargetY && mZ == mTargetZ)
    {
        add(panelPosition);
        if (isFull())
        {
            LOG_GAME("Win game!");
            return NEXT_MOVE_CODE_WIN_GAME;
        }
        else
        {
            // eat
            int targetPanelPosition = generateRandomUnvailableValue(0, MATRIX_SIZE_2D);
            if (targetPanelPosition < 0)
            {
                LOG_GAME("Win game!");
                return NEXT_MOVE_CODE_WIN_GAME;
            }
            else
            {
                // generate new target position
                mTargetY = targetPanelPosition / MATRIX_SIZE_1D + 1;
                mTargetZ = targetPanelPosition % MATRIX_SIZE_1D + 1;
                setX = mX;
                setY = mY;
                setZ = mZ;
                return NEXT_MOVE_CODE_PLUS;
            }
        }
    }
    else
    {
        add(panelPosition);
        int firstPanelPosition = pop();
        if (firstPanelPosition >= 0)
        {
            clearX = mX;
            clearY = firstPanelPosition / MATRIX_SIZE_1D + 1;
            clearZ = firstPanelPosition % MATRIX_SIZE_1D + 1;
        }
        setX = mX;
        setY = mY;
        setZ = mZ;
        return NEXT_MOVE_CODE_NORMAL;
    }
}

void SnakeGameManager::command(int command)
{
    if (command == COMMAND_GAME_RIGHT)
    {
        setDir(0, 1, 0);
    }
    else if (command == COMMAND_GAME_UP)
    {
        setDir(0, 0, 1);
    }
    else if (command == COMMAND_GAME_LEFT)
    {
        setDir(0, -1, 0);
    }
    else if (command == COMMAND_GAME_DOWN)
    {
        setDir(0, 0, -1);
    }
}

void SnakeGameManager::getCurrentPosition(int &x, int &y, int &z)
{
    x = mX;
    y = mY;
    z = mZ;
}

void SnakeGameManager::getTargetPosition(int &x, int &y, int &z)
{
    x = mTargetX;
    y = mTargetY;
    z = mTargetZ;
}