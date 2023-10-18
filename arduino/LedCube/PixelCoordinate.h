#ifndef _PIXEL_COORDINATE_H_
#define _PIXEL_COORDINATE_H_

#include "LedManager.h"

class PixelCoordinate
{
public:
    static int getArrayPosition(int x, int y, int z);
    static bool getDescartesPositions(int arrayPosition, int& x, int& y, int& z);
    static int getMatrixPosition(int arrayPosition);
    static int getMatrixPosition(int x, int y, int z);
    static bool arePointsCoplanar(int arrayPosition1, int arrayPosition2);
    static bool arePointsCoplanar(int x1, int y1, int z1, int x2, int y2, int z2);
};

#endif