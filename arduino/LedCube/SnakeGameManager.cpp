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
    mDir = DIR_MODE_NONE;
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

// Data is stored in: mFristIndex+1 -> mLastIndex
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
    if (mLength <= 0)
    {
        return false;
    }
    int realIndex = (mFristIndex + 1) % DATA_SIZE_MAX;
    if (realIndex <= mLastIndex)
    {
        for (int i = realIndex; i <= mLastIndex; i++)
        {
            if (mDataArray[i] == value)
            {
                return true;
            }
        }
    }
    else
    {
        for (int i = 0; i <= realIndex; i++)
        {
            if (mDataArray[i] == value)
            {
                return true;
            }
        }
        for (int i = mLastIndex; i <= DATA_SIZE_MAX; i++)
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
    return (x - 1) * MATRIX_SIZE_1D + y - 1;
}

void SnakeGameManager::getRawPosition(int panelPosition, int &x, int &y, int &z)
{
    x = panelPosition / MATRIX_SIZE_1D + 1;
    y = panelPosition % MATRIX_SIZE_1D + 1;
}

void SnakeGameManager::startGame()
{
    resetGame();
    setLengthMax(MATRIX_SIZE_2D);
    setDir(DIR_MODE_NONE);

    // start position
    int panelPosition = getPanelPosition(mX, mY, mZ);
    SnakeGameManager::getInstance()->add(panelPosition);

    // Generate frist target position
    mTargetX = mX;
    mTargetY = mY;
    mTargetZ = mZ;
    while (1)
    {
        int targetPanelPosition = generateRandomUnvailableValue(0, MATRIX_SIZE_2D);
        if (targetPanelPosition >= 0)
        {
            getRawPosition(targetPanelPosition, mTargetX, mTargetY, mTargetZ);
            if (pow(mX - mTargetX, 2) + pow(mY - mTargetY, 2) + pow(mZ - mTargetZ, 2) > 3)
            {
                break;
            }
        }
    }
}

void SnakeGameManager::setDir(int dir)
{
    if ((mDir == DIR_MODE_RIGHT && dir == DIR_MODE_LEFT) ||
        (mDir == DIR_MODE_LEFT && dir == DIR_MODE_RIGHT) ||
        (mDir == DIR_MODE_UP && dir == DIR_MODE_DOWN) ||
        (mDir == DIR_MODE_DOWN && dir == DIR_MODE_UP))
    {
        return;
    }
    mDir = dir;
}

#ifdef ENABLE_MPU6050_SENSOR
void SnakeGameManager::handleDirByMpu()
{
    static unsigned long long time = 0;
    static int preDir = DIR_MODE_NONE, newDir = DIR_MODE_NONE;

    if (millis() > time)
    {
        time = millis() + 10UL;
        int gyroX = HardwareController::getInstance()->getGyroX();
        int gyroY = HardwareController::getInstance()->getGyroY();
        bool isAngleRight = gyroX > GYRO_THRESHOLD;
        bool isAngleLeft = gyroX < -GYRO_THRESHOLD;
        bool isAngleUp = gyroY < -GYRO_THRESHOLD;
        bool isAngleDown = gyroY > GYRO_THRESHOLD;
        // LOG_GAME("gyroX:%d, gyroY:%d -> right:%d, left:%d, up:%d, down:%d", gyroX, gyroY, isAngleRight, isAngleLeft, isAngleUp, isAngleDown);
        preDir = newDir;
        if (isAngleRight)
        {
            newDir = DIR_MODE_RIGHT;
        }
        else if (isAngleLeft)
        {
            newDir = DIR_MODE_LEFT;
        }
        else if (isAngleUp)
        {
            newDir = DIR_MODE_UP;
        }
        else if (isAngleDown)
        {
            newDir = DIR_MODE_DOWN;
        }
        else
        {
            newDir = DIR_MODE_NONE;
        }
        if (preDir == DIR_MODE_NONE && newDir != preDir)
        {
            setDir(newDir);
        }
    }
}
#endif

int SnakeGameManager::nextMove(int &setX, int &setY, int &setZ, int &clearX, int &clearY, int &clearZ)
{
    switch (mDir)
    {
    case DIR_MODE_RIGHT:
        mY++;
        break;
    case DIR_MODE_LEFT:
        mY--;
        break;
    case DIR_MODE_UP:
        mX++;
        break;
    case DIR_MODE_DOWN:
        mX--;
        break;
    default:
        return NEXT_MOVE_CODE_NONE;
    }

    if (mX > MATRIX_SIZE_1D)
    {
        mX = 1;
    }
    if (mX < 1)
    {
        mX = MATRIX_SIZE_1D;
    }

    if (mY > MATRIX_SIZE_1D)
    {
        mY = 1;
    }
    if (mY < 1)
    {
        mY = MATRIX_SIZE_1D;
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
                getRawPosition(targetPanelPosition, mTargetX, mTargetY, mTargetZ);
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
            clearY = mY;
            clearZ = mZ;
            getRawPosition(firstPanelPosition, clearX, clearY, clearZ);
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
        setDir(DIR_MODE_RIGHT);
    }
    else if (command == COMMAND_GAME_UP)
    {
        setDir(DIR_MODE_UP);
    }
    else if (command == COMMAND_GAME_LEFT)
    {
        setDir(DIR_MODE_LEFT);
    }
    else if (command == COMMAND_GAME_DOWN)
    {
        setDir(DIR_MODE_DOWN);
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