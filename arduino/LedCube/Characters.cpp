#include "Characters.h"

unsigned char Characters::getCode(char character, int column)
{
    int row = character - CHARACTERS_OFFSET;
    if (column >= 0 && column < CHARACTERS_COL_MAX && row >= 0 && row < CHARACTERS_ROW_MAX)
    {
        return pgm_read_byte(&charactersMatrix[row][column]);
    }
    return 0;
}

unsigned char Characters::getWidth(char character)
{
    return getCode(character, 0);
}
