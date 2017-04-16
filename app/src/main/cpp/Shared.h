//
// Created by miroslav on 12.04.17..
//

#ifndef SLIMPLAYER_SHARED_H
#define SLIMPLAYER_SHARED_H

#include <string>
#include <sstream>


template <class T>
std::string to_string (const T& t)
{
    std::stringstream ss;
    ss << t;
    return ss.str();
}

#endif //SLIMPLAYER_SHARED_H
