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

public class JumpData implements CoverageData {
  private int myTrueHits;
  private int myFalseHits;

  public void touchTrueHit() {
    myTrueHits++;
  }

  public void touchFalseHit() {
    myFalseHits++;
  }

  public int getTrueHits() {
    return myTrueHits;
  }

  public int getFalseHits() {
    return myFalseHits;
  }

  public void save(final DataOutputStream os) throws IOException {
    CoverageIOUtil.writeINT(os, myTrueHits);
    CoverageIOUtil.writeINT(os, myFalseHits);
  }

  public void merge(final CoverageData data) {
    final JumpData jumpData = (JumpData)data;
    myTrueHits += jumpData.myTrueHits;
    myFalseHits += jumpData.myFalseHits;
  }

  public void setTrueHits(final int trueHits) {
    myTrueHits = trueHits;
  }

  public void setFalseHits(final int falseHits) {
    myFalseHits = falseHits;
  }
}
