//
// Created by miroslav on 02.04.17..
//

#ifndef SLIMPLAYER_JRECT_H
#define SLIMPLAYER_JRECT_H

#include <jni.h>

/**
 * Rectangle class
 */
class Rect
{
public:
    int startx;
    int starty;
    int endx;
    int endy;

    Rect( int startx, int starty, int endx, int endy );


    int getWitdth();


    int getHeight();
};

#endif //SLIMPLAYER_JRECT_H
