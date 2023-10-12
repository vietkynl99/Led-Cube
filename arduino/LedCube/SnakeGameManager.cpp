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
    mGameMode = GAME_MODE_FULL_SIDES;
    resetGame();
}

void SnakeGameManager::resetGame()
{
    mLength = 0;
    mFristIndex = 0;
    mLastIndex = 0;
    mLengthMax = DATA_SIZE_MAX;
    mDir = DIR_MODE_NONE;
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

void SnakeGameManager::generateFirstTargetPosition()
{
    while (1)
    {
        int targetPosition = generateRandomPosition();
        if (targetPosition >= 0)
        {
            getRawPosition(targetPosition, mTargetX, mTargetY, mTargetZ);
            if (PixelCoordinate::getMatrixPosition(mX, mY, mZ) == PixelCoordinate::getMatrixPosition(mTargetX, mTargetY, mTargetZ) &&
                pow(mX - mTargetX, 2) + pow(mY - mTargetY, 2) + pow(mZ - mTargetZ, 2) > 3)
            {
                break;
            }
        }
    }
}

int SnakeGameManager::generateRandomPosition()
{
    if (isFull())
    {
        return -1;
    }
    for (int i = 0; i < mLengthMax * 100; i++)
    {
        int value = random(0, mLengthMax);
        if (!isExists(value))
        {
            return value;
        }
    }
    return -1;
}

int SnakeGameManager::getPanelPosition(int x, int y, int z)
{
    return PixelCoordinate::getArrayPosition(x, y, z);
}

void SnakeGameManager::getRawPosition(int panelPosition, int &x, int &y, int &z)
{
    PixelCoordinate::getDescartesPositions(panelPosition, x, y, z);
}

void SnakeGameManager::startGame()
{
    resetGame();
    setLengthMax(NUM_LEDS);
    setDir(DIR_MODE_NONE);

    // start position
    int panelPosition = getPanelPosition(mX, mY, mZ);
    SnakeGameManager::getInstance()->add(panelPosition);

    generateFirstTargetPosition();
}

void SnakeGameManager::setDirAxis(int dirX, int dirY, int dirZ)
{
    mDirX = dirX;
    mDirY = dirY;
    mDirZ = dirZ;
}

void SnakeGameManager::setDir(int dir, bool force)
{
    if ((dir == mDir) ||
        (dir < DIR_MODE_NONE) ||
        (dir >= DIR_MODE_MAX))
    {
        return;
    }
    if (((mX == 0 || mX == MATRIX_SIZE_1D + 1) && (dir == DIR_MODE_AXIS1_DEC || dir == DIR_MODE_AXIS1_INC)) ||
        ((mY == 0 || mY == MATRIX_SIZE_1D + 1) && (dir == DIR_MODE_AXIS2_DEC || dir == DIR_MODE_AXIS2_INC)) ||
        ((mZ == 0 || mZ == MATRIX_SIZE_1D + 1) && (dir == DIR_MODE_AXIS3_DEC || dir == DIR_MODE_AXIS3_INC)))
    {
        return;
    }
    if (!force)
    {
        int minDir = min(dir, mDir);
        int maxDir = max(dir, mDir);
        if ((minDir == DIR_MODE_AXIS1_DEC && maxDir == DIR_MODE_AXIS1_INC) ||
            (minDir == DIR_MODE_AXIS2_DEC && maxDir == DIR_MODE_AXIS2_INC) ||
            (minDir == DIR_MODE_AXIS3_DEC && maxDir == DIR_MODE_AXIS3_INC))
        {
            return;
        }
    }
    mDir = dir;

    switch (mDir)
    {
    case DIR_MODE_AXIS1_INC:
        setDirAxis(1, 0, 0);
        break;
    case DIR_MODE_AXIS1_DEC:
        setDirAxis(-1, 0, 0);
        break;
    case DIR_MODE_AXIS2_INC:
        setDirAxis(0, 1, 0);
        break;
    case DIR_MODE_AXIS2_DEC:
        setDirAxis(0, -1, 0);
        break;
    case DIR_MODE_AXIS3_INC:
        setDirAxis(0, 0, 1);
        break;
    case DIR_MODE_AXIS3_DEC:
        setDirAxis(0, 0, -1);
        break;
    default:
        setDirAxis(0, 0, 0);
        break;
    }
}

void SnakeGameManager::detectCurrentDir()
{
    if (abs(mDirX + mDirY + mDirZ) != 1)
    {
        setDir(DIR_MODE_NONE, true);
        return;
    }
    if (mDirX == -1)
    {
        setDir(DIR_MODE_AXIS1_DEC, true);
    }
    else if (mDirX == 1)
    {
        setDir(DIR_MODE_AXIS1_INC, true);
    }
    else if (mDirY == -1)
    {
        setDir(DIR_MODE_AXIS2_DEC, true);
    }
    else if (mDirY == 1)
    {
        setDir(DIR_MODE_AXIS2_INC, true);
    }
    else if (mDirZ == -1)
    {
        setDir(DIR_MODE_AXIS3_DEC, true);
    }
    else if (mDirZ == 1)
    {
        setDir(DIR_MODE_AXIS3_INC, true);
    }
    else
    {
        setDir(DIR_MODE_NONE, true);
    }
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
        int gyroZ = HardwareController::getInstance()->getGyroZ();
        // LOG_GAME("gyroX: %d\tgyroY: %d\tgyroZ: %d", gyroX, gyroY, gyroZ);
        preDir = newDir;
        if (gyroX > GYRO_THRESHOLD)
        {
            LOG_GAME("gyroX +");
            newDir = DIR_MODE_AXIS1_INC;
        }
        else if (gyroX < -GYRO_THRESHOLD)
        {
            LOG_GAME("gyroX -");
            newDir = DIR_MODE_AXIS1_DEC;
        }
        else if (gyroY > GYRO_THRESHOLD)
        {
            LOG_GAME("gyroY +");
            newDir = DIR_MODE_AXIS2_INC;
        }
        else if (gyroY < -GYRO_THRESHOLD)
        {
            LOG_GAME("gyroY -");
            newDir = DIR_MODE_AXIS2_DEC;
        }
        else if (gyroZ > GYRO_THRESHOLD)
        {
            LOG_GAME("gyroZ +");
            newDir = DIR_MODE_AXIS3_INC;
        }
        else if (gyroZ < -GYRO_THRESHOLD)
        {
            LOG_GAME("gyroZ -");
            newDir = DIR_MODE_AXIS3_DEC;
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

bool SnakeGameManager::caculateNextDir(int &axis1, int &axis2, int &axist3, int &dir1, int &dir2, int &dir3)
{
    bool isChanged = false;
    if (dir1)
    {
        if (axis1 > MATRIX_SIZE_1D)
        {
            axis1 = MATRIX_SIZE_1D + 1;
            isChanged = true;
        }
        else if (axis1 < 1)
        {
            axis1 = 0;
            isChanged = true;
        }

        if (isChanged)
        {
            dir1 = 0;
            if (axis2 == 0)
            {
                axis2 = 1;
                dir2 = 1;
                dir3 = 0;
            }
            else if (axis2 == MATRIX_SIZE_1D + 1)
            {
                axis2 = MATRIX_SIZE_1D;
                dir2 = -1;
                dir3 = 0;
            }
            else if (axist3 == 0)
            {
                axist3 = 1;
                dir2 = 0;
                dir3 = 1;
            }
            else if (axist3 == MATRIX_SIZE_1D + 1)
            {
                axist3 = MATRIX_SIZE_1D;
                dir2 = 0;
                dir3 = -1;
            }
            else
            {
                LOG_GAME("Invalid position: %d %d %d - %d %d %d", mX, mY, mZ, mDirX, mDirY, mDirZ);
            }
            detectCurrentDir();
        }
    }
    return isChanged;
}

int SnakeGameManager::nextMove(int &setX, int &setY, int &setZ, int &clearX, int &clearY, int &clearZ)
{
    if (mDir == DIR_MODE_NONE)
    {
        return NEXT_MOVE_CODE_NONE;
    }

    if (mDirX + mDirY + mDirZ == 0)
    {
        return NEXT_MOVE_CODE_NONE;
    }

    mX += mDirX;
    mY += mDirY;
    mZ += mDirZ;

    int isChanged = false;
    if (!isChanged)
    {
        isChanged = caculateNextDir(mX, mY, mZ, mDirX, mDirY, mDirZ);
    }
    if (!isChanged)
    {
        isChanged = caculateNextDir(mY, mZ, mX, mDirY, mDirZ, mDirX);
    }
    if (!isChanged)
    {
        isChanged = caculateNextDir(mZ, mX, mY, mDirZ, mDirX, mDirY);
    }

    // LOG_GAME("Current position: %d %d %d - %d %d %d", mX, mY, mZ, mDirX, mDirY, mDirZ);

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
            int targetPosition = generateRandomPosition();
            if (targetPosition < 0)
            {
                LOG_GAME("Win game!");
                return NEXT_MOVE_CODE_WIN_GAME;
            }
            else
            {
                // generate new target position
                getRawPosition(targetPosition, mTargetX, mTargetY, mTargetZ);
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
    // if (command == COMMAND_GAME_RIGHT)
    // {
    //     setDir(DIR_MODE_RIGHT);
    // }
    // else if (command == COMMAND_GAME_UP)
    // {
    //     setDir(DIR_MODE_UP);
    // }
    // else if (command == COMMAND_GAME_LEFT)
    // {
    //     setDir(DIR_MODE_LEFT);
    // }
    // else if (command == COMMAND_GAME_DOWN)
    // {
    //     setDir(DIR_MODE_DOWN);
    // }
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