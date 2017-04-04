//
// Created by miroslav on 02.04.17..
//

#ifndef SLIMPLAYER_JRECT_H
#define SLIMPLAYER_JRECT_H

#include <jni.h>

class Jrect
{
private:
    int startx;
    int starty;
    int endx;
    int endy;

public:
    Jrect( jint startx, jint starty, jint endx, jint endy );


    int getWitdth();


    int getHeight();
};

#endif //SLIMPLAYER_JRECT_H
