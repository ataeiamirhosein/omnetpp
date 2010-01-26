//==========================================================================
//  RESULTRECORDERS.CC - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2008 Andras Varga
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "cproperties.h"
#include "cproperty.h"
#include "resultrecorders.h"


simtime_t ResultRecorder::warmupEndTime;

void ResultRecorder::listenerAdded(cComponent *component, simsignal_t signalID)
{
    ASSERT(getSubscribeCount() == 1);  // may only be subscribed once (otherwise results get mixed)
}

void ResultRecorder::listenerRemoved(cComponent *component, simsignal_t signalID)
{
    if (getSubscribeCount() == 0)
        delete this;
}

std::string ResultRecorder::makeName(simsignal_t signalID, const char *opname)
{
    const char *signalName = cComponent::getSignalName(signalID);
    if (!signalName)
        signalName = "<unnamed>";
    return std::string(signalName) + "." + opname;
}

void ResultRecorder::extractSignalAttributes(cComponent *component, simsignal_t signalID, opp_string_map& result)
{
    const char *signalName = cComponent::getSignalName(signalID);
    if (signalName)
        extractSignalAttributes(component, signalName, result);
}

void ResultRecorder::extractSignalAttributes(cComponent *component, const char *signalName, opp_string_map& result)
{
    cProperty *property = component->getProperties()->get("signal", signalName);
    if (!property)
        return;

    // fill result[] from the properties
    const std::vector<const char *>& keys = property->getKeys();
    for (int i = 0; i < (int)keys.size(); i++)
    {
        const char *key = keys[i];
        int numValues = property->getNumValues(key);
        if (numValues == 0)
            result[key] = "";
        else if (numValues == 1)
            result[key] = property->getValue(key, 0);
        else {
            std::string buf;
            for (int j = 0; j < numValues; j++) {
                if (j > 0) buf += ",";
                buf += property->getValue(key, j);
            }
            result[key] = buf;
        }
    }
}

//---

void NumericResultRecorder::receiveSignal(cComponent *source, simsignal_t signalID, long l)
{
    simtime_t t = simulation.getSimTime();
    if (t >= getEndWarmupPeriod())
        collect(t, l);
}

void NumericResultRecorder::receiveSignal(cComponent *source, simsignal_t signalID, double d)
{
    simtime_t t = simulation.getSimTime();
    if (t >= getEndWarmupPeriod())
        collect(t, d);
}

void NumericResultRecorder::receiveSignal(cComponent *source, simsignal_t signalID, simtime_t d)
{
    simtime_t t = simulation.getSimTime();
    if (t >= getEndWarmupPeriod())
        collect(t, SIMTIME_DBL(d));
}

void NumericResultRecorder::receiveSignal(cComponent *source, simsignal_t signalID, const char *s)
{
    const char *signalName = cComponent::getSignalName(signalID);
    throw cRuntimeError(
            "%s: unsupported signal data type 'const char *' for signal %s (id=%d)",
            opp_typename(typeid(*this)), signalName, (int)signalID);
}

void NumericResultRecorder::receiveSignal(cComponent *source, simsignal_t signalID, cObject *obj)
{
    TimeValue *d = dynamic_cast<TimeValue *>(obj);
    if (!d) {
        const char *signalName = cComponent::getSignalName(signalID);
        throw cRuntimeError(
                "%s: unsupported signal data type '%s *' for signal %s (id=%d)",
                opp_typename(typeid(*this)), obj->getClassName(), signalName, (int)signalID);
    }
    if (d->time >= getEndWarmupPeriod())
        collect(d->time, d->value);
}

//---

//TODO use arg-less ctor!!! we could call registerOutputVector() in listenerAdded(), or just remember component ptr there, and register vector later when 1st signal gets fired

VectorRecorder::VectorRecorder(cComponent *component, const char *signalName)
{
    opp_string_map attributes;
    extractSignalAttributes(component, signalName, attributes);
    handle = ev.registerOutputVector(component->getFullPath().c_str(), signalName);
    ASSERT(handle!=NULL);
    for (opp_string_map::iterator it = attributes.begin(); it != attributes.end(); ++it)
        ev.setVectorAttribute(handle, it->first.c_str(), it->second.c_str());
}

void VectorRecorder::receiveSignal(cComponent *source, simsignal_t signalID, cObject *obj)
{
    // copied from base class to add monotonicity check
    TimeValue *d = dynamic_cast<TimeValue *>(obj);
    if (!d) {
        const char *signalName = cComponent::getSignalName(signalID);
        throw cRuntimeError(
                "%s: unsupported signal data type '%s *' for signal %s (id=%d)",
                opp_typename(typeid(*this)), obj->getClassName(), signalName, (int)signalID);
    }
    if (d->time < lastTime) {
        const char *signalName = cComponent::getSignalName(signalID);
        throw cRuntimeError(
                "%s: cannot record data with an earlier timestamp (t=%s) than "
                "the previously recorded value, for signal %s (id=%d)",
                opp_typename(typeid(*this)), SIMTIME_STR(d->time), signalName, (int)signalID);
    }
    if (d->time >= getEndWarmupPeriod())
        collect(d->time, d->value);
}

void VectorRecorder::collect(simtime_t t, double value)
{
    lastTime = t;
    ev.recordInOutputVector(handle, t, value);
}

//---

void CountRecorder::receiveSignal(cComponent *source, simsignal_t signalID, const char *s)
{
    simtime_t t = simulation.getSimTime();
    if (t >= getEndWarmupPeriod())
        collect(t, (long)0); // dummy value
}

void CountRecorder::receiveSignal(cComponent *source, simsignal_t signalID, cObject *obj)
{
    // if it is a TimeValue, we should use its time for checking warm-up period
    TimeValue *d = dynamic_cast<TimeValue *>(obj);
    if (!d)
        collect(simulation.getSimTime(), (long)0);  // dummy value
    else {
        if (d->time >= getEndWarmupPeriod())
            collect(d->time, (long)0); // dummy value
    }
}

void CountRecorder::finish(cComponent *component, simsignal_t signalID)
{
    std::string scalarName = makeName(signalID, "count");
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordScalar(component, scalarName.c_str(), count, &attributes);
}

//---

void SumRecorder::finish(cComponent *component, simsignal_t signalID)
{
    std::string scalarName = makeName(signalID, "sum");
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordScalar(component, scalarName.c_str(), sum, &attributes);
}

//---

void MeanRecorder::finish(cComponent *component, simsignal_t signalID)
{
    std::string scalarName = makeName(signalID, "mean");
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordScalar(component, scalarName.c_str(), sum/count, &attributes); // note: this is NaN if count==0
}

//---

void MinRecorder::finish(cComponent *component, simsignal_t signalID)
{
    std::string scalarName = makeName(signalID, "min");
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordScalar(component, scalarName.c_str(), isPositiveInfinity(min) ? NaN : min, &attributes);
}

//---

void MaxRecorder::finish(cComponent *component, simsignal_t signalID)
{
    std::string scalarName = makeName(signalID, "max");
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordScalar(component, scalarName.c_str(), isNegativeInfinity(max) ? NaN : max, &attributes);
}

//---

void TimeAverageRecorder::collect(simtime_t t, double value)
{
    if (startTime==-1)
        startTime = t;
    else
        weightedSum += lastValue * SIMTIME_DBL(t - lastTime);
    lastTime = t;
    lastValue = value;
}

void TimeAverageRecorder::finish(cComponent *component, simsignal_t signalID)
{
    bool empty = (startTime == -1);
    simtime_t t = simulation.getSimTime();
    collect(t, 0.0); // to get the last interval counted in; value 0.0 is just a dummy
    double interval = SIMTIME_DBL(t - startTime);

    std::string scalarName = makeName(signalID, "timeavg");
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordScalar(component, scalarName.c_str(), empty ? NaN : (weightedSum / interval), &attributes);
}

//---

void StatisticsRecorder::finish(cComponent *component, simsignal_t signalID)
{
    const char *signalName = cComponent::getSignalName(signalID);
    opp_string_map attributes;
    extractSignalAttributes(component, signalID, attributes);
    ev.recordStatistic(component, signalName, statistic, &attributes);
}

