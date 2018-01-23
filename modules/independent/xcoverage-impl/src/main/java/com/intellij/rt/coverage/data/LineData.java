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

import java.io.DataOutputStream;
import java.io.IOException;

public class LineData implements CoverageData {
  private final int myLineNumber;
  private String myMethodSignature;

  private int myHits = 0;

  private byte myStatus = -1;
  private String myUniqueTestName = null;
  private boolean myMayBeUnique = true;

  private JumpsAndSwitches myJumpsAndSwitches;

  public LineData(final int line, final String desc) {
    myLineNumber = line;
    myMethodSignature = desc;
  }

  public void touch() {
    myHits++;
    setTestName(ProjectData.getCurrentTestName());
  }

  public int getHits() {
    return myHits;
  }

  JumpsAndSwitches getOrCreateJumpsAndSwitches() {
    if (myJumpsAndSwitches == null) {
      myJumpsAndSwitches = new JumpsAndSwitches();
    }
    return myJumpsAndSwitches;
  }

  public int getStatus() {
    if (myStatus != -1) return myStatus;
    if (myHits == 0) {
      myStatus = LineCoverage.NONE;
      return myStatus;
    }

    if (myJumpsAndSwitches != null) {
      JumpData[] jumps = getOrCreateJumpsAndSwitches().getJumps();
      if (jumps != null) {
        for (int i = 0; i < jumps.length; i++) {
          final JumpData jumpData = jumps[i];
          if ((jumpData.getFalseHits() > 0 ? 1 : 0) + (jumpData.getTrueHits() > 0 ? 1 : 0) < 2){
            myStatus = LineCoverage.PARTIAL;
            return myStatus;
          }
        }
      }

      SwitchData[] switches = getOrCreateJumpsAndSwitches().getSwitches();
      if (switches != null) {
        for (int s = 0; s < switches.length; s++) {
          final SwitchData switchData = switches[s];
          if (switchData.getDefaultHits() == 0){
            myStatus = LineCoverage.PARTIAL;
            return myStatus;
          }
          for (int i = 0; i < switchData.getHits().length; i++) {
            int hit = switchData.getHits()[i];
            if (hit == 0){
              myStatus = LineCoverage.PARTIAL;
              return myStatus;
            }
          }
        }
      }
    }

    myStatus = LineCoverage.FULL;
    return myStatus;
  }

  public void save(final DataOutputStream os) throws IOException {
    CoverageIOUtil.writeINT(os, myLineNumber);
    CoverageIOUtil.writeUTF(os, myUniqueTestName != null ? myUniqueTestName : "");
    CoverageIOUtil.writeINT(os, myHits);
    if (myHits > 0) {
      if (myJumpsAndSwitches != null) {
        getOrCreateJumpsAndSwitches().save(os);
      } else {
        new JumpsAndSwitches().save(os);
      }
    }
  }

  public void merge(final CoverageData data) {
    LineData lineData = (LineData)data;
    myHits += lineData.myHits;
    if (myJumpsAndSwitches != null || lineData.myJumpsAndSwitches != null) {
      getOrCreateJumpsAndSwitches().merge(lineData.getOrCreateJumpsAndSwitches());
    }
    if (lineData.myMethodSignature != null) {
      myMethodSignature = lineData.myMethodSignature;
    }
    if (myStatus != -1) {
      byte status = (byte) lineData.getStatus();
      if (status > myStatus) {
        myStatus = status;
      }
    }
  }

  public JumpData addJump(final int jump) {
    return getOrCreateJumpsAndSwitches().addJump(jump);
  }

  public JumpData getJumpData(int jump) {
    return getOrCreateJumpsAndSwitches().getJumpData(jump);
  }

  public void touchBranch(final int jump, final boolean hit) {
    final JumpData jumpData = getJumpData(jump);
    if (jumpData != null) {
      if (hit) {
        jumpData.touchTrueHit();
      }
      else {
        jumpData.touchFalseHit();
      }
    }
  }

  public SwitchData addSwitch(final int switchNumber, final int[] keys) {
    return getOrCreateJumpsAndSwitches().addSwitch(switchNumber, keys);
  }

  public SwitchData getSwitchData(int switchNumber) {
    return getOrCreateJumpsAndSwitches().getSwitchData(switchNumber);
  }

  public SwitchData addSwitch(final int switchNumber, final int min, final int max) {
    int[] keys = new int[max - min + 1];
    for (int i = min; i <= max; i++) {
      keys[i - min] = i;
    }
    return addSwitch(switchNumber, keys);
  }

  public void touchBranch(final int switchNumber, final int key) {
    final SwitchData switchData = getSwitchData(switchNumber);
    if (switchData != null) {
      switchData.touch(key);
    }
  }

  public int getLineNumber() {
    return myLineNumber;
  }

  public String getMethodSignature() {
    return myMethodSignature;
  }

  public void setStatus(final byte status) {
    myStatus = status;
  }

  public void setTrueHits(final int jumpNumber, final int trueHits) {
    addJump(jumpNumber).setTrueHits(trueHits);
  }

  public void setFalseHits(final int jumpNumber, final int falseHits) {
    addJump(jumpNumber).setFalseHits(falseHits);
  }

  public void setDefaultHits(final int switchNumber, final int[] keys, final int defaultHit) {
    addSwitch(switchNumber, keys).setDefaultHits(defaultHit);
  }

  public void setSwitchHits(final int switchNumber, final int[] keys, final int[] hits) {
    addSwitch(switchNumber, keys).setKeysAndHits(keys, hits);
  }

  public JumpData[] getJumps() {
    if (myJumpsAndSwitches == null) return null;
    return getOrCreateJumpsAndSwitches().getJumps();
  }

  public SwitchData[] getSwitches() {
    if (myJumpsAndSwitches == null) return null;
    return getOrCreateJumpsAndSwitches().getSwitches();
  }

  public void setHits(final int hits) {
    myHits = hits;
  }

  public void setTestName(String testName) {
    if (testName != null) {
      if (myUniqueTestName == null) {
        if (myMayBeUnique) myUniqueTestName = testName;
      } else if (!myUniqueTestName.equals(testName)) {
        myUniqueTestName = null;
        myMayBeUnique = false;
      }
    }
  }

  public boolean isCoveredByOneTest() {
    return myUniqueTestName != null && myUniqueTestName.length() > 0;
  }

  public void removeJump(final int jump) {
    if (myJumpsAndSwitches == null) return;
    getOrCreateJumpsAndSwitches().removeJump(jump);
  }

  public void fillArrays() {
    if (myJumpsAndSwitches == null) return;
    getOrCreateJumpsAndSwitches().fillArrays();
  }
}
