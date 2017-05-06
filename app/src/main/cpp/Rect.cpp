//
// Created by miroslav on 02.04.17..
//

#include "Rect.h"

Rect::Rect( int startx, int starty, int endx, int endy )
{
this->startx    = startx;
this->starty    = starty;
this->endx      = endx;
this->endy      = endy;
}

int Rect::getWitdth()
{
return endx - startx;
}

int Rect::getHeight()
{
return endy - starty;
}
