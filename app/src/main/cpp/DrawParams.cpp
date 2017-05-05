//
// Created by miroslav on 05.05.17..
//

#include "DrawParams.h"

DrawParams::DrawParams(  int screenWidth, int screenHeight, int strokeWidth, int drawOffset, const NVGcolor &strokeColor )
        :  screenWidth( screenWidth ), screenHeight( screenHeight ),
          strokeWidth( strokeWidth ), drawOffset(drawOffset), strokeColor( strokeColor )
{ }

void DrawParams::set( int screenWidth, int screenHeight, int strokeWidth, int drawOffset, const NVGcolor& strokeColor )
{
    this->screenWidth   = screenWidth;
    this->screenHeight  = screenHeight;
    this->strokeWidth   = strokeWidth;
    this->drawOffset    = drawOffset;
    this->strokeColor   = strokeColor;
}
