//
// This file is part of an OMNeT++/OMNEST simulation example.
//
// Copyright (C) 2006-2015 OpenSim Ltd.
//
// This file is distributed WITHOUT ANY WARRANTY. See the file
// `license' for details on this and other legal matters.
//

#ifndef __SINKEXT_H__
#define __SINKEXT_H__

#include <omnetpp.h>
#include "Sink.h"

USING_NAMESPACE

/**
 * Sink module that displays the number of received jobs.
 */
class SinkExt : public queueing::Sink
{
  protected:
    int numJobsReceived;

    virtual void initialize() override;
    virtual void handleMessage(cMessage *msg) override;
};

#endif
