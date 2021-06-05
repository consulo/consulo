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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import gnu.trove.TIntObjectHashMap;

import java.io.*;

/**
 * @author anna
 * @since 05-May-2009
 */
public class ProjectDataLoader {

  public static ProjectData load(File sessionDataFile) {
    final ProjectData projectInfo = new ProjectData();
    DataInputStream in = null;
    try {
      in = new DataInputStream(new BufferedInputStream(new FileInputStream(sessionDataFile)));
      final TIntObjectHashMap dict = new TIntObjectHashMap(1000, 0.99f);
      final int classCount = CoverageIOUtil.readINT(in);
      for (int c = 0; c < classCount; c++) {
        final ClassData classInfo = projectInfo.getOrCreateClassData(StringsPool.getFromPool(CoverageIOUtil.readUTFFast(in)));
        dict.put(c, classInfo);
      }
      for (int c = 0; c < classCount; c++) {
        final ClassData classInfo = (ClassData)dict.get(CoverageIOUtil.readINT(in));
        final int methCount = CoverageIOUtil.readINT(in);
        final TIntObjectHashMap lines = new TIntObjectHashMap(4, 0.99f);
        int maxLine = 1;
        for (int m = 0; m < methCount; m++) {
          final String methodSig = expand(in, dict);
          final int lineCount = CoverageIOUtil.readINT(in);
          for (int l = 0; l < lineCount; l++) {
            final int line = CoverageIOUtil.readINT(in);
            LineData lineInfo = (LineData) lines.get(line);
            if (lineInfo == null) {
              lineInfo = new LineData(line, StringsPool.getFromPool(methodSig));
              lines.put(line, lineInfo);
              if (line > maxLine) maxLine = line;
            }
            classInfo.registerMethodSignature(lineInfo);
            String testName = CoverageIOUtil.readUTFFast(in);
            if (testName != null && testName.length() > 0) {
              lineInfo.setTestName(testName);
            }
            final int hits = CoverageIOUtil.readINT(in);
            lineInfo.setHits(hits);
            if (hits > 0) {
              final int jumpsNumber = CoverageIOUtil.readINT(in);
              for (int j = 0; j < jumpsNumber; j++) {
                lineInfo.setTrueHits(j, CoverageIOUtil.readINT(in));
                lineInfo.setFalseHits(j, CoverageIOUtil.readINT(in));
              }
              final int switchesNumber = CoverageIOUtil.readINT(in);
              for (int s = 0; s < switchesNumber; s++) {
                final int defaultHit = CoverageIOUtil.readINT(in);
                final int keysLength = CoverageIOUtil.readINT(in);
                final int[] keys = new int[keysLength];
                final int[] keysHits = new int[keysLength];
                for (int k = 0; k < keysLength; k++) {
                  keys[k] = CoverageIOUtil.readINT(in);
                  keysHits[k] = CoverageIOUtil.readINT(in);
                }
                lineInfo.setDefaultHits(s, keys, defaultHit);
                lineInfo.setSwitchHits(s, keys, keysHits);
              }
            }
            lineInfo.fillArrays();
          }
        }
        classInfo.setLines(LinesUtil.calcLineArray(maxLine, lines));
      }
    } catch (Exception e) {
      ErrorReporter.reportError("Failed to load coverage data from file: " + sessionDataFile.getAbsolutePath(), e);
      return projectInfo;
    }
    finally {
      try {
        in.close();
      }
      catch (IOException e) {
        ErrorReporter.reportError("Failed to close file: " + sessionDataFile.getAbsolutePath(), e);
      }
    }
    return projectInfo;
  }

  private static String expand(DataInputStream in, final TIntObjectHashMap dict) throws IOException {
    return CoverageIOUtil.processWithDictionary(CoverageIOUtil.readUTFFast(in), new CoverageIOUtil.Consumer() {
      protected String consume(String type) {
        final int typeIdx;
        try {
          typeIdx = Integer.parseInt(type);
        } catch (NumberFormatException e) {
          return type;
        }
        return ((ClassData) dict.get(typeIdx)).getName();
      }
    });
  }
}