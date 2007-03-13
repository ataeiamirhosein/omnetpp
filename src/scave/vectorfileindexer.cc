//=========================================================================
//  VECTORFILEINDEXER.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <sys/stat.h>
#include <errno.h>
#include <sstream>
#include <ostream>
#include "platmisc.h"
#include "stringutil.h"
#include "scaveutils.h"
#include "filereader.h"
#include "linetokenizer.h"
#include "indexfile.h"
#include "vectorfileindexer.h"

static inline bool existsFile(const std::string fileName)
{
    struct stat s;
    return stat(fileName.c_str(), &s)==0;
}

static std::string createTempFileName(const std::string baseFileName)
{
    std::string prefix = baseFileName;
    prefix.append(".temp");
    std::string tmpFileName = prefix;
    int serial = 0;

    while (existsFile(tmpFileName))
        tmpFileName = opp_stringf("%s%d", prefix.c_str(), serial++);
    return tmpFileName;
}

void VectorFileIndexer::generateIndex(const char *vectorFileName)
{
    FileReader reader(vectorFileName);
    LineTokenizer tokenizer(1024);
    VectorFileIndex index;
    index.vectorFileName = vectorFileName;

    char *line;
    char **tokens;
    int numTokens, lineNo, numOfUnrecognizedLines = 0;
    int currentVectorId = -1;
    VectorData *currentVectorRef = NULL;
    Block currentBlock;

    while ((line=reader.getNextLineBufferPointer())!=NULL)
    {
        tokenizer.tokenize(line, reader.getLastLineLength());
        numTokens = tokenizer.numTokens();
        tokens = tokenizer.tokens();
        lineNo = reader.getNumReadLines();

        if (numTokens == 0 || tokens[0][0] == '#')
            continue;
        else if (tokens[0][0] == 'r' && strcmp(tokens[0], "run") == 0 ||
                 tokens[0][0] == 'a' && strcmp(tokens[0], "attr") == 0 ||
                 tokens[0][0] == 'p' && strcmp(tokens[0], "param") == 0)
        {
            index.run.parseLine(tokens, numTokens, vectorFileName, lineNo);
        }
        else if (tokens[0][0] == 'v' && strcmp(tokens[0], "vector") == 0)
        {
            if (numTokens < 4)
                throw opp_runtime_error("vector file indexer: broken vector declaration, file %s, line %d", vectorFileName, lineNo);

            VectorData vector;
            if (!parseInt(tokens[1], vector.vectorId))
                throw opp_runtime_error("");
            vector.moduleName = tokens[2];
            vector.name = tokens[3];
            vector.columns = numTokens >= 5 ? tokens[4] : "TV";
            vector.blockSize = 0;
            
            index.addVector(vector);
        }
        else
        {
            int vectorId;
            double simTime;
            double value;
            long eventNum = -1;

            if (!parseInt(tokens[0], vectorId))
            {
                numOfUnrecognizedLines++;
                continue;
            }

            if (currentVectorRef == NULL || vectorId != currentVectorRef->vectorId)
            {
                if (currentVectorRef != NULL)
                {
                    long blockSize = (long)(reader.getLastLineStartOffset() - currentBlock.startOffset);
                    if (blockSize > currentVectorRef->blockSize)
                        currentVectorRef->blockSize = blockSize;
                    currentVectorRef->addBlock(currentBlock);
                }

                currentBlock = Block();
                currentBlock.startOffset = reader.getLastLineStartOffset();
                currentVectorRef = index.getVector(vectorId);
                if (currentVectorRef == NULL)
                    throw opp_runtime_error("vector file indexer: missing vector declaration for id %d, file %s, line %d",
                                            vectorId, vectorFileName, lineNo);
            }

            for (int i = 0; i < currentVectorRef->columns.size(); ++i)
            {
                char column = currentVectorRef->columns[i];
                if (i+1 >= numTokens)
                    throw opp_runtime_error("vector file indexer: data line too short, file %s, line %d", vectorFileName, lineNo);

                char *token = tokens[i+1];
                switch (column)
                {
                case 'T':
                    if (!parseDouble(token, simTime))
                        throw opp_runtime_error("vector file indexer: malformed simulation time, file %s, line %d", vectorFileName, lineNo);
                    break;
                case 'V':
                    if (!parseDouble(token, value))
                        throw opp_runtime_error("vector file indexer: malformed data value, file %s, line %d", vectorFileName, lineNo);
                    break;
                case 'E':
                    if (!parseLong(token, eventNum))
                        throw opp_runtime_error("vector file indexer: malformed event number, file %s, line %d", vectorFileName, lineNo);
                    break;
                }
            }

            currentBlock.collect(eventNum, simTime, value);
        }
    }

    // finish last block
    if (currentBlock.count() > 0)
    {
        assert(currentVectorRef != NULL);
        long blockSize = (long)(reader.getFileSize() - currentBlock.startOffset);
        if (blockSize > currentVectorRef->blockSize)
            currentVectorRef->blockSize = blockSize;
        currentVectorRef->addBlock(currentBlock);
    }

    if (numOfUnrecognizedLines > 0)
    {
        fprintf(stderr, "Found %d unrecognized lines in %s.\n", numOfUnrecognizedLines, vectorFileName);
    }

    // generate index file
    std::string indexFileName = IndexFile::getIndexFileName(vectorFileName);
    std::string tempIndexFileName = createTempFileName(indexFileName);
    IndexFileWriter writer(tempIndexFileName.c_str());
    writer.writeAll(index);

    // rename generated index file
    if (unlink(indexFileName.c_str())!=0 && errno!=ENOENT)
        throw opp_runtime_error("Cannot remove original index file `%s': %s", indexFileName, strerror(errno));
    if (rename(tempIndexFileName.c_str(), indexFileName.c_str())!=0)
        throw opp_runtime_error("Cannot rename index file from '%s' to '%s': %s", tempIndexFileName.c_str(), indexFileName.c_str(), strerror(errno));
}

