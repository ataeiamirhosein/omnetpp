#=================================================================
#  MENUPROC.TCL - part of
#
#                     OMNeT++/OMNEST
#            Discrete System Simulation in C++
#
#=================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2008 Andras Varga
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#

#------
# Parts of this file were created using Stewart Allen's Visual Tcl (vtcl)
#------

#===================================================================
#    HELPER/GUI PROCEDURES
#===================================================================

proc networkPresent {} {
    if [opp_isnull [opp_object_systemmodule]] {
       messagebox {Error} {No network has been set up yet.} info ok
       return 0
    }
    return 1
}

proc networkReady {} {
    if {[networkPresent] == 0} {return 0}

    if {[isSimulationOk] == 1} {
        return 1
    } else {
        set ans [messagebox {Warning} {Cannot continue this simulation. Rebuild network?} question yesno]
        if {$ans == "yes"} {
            rebuild
            return [isSimulationOk]
        } else {
            return 0
        }
    }
}

proc isSimulationOk {} {
    set state [opp_getsimulationstate]
    if {$state == "SIM_NEW" || $state == "SIM_RUNNING" || $state == "SIM_READY"} {
        return 1
    } else {
        return 0
    }
}

proc isRunning {} {
    set state [opp_getsimulationstate]
    if {$state == "SIM_RUNNING" || $state == "SIM_BUSY"} {
        return 1
    } else {
        return 0
    }
}

proc checkRunning {} {
    if {[opp_getsimulationstate] == "SIM_RUNNING"} {
       messagebox {Warning} {Sorry, you cannot do this while the simulation\
                             is running. Please stop it first.} info ok
       return 1
    }
    if {[opp_getsimulationstate] == "SIM_BUSY"} {
       messagebox {Warning} {The simulation is waiting for external synchronization -- press STOP to interrupt it.} info ok
       return 1
    }
    return 0
}

#===================================================================
#    MENU PROCEDURES
#===================================================================

proc about {} {
    # implements Help|About
    global OMNETPP_RELEASE OMNETPP_EDITION OMNETPP_BUILDID
    aboutDialog "About OMNeT++/OMNEST" "
This simulation model is based on:

OMNeT++/OMNEST
Discrete Event Simulation Framework
" \
"(C) 1992-2008 Andras Varga
Release: $OMNETPP_RELEASE, build: $OMNETPP_BUILDID, edition: $OMNETPP_EDITION

NO WARRANTY -- see license for details."
}

proc exitOmnetpp {} {
    global config

    set isrunning [isRunning]
    set state [opp_getsimulationstate]

    if {$config(confirm-exit)} {
        if [opp_isnotnull [opp_object_systemmodule]] {
            if {$isrunning} {
                set ans [messagebox {Warning} {The simulation is currently running. Do you really want to quit?} warning yesno]
                if {$ans == "no"} {
                    return
                }
            } elseif {$state == "SIM_READY"} {
                set ans [messagebox {Warning} {Do you want to call finish() before exiting?} warning yesnocancel]
                if {$ans == "yes"} {
                    # no catch{}: exceptions are handled inside
                    opp_finish_simulation
                } elseif {$ans == "no"} {
                    # nothing to do
                } elseif {$ans == "cancel"} {
                    return
                }
            } else {
                #set ans [messagebox {Warning} {Do you really want to quit?} warning yesno]
            }
        }
    }

    if {$isrunning} {
       opp_stopsimulation
    }

    # save settings (fonts etc) globally, and inspector list locally
    saveTkenvrc "~/.tkenvrc" 1 0 1
    saveTkenvrc ".tkenvrc"   0 1 1 "# Partial Tkenv config file -- see \$HOME/.tkenvrc as well"

    opp_exitomnetpp
}

proc createSnapshot {} {
    # implements File|Create snapshot

    if {[networkPresent] == 0} return

    set label ""
    set ok [inputbox {Snapshot} {Enter label for current simulation snapshot:} label]
    if {$ok == 1} {
        if [catch {opp_createsnapshot $label} err] {
          messagebox {Error} "Error: $err" error ok
          return
        }
        set snapshotfile [opp_getfilename snapshot]
        if {$snapshotfile==""} {
            messagebox {Snapshot created} "Current state of simulation has been saved." info ok
        } else {
            set ans [messagebox {Snapshot created} "Current state of simulation has been \
saved into \"$snapshotfile\". Do you want to open it now in a file viewer window?" question yesno]
            if {$ans == "yes"} {
                viewSnapshotFile
            }
        }
    }
}

proc loadNedFile {} {
   global config

   set fname $config(last-nedfile)

   if {[string compare [file tail $fname] $fname]==0} {
       set dir "."
   } else {
       set dir [file dirname $fname]
   }

   set fname [file tail $fname]
   set fname [tk_getOpenFile -defaultextension ".ned" \
              -initialdir $dir -initialfile $fname \
              -filetypes {{{NED files} {*.ned}} {{All files} {*}}}]

   if {$fname!=""} {
      set config(last-nedfile) $fname
      if [catch {opp_loadnedfile $fname} err] {
        opp_refreshinspectors
        messagebox {Error} "Error: $err" error ok
        return
      }
      opp_refreshinspectors
   }
}

proc newNetwork {} {
    # implements File|New network...

    if [checkRunning] return

    # get list of network names
    set networks [opp_getnetworktypes]
    set localpackage [opp_getsimoption localpackage]
    set networknames {}
    set localnetworknames {}
    foreach net $networks {
        set networkname [opp_getobjectname $net]
        set networkqname [opp_getobjectfullname $net]
        if {"$localpackage.$networkname" == "$networkqname"} {
            lappend localnetworknames $networkname
        } else {
            lappend networknames $networkqname
        }
    }
    set localnetworknames [lsort -dictionary $localnetworknames]
    set networknames [lsort -dictionary $networknames]

    set networknames [concat $localnetworknames $networknames]

    # pop up dialog, with current network as default
    set netname [opp_getnetworktype]
    set ok [comboSelectionDialog "Set up network" "Set up a network. NOTE: The network will use parameter values defined in the \n\[General\] section of the ini file." "Select network:" netname $networknames]
    if {$ok == 1} {
       busy "Setting up network..."
       inspectorList:addAll 1
       opp_newnetwork $netname
       reflectRecordEventlog
       busy

       if [opp_isnotnull [opp_object_systemmodule]] {
           # tell plugins about it
           busy "Notifying Tcl plugins..."
           notifyPlugins newNetwork
           busy
       }
    }
}

proc newRun {} {
    # implements File|New run...

    if [checkRunning] return

    set configname [opp_getactiveconfigname]
    set runnumber  [opp_getactiverunnumber]

    # pop up selection dialog
    set ok [runSelectionDialog configname runnumber]
    if {$ok == 1} {
       # puts "DBG: selected $configname $runnumber"
       busy "Setting up network..."
       inspectorList:addAll 1
       opp_newrun $configname $runnumber
       reflectRecordEventlog
       busy

       if [opp_isnotnull [opp_object_systemmodule]] {
           # tell plugins about it
           busy "Notifying Tcl plugins..."
           notifyPlugins newNetwork
           busy
       }
    }
}

proc editCopy {{w .log.main.text}} {
   # implements Edit|Copy
   tk_textCopy $w
}

proc editFind {{w .log.main.text}} {
   # implements Edit|Find...
   findDialog $w
}

proc editFindNext {{w .log.main.text}} {
   # implements Edit|Find next
   findNext $w
}

proc editFilterWindowContents {{insp .log}} {
   LogInspector:openFilterDialog $insp
}

proc toggleTimeline {} {
   global config widgets

   if {$config(display-timeline)==1} {
       set config(display-timeline) 0
       pack forget .timelineframe
       .toolbar.tline config -relief flat
   } else {
       set config(display-timeline) 1
       pack .timelineframe -before .main -anchor center -expand 0 -fill x -side top -padx 0 -pady 0 -ipadx 0 -ipady 0
       .toolbar.tline config -relief sunken
       redrawTimeline
   }
}

proc toggleRecordEventlog {} {
   if {[opp_getsimoption record_eventlog]==1} {
       opp_setsimoption record_eventlog 0
   } else {
       opp_setsimoption record_eventlog 1
   }
   if {[opp_eventlogrecording hasintervals]} {
       messagebox {Eventlog Recording} "Switching to manual control of eventlog recording -- the recording intervals specified in the configuration will be ignored." info ok
       opp_eventlogrecording clearintervals
   }
   reflectRecordEventlog
}

proc reflectRecordEventlog {} {
   global config widgets

   if {[opp_getsimoption record_eventlog]==1} {
       .toolbar.eventlog config -relief sunken
   } else {
       .toolbar.eventlog config -relief flat
   }
}

proc setGuiForRunmode {mode {modinspwin ""} {untilmode ""}} {  #FIXME needs to be revised
    global opp
    set insp $modinspwin
    if {$insp!="" && ![winfo exists $insp]} {set insp ""}

    set default_iconbutton_relief "flat"
    .toolbar.step config -relief $default_iconbutton_relief
    .toolbar.run config -relief $default_iconbutton_relief
    .toolbar.fastrun config -relief $default_iconbutton_relief
    .toolbar.exprrun config -relief $default_iconbutton_relief
    catch {$opp(sunken-run-button) config -relief $default_iconbutton_relief}
    removeStopDialog

    if {$insp==""} {
        if {$mode=="step"} {
            .toolbar.step config -relief sunken
        } elseif {$mode=="slow"} {
            .toolbar.run config -relief sunken
        } elseif {$mode=="normal"} {
            .toolbar.run config -relief sunken
        } elseif {$mode=="fast"} {
            .toolbar.fastrun config -relief sunken
        } elseif {$mode=="express"} {
            .toolbar.exprrun config -relief sunken
            displayStopDialog
        } elseif {$mode=="notrunning"} {
            .toolbar.until config -relief $default_iconbutton_relief
        } else {
            error "wrong mode parameter $mode"
        }
    } else {
        if {$mode=="normal"} {
            $insp.toolbar.mrun config -relief sunken
            set opp(sunken-run-button) $insp.toolbar.mrun
        } elseif {$mode=="fast"} {
            $insp.toolbar.mfast config -relief sunken
            set opp(sunken-run-button) $insp.toolbar.mfast
        } elseif {$mode=="express"} {
            displayStopDialog
        } elseif {$mode=="notrunning"} {
            .toolbar.until config -relief $default_iconbutton_relief
        } else {
            error "wrong mode parameter $mode with module inspector"
        }
    }

    if {$untilmode=="until_on"} {
        .toolbar.until config -relief sunken
    } elseif {$untilmode=="until_off"} {
        .toolbar.until config -relief $default_iconbutton_relief
    } elseif {$untilmode!=""} {
        error "wrong untilmode parameter $mode"
    }
}

proc oneStep {} {
    # implements Simulate|One step

    if [isRunning] {
        setGuiForRunmode step
        opp_stopsimulation
    } else {
        if {![networkReady]} {return}
        setGuiForRunmode step
        opp_onestep
        setGuiForRunmode notrunning
    }
}

proc debugNextEvent {} {
    # implements Simulate|Debug next event

    if [isRunning] {
        messagebox {Error} {Simulation is currently running -- please stop it first.} info ok
     } else {
        if {![networkReady]} {return}
        set ans [messagebox {Confirm} {Trigger debugger breakpoint for the next simulation event?\
           Note: If you are not running under a debugger, this will likely result in a crash.} warning okcancel]
        if {$ans == "ok"} {
           setGuiForRunmode step
           opp_request_trap_on_next_event
           opp_onestep
           setGuiForRunmode notrunning
        }
    }
}

proc runSimulation {mode} {
    if [isRunning] {
        setGuiForRunmode $mode
        opp_set_run_mode $mode
        opp_set_run_until_module
    } else {
        if {![networkReady]} {return}
        setGuiForRunmode $mode
        opp_run $mode
        setGuiForRunmode notrunning
    }
}

proc runSlow {} {
    # implements Simulate|Slow execution
    runSimulation slow
}

proc runNormal {} {
    # implements Simulate|Run
    runSimulation normal
}

proc runFast {} {
    # implements Simulate|Fast Run
    runSimulation fast
}

proc runExpress {} {
    # implements Simulate|Express Run
    runSimulation express
}

proc runUntil {} {
    # implements Simulate|Run until...

    if {[networkReady] == 0} {
       return
    }

    set time ""
    set event ""
    set msg ""
    set mode ""    ;# will be set to Normal, Fast or Express

    set ok [runUntilDialog time event msg mode]
    if {$ok==0} return

    if {$mode=="Normal"} {
        set mode "normal"
    } elseif {$mode=="Fast"} {
        set mode "fast"
    } elseif {$mode=="Express"} {
        set mode "express"
    } else {
        set mode "normal"
    }

    set untilmode "until_on"
    if {$time=="" && $event=="" && $msg==""} {set until_on "until_off"}

    if [isRunning] {
        if [catch {
            setGuiForRunmode $mode "" $untilmode
            opp_set_run_mode $mode
            opp_set_run_until $time $event $msg
        } err] {
            messagebox {Error} "Error: $err" error ok
        }
    } else {
        if {![networkReady]} {return}
        if [catch {
            setGuiForRunmode $mode "" $untilmode
            opp_run $mode $time $event $msg
        } err] {
            messagebox {Error} "Error: $err" error ok
        }
        setGuiForRunmode notrunning
    }
}

proc runUntilMsg {msg mode} {
    if {[networkReady] == 0} {
       return
    }
    # mode must be "normal", "fast" or "express"
    if [isRunning] {
        if [catch {
            setGuiForRunmode $mode "" "until_on"
            opp_set_run_mode $mode
            opp_set_run_until "" "" $msg
        } err] {
            messagebox {Error} "Error: $err" error ok
        }
    } else {
        if [catch {
            setGuiForRunmode $mode "" "until_on"
            opp_run $mode "" "" $msg
        } err] {
            messagebox {Error} "Error: $err" error ok
        }
        setGuiForRunmode notrunning
    }
}

proc excludeMessageFromAnimation {msg} {
    set name [opp_getobjectfullname $msg]
    set class [opp_getobjectshorttypename $msg]
    set namepattern [regsub -all -- {[0-9]+} $name {*}]
    set namepattern [regsub -all -- {[^[:print:]]} $namepattern {?}]  ;# sanitize: replace nonprintable chars with '?'
    set namepattern [regsub -all -- {["\\]} $namepattern {?}] ;# sanitize: replace quotes (") and backslashes with '?'
    if {[regexp " " $namepattern]} {   # must be quoted if contains spaces
        set namepattern "\"$namepattern\""
    }

    set filters [string trim [opp_getsimoption silent_event_filters]]
    if {$filters != ""} {append filters "\n"}
    append filters "$namepattern and className($class)\n"
    opp_setsimoption silent_event_filters $filters

    redrawTimeline
    opp_refreshinspectors
}

proc startAll {} {

    if [checkRunning] return

    if {[networkPresent] == 0} return
    opp_start_all
}

proc callFinish {} {

    # check state is not SIM_RUNNING
    if [checkRunning] return

    # check state is not SIM_NONET
    if {[networkPresent] == 0} return

    # check state is not SIM_FINISHCALLED
    if {[opp_getsimulationstate] == "SIM_FINISHCALLED"} {
       messagebox {Error} {finish() has been invoked already.} info ok
       return
    }

    # check state is not SIM_ERROR
    if {[opp_getsimulationstate] == "SIM_ERROR"} {
       set ans [messagebox {Warning} \
                  {Simulation was stopped with error, calling finish() might produce unexpected results. Proceed anyway?} \
                  question yesno]
       if {$ans == "no"} {
           return
       }
    } else {
       set ans [messagebox {Question} \
                  {Do you want to conclude this simulation run and invoke finish() on all modules?} \
                  question yesno]
       if {$ans == "no"} {
           return
       }
    }

    busy "Invoking finish() on all modules..."
    opp_finish_simulation
    busy
}

proc rebuild {} {
    # implements Simulate|Rebuild

    if [checkRunning] return

    if {[networkPresent] == 0} return
    busy "Rebuilding network..."
    inspectorList:addAll 1
    opp_rebuild
    reflectRecordEventlog
    busy
}


proc stopSimulation {} {
    # implements Simulate|Stop
    if {[opp_getsimulationstate] == "SIM_RUNNING" || [opp_getsimulationstate] == "SIM_BUSY"} {
       # "opp_stopsimulation" just *asks* the simulation to stop, causing it to return
       # from the "opp_run" command.
       # "setGuiForRunmode notrunning" will be called after "opp_run" has returned.
       opp_stopsimulation
    }

    # this proc doubles as "stop layouting", when in graphical module inspectors
    global stoplayouting
    set stoplayouting 1
}

proc inspectFilteredObjectList {{insp ".network"}} {
    # implements Find/inspect objects...
    set ptr [opp_inspector_getobject $insp]
    filteredObjectList:window $ptr
}

proc inspectBypointer {} {
    # implements Inspect|By pointer...
    set pointer "ptr"
    set ok [inputbox {Inspect by pointer...} {Enter pointer (invalid pointer may cause segmentation fault!):} pointer]
    if {$ok == 1} {
        opp_inspect $pointer (default)
    }
}

proc inspectSystemModule {} {
    # implements Inspect|Toplevel modules...
    if {[networkPresent] == 0} return
    opp_inspect [opp_object_systemmodule] (default)
}

proc inspectMessageQueue {} {
    # implements Inspect|Message queue...
    opp_inspect [opp_object_messagequeue] (default)
}

proc inspectSimulation {} {
    # implements Inspect|Simulation...
    opp_inspect [opp_object_simulation] (default)
}

proc inspectComponentTypes {} {
    opp_inspect [opp_object_componenttypes] {(default)}
}

proc inspectClasses {} {
    opp_inspect [opp_object_classes] {(default)}
}

proc inspectEnums {} {
    opp_inspect [opp_object_enums] {(default)}
}

proc inspectConfigEntries {} {
    opp_inspect [opp_object_configentries] {(default)}
}

proc inspectFunctions {} {
    opp_inspect [opp_object_functions] {(default)}
}

proc simulationOptions {} {
    optionsDialog .
    opp_refreshinspectors
}

proc saveTkenvConfig {} {
    set filename "tkenv.cfg"
    set filename [tk_getSaveFile -title "Save Tkenv configuration" \
                  -defaultextension "cfg" -initialfile $filename \
                  -filetypes {{{Configuration files} {*.cfg}} {{All files} {*}}}]

    if {$filename!=""} {
       saveTkenvrc $filename 1 1 0 "# Tkenv config file"
    }
}

proc loadTkenvConfig {} {
    set filename "tkenv.cfg"
    set filename [tk_getOpenFile -title "Load Tkenv configuration" \
                  -defaultextension "cfg" -initialfile $filename \
                  -filetypes {{{Configuration files} {*.cfg}} {{All files} {*}}}]

    if {$filename!=""} {
       loadTkenvrc $filename
    }
}

proc editTextFile {} {
    # implements Options|File name...
    set types {
         {{Text files}             {*.txt}}
         {{Ini files}              {*.ini}}
         {{NED files}              {*.ned}}
         {{C++ files}              {*.cc *.cpp}}
         {{C++ headers}            {*.h}}
         {{Saved log files}        {*.out}}
         {{Output vectors}         {*.vec}}
         {{Output scalars}         {*.sca}}
         {{Snapshot files}         {*.sna}}
         {{Inspector lists}        {*.lst}}
         {{All files}              {*}}
    }
    set filename [tk_getOpenFile -title "View/Edit text file" \
                  -defaultextension "out" -initialfile "" \
                  -filetypes $types]

    if {$filename!=""} {
       createFileViewer $filename
    }
}

proc viewIniFile {} {
    set fname [opp_getfilename ini]
    if {$fname == ""} {
       messagebox {Info} "The current configuration manager doesn't use file input." info ok
       return
    }

    viewFile $fname
}

proc viewOutputVectorFile {} {
    set fname [opp_getfilename outvector]
    if {$fname == ""} {
       messagebox {Info} "The current output vector manager doesn't use file output." info ok
       return
    }
    if {![file exists $fname]} {
       messagebox {Info} "Output vector file not yet created (no values recorded yet)." info ok
       return
    }
    viewFile $fname
}

proc viewOutputScalarFile {} {
    set fname [opp_getfilename outscalar]
    if {$fname == ""} {
       messagebox {Info} "The current output scalar manager doesn't use file output." info ok
       return
    }
    if {![file exists $fname]} {
       messagebox {Info} "Output scalar file not yet created (no output scalars written)." info ok
       return
    }
    viewFile $fname
}

proc viewSnapshotFile {} {
    set fname [opp_getfilename snapshot]
    if {$fname == ""} {
       messagebox {Info} "The current snapshot manager doesn't use file output." info ok
       return
    }
    if {![file exists $fname]} {
       messagebox {Info} "Snapshot file not yet created (no snapshots done yet)." info ok
       return
    }
    viewFile $fname
}

proc viewFile {filename} {
    if [catch {open $filename r} f] {
       messagebox {Error} "Error: $f" info ok
       return
    } else {
       createFileViewer $filename
    }
}


