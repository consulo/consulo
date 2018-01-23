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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel.Sher
 */
public class JumpsAndSwitches implements CoverageData {
  private List myJumps;
  private JumpData[] myJumpsArray;

  private List mySwitches;
  private SwitchData[] mySwitchesArray;

  public JumpData[] getJumps() {
    return myJumpsArray;
  }

  public SwitchData[] getSwitches() {
    return mySwitchesArray;
  }

  public JumpData addJump(final int jump) {
    if (myJumps == null) myJumps = new ArrayList();
    if (myJumps.size() <= jump) {
      for (int i = myJumps.size(); i <= jump; i++){
        myJumps.add(new JumpData());
      }
    }
    return (JumpData)myJumps.get(jump);
  }

  public JumpData getJumpData(int jump) {
    return myJumpsArray == null ? null : myJumpsArray[jump];
  }

  public SwitchData addSwitch(final int switchNumber, final int[] keys) {
    if (mySwitches == null) mySwitches = new ArrayList();
    final SwitchData switchData = new SwitchData(keys);
    if (mySwitches.size() <= switchNumber) {
      for(int i = mySwitches.size(); i < switchNumber; i++) {
        mySwitches.add(new SwitchData(new int[0]));
      }
      if (mySwitches.size() == switchNumber) {
        mySwitches.add(switchData);
      }
    }
    return (SwitchData)mySwitches.get(switchNumber);
  }

  public SwitchData getSwitchData(int switchNumber) {
    return mySwitchesArray == null ? null : mySwitchesArray[switchNumber];
  }

  public void save(final DataOutputStream os) throws IOException {
    CoverageIOUtil.writeINT(os, myJumpsArray != null ? myJumpsArray.length : 0);
    if (myJumpsArray != null) {
      for (int j = 0; j < myJumpsArray.length; j++) {
        myJumpsArray[j].save(os);
      }
    }
    CoverageIOUtil.writeINT(os, mySwitchesArray != null ? mySwitchesArray.length : 0);
    if (mySwitchesArray != null) {
      for (int s = 0; s < mySwitchesArray.length; s++) {
        mySwitchesArray[s].save(os);
      }
    }
  }

  public void removeJump(final int jump) {
    if (jump > 0 && jump <= myJumps.size()) {
      myJumps.remove(jump - 1);
    }
  }

  public void fillArrays() {
    if (myJumps != null) {
      myJumpsArray = new JumpData[myJumps.size()];
      for (int i = 0; i < myJumps.size(); i++) {
        myJumpsArray[i] = (JumpData)myJumps.get(i);
      }
      myJumps = null;
    }
    if (mySwitches != null) {
      mySwitchesArray = new SwitchData[mySwitches.size()];
      for (int i = 0; i < mySwitches.size(); i++) {
        mySwitchesArray[i] = (SwitchData)mySwitches.get(i);
      }
      mySwitches = null;
    }
  }

  public void merge(final CoverageData data) {
    JumpsAndSwitches jumpsData = (JumpsAndSwitches)data;
    if (jumpsData.myJumpsArray != null) {
      if (myJumpsArray == null) {
        myJumpsArray = new JumpData[jumpsData.myJumpsArray.length];
      }
      else if (jumpsData.myJumpsArray != null) {
        if (myJumpsArray.length < jumpsData.myJumpsArray.length) {
          JumpData[] extJumpsArray = new JumpData[jumpsData.myJumpsArray.length];
          System.arraycopy(myJumpsArray, 0, extJumpsArray, 0, myJumpsArray.length);
          myJumpsArray = extJumpsArray;
        }
      }
      mergeJumps(myJumpsArray, jumpsData.myJumpsArray);
    }
    if (jumpsData.mySwitchesArray != null) {
      if (mySwitchesArray == null) {
        mySwitchesArray = new SwitchData[jumpsData.mySwitchesArray.length];
      }
      else if (jumpsData.mySwitchesArray != null) {
        if (mySwitchesArray.length < jumpsData.mySwitchesArray.length) {
          SwitchData[] extJumpsArray = new SwitchData[jumpsData.mySwitchesArray.length];
          System.arraycopy(mySwitchesArray, 0, extJumpsArray, 0, mySwitchesArray.length);
          mySwitchesArray = extJumpsArray;
        }
      }
      mergeSwitches(mySwitchesArray, jumpsData.mySwitchesArray);
    }
  }

  private static void mergeSwitches(SwitchData[] myArray, SwitchData[] array) {
    for (int i = 0; i < array.length; i++) {
      SwitchData switchData = myArray[i];
      if (switchData == null) {
        if (array[i] == null) continue;
        switchData = new SwitchData(array[i].getKeys());
        myArray[i] = switchData;
      }
      switchData.merge(array[i]);
    }
  }

  private static void mergeJumps(JumpData[] myArray, JumpData[] array) {
    for (int i = 0; i < array.length; i++) {
      JumpData switchData = myArray[i];
      if (switchData == null) {
        if (array[i] == null) continue;
        switchData = new JumpData();
        myArray[i] = switchData;
      }
      switchData.merge(array[i]);
    }
  }
}
