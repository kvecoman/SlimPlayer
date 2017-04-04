//
// Created by miroslav on 02.04.17..
//

#include "Jrect.h"

Jrect::Jrect( jint startx, jint starty, jint endx, jint endy )
{
this->startx = startx;
this->starty = starty;
this->endx = endx;
this->endy = endy;
}

int Jrect::getWitdth()
{
return endx - startx;
}

int Jrect::getHeight()
{
return endy - starty;
}
