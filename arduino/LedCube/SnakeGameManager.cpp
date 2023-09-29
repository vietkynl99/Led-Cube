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
    init();
}

void SnakeGameManager::init()
{
    mLength = 0;
    mFristIndex = 0;
    mLastIndex = 0;
    mLengthMax = DATA_SIZE_MAX;
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
    mLastIndex = (mLastIndex + 1) % mLengthMax;
    mDataArray[mLastIndex] = value;
    return true;
}

int SnakeGameManager::pop()
{
    int ret = -1;
    if (mLength > 0)
    {
        ret = mDataArray[mFristIndex];
        mLength--;
        mFristIndex = (mFristIndex + 1) % mLengthMax;
    }
    return ret;
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
