//=========================================================================
//  EVENTLOG.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __EVENTLOG_H_
#define __EVENTLOG_H_

#include <sstream>
#include <set>
#include <map>
#include "Event.h"
#include "EventLogFilter.h"

class EventLog
{
   protected:
      typedef std::map<long, Event> EventNumberToEventMap;
      EventNumberToEventMap eventNumberToEventMap;

      EventLogFilter *filter(Event *tracedEvent, std::set<int> *moduleIds, bool wantCauses, bool wantConsequences, bool wantNonDeliveryMessages);

   public:
      EventLog(const char *fileName);
};

#endif