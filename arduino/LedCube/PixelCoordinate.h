#ifndef _PIXEL_COORDINATE_H_
#define _PIXEL_COORDINATE_H_

#include "LedManager.h"

class PixelCoordinate
{
public:
    static int getArrayPosition(int x, int y, int z);
    static bool getDescartesPositions(int arrayPosition, int *x, int *y, int *z);
    static int getMatrixPosition(int arrayPosition);
    static int getMatrixPosition(int x, int y, int z);
};

#endif