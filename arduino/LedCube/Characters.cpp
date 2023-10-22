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

int Characters::getWidth(char character)
{
    return getCode(character, 0);
}

int Characters::getStringWidth(const char *str)
{
    int sum = 0;
    for (int i = 0; i < strlen(str); i++)
    {
        // Add 1 space between characters
        sum += getWidth(str[i]) + 1;
    }
    // Remove space in the end of string
    if (sum > 1)
    {
        sum--;
    }
    return sum;
}