//
// Created by miroslav on 05.05.17..
//

#ifndef SLIMPLAYER_DRAWPARAMS_H
#define SLIMPLAYER_DRAWPARAMS_H


//#include "nanovg/nanovg.h"
#include "Shared.h"

/**
 * Class used to pass around basic draw parameters like screen size and stroke width/color
 */
class DrawParams
{
public:
    int         screenWidth = -1;
    int         screenHeight = -1;
    int         strokeWidth = -1;
    int         drawOffset = -1;
    NVGcolor    strokeColor;



    DrawParams(  int screenWidth, int screenHeight, int drawOffset, int strokeWidth, const NVGcolor &strokeColor );

    void set( int screenWidth, int screenHeight, int strokeWidth, int drawOffset, const NVGcolor& strokeColor );
};


#endif //SLIMPLAYER_DRAWPARAMS_H
