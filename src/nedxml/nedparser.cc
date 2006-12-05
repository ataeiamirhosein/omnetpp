//==========================================================================
//  NEDPARSER.CC - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#include <assert.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sstream>

#include "nedparser.h"
#include "nedfilebuffer.h"
#include "nedelements.h"
#include "nederror.h"

#include "nedyydefs.h"


NEDParser *np;

//--------

NEDParser::NEDParser(NEDErrorStore *e)
{
    nedsource = NULL;
    parseexpr = true;
    storesrc = false;
    errors = e;
}

NEDParser::~NEDParser()
{
    delete nedsource;
}

NEDElement *NEDParser::parseNEDFile(const char *fname)
{
    if (!loadFile(fname))
        return NULL;
    return parseNED();
}

NEDElement *NEDParser::parseNEDText(const char *nedtext)
{
    if (!loadText(nedtext))
        return NULL;
    return parseNED();
}

NEDElement *NEDParser::parseMSGFile(const char *fname)
{
    if (!loadFile(fname))
        return NULL;
    return parseMSG();
}

NEDElement *NEDParser::parseMSGText(const char *nedtext)
{
    if (!loadText(nedtext))
        return NULL;
    return parseMSG();
}

bool NEDParser::loadFile(const char *fname)
{
    // init class members
    if (nedsource) delete nedsource;
    nedsource = new NEDFileBuffer();
    filename = fname;
    errors->clear();

    // cosmetics on file name: substitute "~"
    char newfilename[1000];
    if (fname[0]=='~') {
        sprintf(newfilename,"%s%s",getenv("HOME"),fname+1);
    } else {
        strcpy(newfilename,fname);
    }

    // load whole file into memory
    if (!nedsource->readFile(newfilename))
        {errors->add(NULL, "cannot read %s", fname); return false;}
    return true;
}

bool NEDParser::loadText(const char *nedtext)
{
    // init vars
    if (nedsource) delete nedsource;
    nedsource = new NEDFileBuffer();
    filename = "buffer";
    errors->clear();

    // prepare nedsource object
    if (!nedsource->setData(nedtext))
        {errors->add(NULL, "unable to allocate work memory"); return false;}
    return true;
}

NEDElement *NEDParser::parseNED()
{
    errors->clear();
    if (guessIsNEDInNewSyntax(nedsource->getFullText()))
        return ::doParseNED2(this, nedsource->getFullText());
    else
        return ::doParseNED1(this, nedsource->getFullText());
}

NEDElement *NEDParser::parseMSG()
{
    errors->clear();
    return ::doParseMSG2(this, nedsource->getFullText());
}

bool NEDParser::guessIsNEDInNewSyntax(const char *txt)
{
    // first, remove all comments and string literals
    char *buf = new char [strlen(txt)+1];
    const char *s;
    char *d;
    for (s=txt, d=buf; *s; )
    {
        if (*s=='/' && *(s+1)=='/') {
            // if there's a comment, skip rest of the line
            s += 2;
            while (*s && *s!='\r' && *s!='\n')
                s++;
        }
        else if (*s=='"') {
            // leave out string literals as well
            s++;
            while (*s && *s!='\r' && *s!='\n' && *s!='"')
                if (*s++=='\\')
                    s++;
            if (*s=='"')
                s++;
        }
        else {
            // copy everything else
            *d++ = *s++;
        }
    }
    *d = '\0';

    // Only in NED2 are curly braces {} and "@" allowed and widely used.
    // If a NED2 source doesn't contain none of those, it might only contain
    // imports (but what for?), and may as well be parsed as old NED.
    // Note: searching for keywords would not be a bulletproof solution because
    // old NED keywords are not reserved in NED2; moreover we'd have to search
    // "whole words only" so strstr() is no good.

    bool isNewSyntax = strchr(buf,'{') || strchr(buf,'}') || strchr(buf,'@');

    // cleanup
    delete [] buf;

    return isNewSyntax;
}

void NEDParser::error(const char *msg, int line)
{
    std::stringstream os;
    os << filename << ":" << line;
    errors->add(os.str().c_str(), ERRCAT_ERROR, "%s", msg);
}

