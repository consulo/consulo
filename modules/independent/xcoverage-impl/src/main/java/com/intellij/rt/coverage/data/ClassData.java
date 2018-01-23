/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.DictionaryLookup;
import com.intellij.rt.coverage.util.ErrorReporter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClassData implements CoverageData {
  private final String myClassName;
  private LineData[] myLinesArray;
  private Map myStatus;
  private int[] myLineMask;
  private String mySource;

  public ClassData(final String name) {
    myClassName = name;
  }

  public String getName() {
    return myClassName;
  }

  public void save(final DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {
    CoverageIOUtil.writeINT(os, dictionaryLookup.getDictionaryIndex(myClassName));
    final Map sigLines = prepareSignaturesMap(dictionaryLookup);
    final Set sigs = sigLines.keySet();
    CoverageIOUtil.writeINT(os, sigs.size());
    for (Iterator it = sigs.iterator(); it.hasNext();) {
      final String sig = (String)it.next();
      CoverageIOUtil.writeUTF(os, sig);
      final List lines = (List)sigLines.get(sig);
      CoverageIOUtil.writeINT(os, lines.size());
      for (int i = 0; i < lines.size(); i++) {
        ((LineData)lines.get(i)).save(os);
      }
    }
  }

  private Map prepareSignaturesMap(DictionaryLookup dictionaryLookup) {
    final Map sigLines = new HashMap();
    if (myLinesArray == null) return sigLines;
    for (int i = 0; i < myLinesArray.length; i++) {
      final LineData lineData = myLinesArray[i];
      if (lineData == null) continue;
      if (myLineMask != null) {
        lineData.setHits(myLineMask[lineData.getLineNumber()]);
      }
      final String sig = CoverageIOUtil.collapse(lineData.getMethodSignature(), dictionaryLookup);
      List lines = (List)sigLines.get(sig);
      if (lines == null) {
        lines = new ArrayList();
        sigLines.put(sig, lines);
      }
      lines.add(lineData);
    }
    return sigLines;
  }

  public void merge(final CoverageData data) {
    ClassData classData = (ClassData) data;
    mergeLines(classData.myLinesArray);
    final Iterator iterator = getMethodSigs().iterator();
    while (iterator.hasNext()) {
      myStatus.put(iterator.next(), null);
    }
    if (mySource == null && classData.mySource != null) {
      mySource = classData.mySource;
    }
  }

  private void mergeLines(LineData[] dLines) {
    if (dLines == null) return;
    if (myLinesArray == null || myLinesArray.length < dLines.length) {
      LineData[] lines = new LineData[dLines.length];
      if (myLinesArray != null) {
        System.arraycopy(myLinesArray, 0, lines, 0, myLinesArray.length);
      }
      myLinesArray = lines;
    }
    for (int i = 0; i < dLines.length; i++) {
      final LineData mergedData = dLines[i];
      if (mergedData == null) continue;
      LineData lineData = myLinesArray[i];
      if (lineData == null) {
        lineData = new LineData(mergedData.getLineNumber(), mergedData.getMethodSignature());
        registerMethodSignature(lineData);
        myLinesArray[i] = lineData;
      }
      lineData.merge(mergedData);
    }
  }

  public void touchLine(int line) {
    myLineMask[line]++;
  }

  public void touch(int line) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touch();
    }
  }

  public void touch(int line, int jump, boolean hit) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBranch(jump, hit);
    }
  }

  public void touch(int line, int switchNumber, int key) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBranch(switchNumber, key);
    }
  }

  public void registerMethodSignature(LineData lineData) {
    initStatusMap();
    myStatus.put(lineData.getMethodSignature(), null);
  }

  public LineData getLineData(int line) {
    return myLinesArray[line];
  }

  /** @noinspection UnusedDeclaration*/
  public Object[] getLines() {
    return myLinesArray;
  }

  /** @noinspection UnusedDeclaration*/
  public boolean containsLine(int line) {
    return myLinesArray[line] != null;
  }

  /** @noinspection UnusedDeclaration*/
  public Collection getMethodSigs() {
    initStatusMap();
    return myStatus.keySet();
  }

  private void initStatusMap() {
    if (myStatus == null) myStatus = new HashMap();
  }

  /** @noinspection UnusedDeclaration*/
  public Integer getStatus(String methodSignature) {
    Integer methodStatus = (Integer)myStatus.get(methodSignature);
    if (methodStatus == null) {
      for (int i = 0; i < myLinesArray.length; i++) {
        final LineData lineData = myLinesArray[i];
        if (lineData != null && methodSignature.equals(lineData.getMethodSignature())) {
          if (lineData.getStatus() != LineCoverage.NONE) {
            methodStatus = new Integer(LineCoverage.PARTIAL);
            break;
          }
        }
      }
      if (methodStatus == null) methodStatus = new Integer(LineCoverage.NONE);
      myStatus.put(methodSignature, methodStatus);
    }
    return methodStatus;
  }

  public String toString() {
    return myClassName;
  }

  public void initLineMask(LineData[] lines) {
    if (myLineMask == null) {
      myLineMask = new int[myLinesArray != null ? Math.max(lines.length, myLinesArray.length) : lines.length];
      Arrays.fill(myLineMask, 0);
      if (myLinesArray != null) {
        for (int i = 0; i < myLinesArray.length; i++) {
          final LineData data = myLinesArray[i];
          if (data != null) {
            myLineMask[i] = data.getHits();
          }
        }
      }
    } else {
      if (myLineMask.length < lines.length) {
        int[] lineMask = new int[lines.length];
        System.arraycopy(myLineMask, 0, lineMask, 0, myLineMask.length);
        myLineMask = lineMask;
      }
      for (int i = 0; i < lines.length; i++) {
        if (lines[i] != null) {
          myLineMask[i] += lines[i].getHits();
        }
      }
    }
  }

  public void setLines(LineData[] lines) {
    if (myLinesArray == null) {
      myLinesArray = lines;
    } else {
      mergeLines(lines);
    }
  }

  public void checkLineMappings(LineMapData[] linesMap, ClassData classData) {
    if (linesMap != null) {
      LineData[] result;
      try {
        result = new LineData[linesMap.length];
        for (int i = 0, linesMapLength = linesMap.length; i < linesMapLength; i++) {
          final LineMapData mapData = linesMap[i];
          if (mapData != null) {
            result[mapData.getSourceLineNumber()] = classData.createSourceLineData(mapData);
          }
        }
      }
      catch (Throwable e) {
        ErrorReporter.reportError("Error creating line mappings for " + classData.getName(), e);
        return;
      }
      myLinesArray = result;
      myLineMask = null;
    }
  }

  private LineData createSourceLineData(LineMapData lineMapData) {
    for (int i = lineMapData.getTargetMinLine(); i <= lineMapData.getTargetMaxLine() && i < myLinesArray.length; i++) {
      final LineData targetLineData = getLineData(i);
      if (targetLineData != null) { //todo ??? show info according to one target line

        final LineData lineData = new LineData(lineMapData.getSourceLineNumber(), targetLineData.getMethodSignature());

        lineData.merge(targetLineData);
        if (myLineMask != null) {
          lineData.setHits(myLineMask[i]);
        }

        return lineData;

      }
    }
    return null;
  }

  public void setSource(String source) {
    this.mySource = source;
  }

  public String getSource() {
    return mySource;
  }

  public int[] touchLines(int[] lines) { //todo
    myLineMask = lines;
    return lines;
  }
}
