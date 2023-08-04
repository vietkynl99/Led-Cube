#include "PixelCoordinate.h"

// arrayPosition: 0 -> NUM_LEDS - 1
// x,y,z: 1->MATRIX_SIZE_1D

int PixelCoordinate::getArrayPosition(int x, int y, int z)
{
    if (x < 1 || y < 1 || z < 1 || x > MATRIX_SIZE_1D || y > MATRIX_SIZE_1D || z > MATRIX_SIZE_1D)
    {
        return -1;
    }
    return -1;
}

bool PixelCoordinate::getDescartesPositions(int arrayPosition, int *x, int *y, int *z)
{
    if (arrayPosition < 0 || arrayPosition >= NUM_LEDS)
    {
        return false;
    }

    int matrixIndex = arrayPosition / MATRIX_SIZE_2D;
    int position = arrayPosition % MATRIX_SIZE_2D;
    if (matrixIndex >= MATRIX_SIDES)
    {
        LOG_LED("Error matrixIndex:%d", matrixIndex);
        return false;
    }

    switch (matrixIndex)
    {
    case 0:
    {
        *x = MATRIX_SIZE_1D - position / MATRIX_SIZE_1D;
        *y = MATRIX_SIZE_1D - position % MATRIX_SIZE_1D;
        *z = 0;
        break;
    }
    case 1:
    {
        *x = 0;
        *y = MATRIX_SIZE_1D - position % MATRIX_SIZE_1D;
        *z = position / MATRIX_SIZE_1D + 1;
        break;
    }
    case 2:
    {
        *x = position % MATRIX_SIZE_1D + 1;
        *y = 0;
        *z = position / MATRIX_SIZE_1D + 1;
        break;
    }
    case 3:
    {
        *x = MATRIX_SIZE_1D + 1;
        *y = position % MATRIX_SIZE_1D + 1;
        *z = position / MATRIX_SIZE_1D + 1;
        break;
    }
    case 4:
    {
        *x = MATRIX_SIZE_1D - position / MATRIX_SIZE_1D;
        *y = position % MATRIX_SIZE_1D + 1;
        *z = MATRIX_SIZE_1D + 1;
        break;
    }
    case 5:
    {
        *x = position % MATRIX_SIZE_1D + 1;
        *y = MATRIX_SIZE_1D + 1;
        *z = MATRIX_SIZE_1D - position / MATRIX_SIZE_1D;
        break;
    }
    default:
        break;
    }
    return true;
}

int PixelCoordinate::getMatrixPosition(int arrayPosition)
{
    return -1;
}

int PixelCoordinate::getMatrixPosition(int x, int y, int z)
{
    return -1;
}