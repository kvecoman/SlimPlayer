//
// Created by miroslav on 05.05.17..
//

#ifndef SLIMPLAYER_DRAWWAVEFORMEXTENSION_H
#define SLIMPLAYER_DRAWWAVEFORMEXTENSION_H



#include "Shared.h"
#include "AudioBufferManager.h"
#include "DrawParams.h"
#include "Point.h"

/**
 * Base class used to calculate and draw waveform of samples passed in
 */
class DrawWaveformExtension
{
private:
    Point * mWaveformPoints = nullptr;

public:

    virtual ~DrawWaveformExtension();

    void initWaveformPoints( int targetSamplesCount );

    void drawWaveform( NVGcontext * nvgContext, Buffer * samplesBuffer, DrawParams * drawParams );

    void calculateWaveformPoints( Buffer * buffer, DrawParams * drawParams );


};


#endif //SLIMPLAYER_DRAWWAVEFORMEXTENSION_H
