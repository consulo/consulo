/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.ui.update;

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public abstract class Update extends ComparableObject.Impl implements Runnable, Comparable {

  public static final int LOW_PRIORITY = 999;
  public static final int HIGH_PRIORITY = 10;

  private boolean myProcessed;
  private boolean myRejected;
  private final boolean myExecuteInWriteAction;

  private int myPriority = LOW_PRIORITY;

  public Update(@NonNls Object identity) {
    this(identity, false);
  }

  public Update(@NonNls Object identity, int priority) {
    this(identity, false, priority);
  }

  public Update(@NonNls Object identity, boolean executeInWriteAction) {
    this(identity, executeInWriteAction, LOW_PRIORITY);
  }

  public Update(@NonNls Object identity, boolean executeInWriteAction, int priority) {
    super(identity);
    myExecuteInWriteAction = executeInWriteAction;
    myPriority = priority;
  }

  public boolean isDisposed() {
    return false;
  }
  
  public boolean isExpired() {
    return false;
  }

  public boolean wasProcessed() {
    return myProcessed;
  }

  public void setProcessed() {
    myProcessed = true;
  }

  public boolean executeInWriteAction() {
    return myExecuteInWriteAction;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return super.toString() + " Objects: " + Arrays.asList(getEqualityObjects());
  }

  public int compareTo(Object o) {
    Update another = (Update) o;

    int weightResult = getPriority() < another.getPriority() ? -1 : (getPriority() == another.getPriority() ? 0 : 1);

    if (weightResult == 0) {
      return  equals(o) ? 0 : 1;
    } 
    else {
      return weightResult;
    }
  }

  public int getPriority() {
    return myPriority;
  }

  public boolean canEat(Update update) {
    return false;
  }

  public void setRejected() {
    myRejected = true;
  }

  public boolean isRejected() {
    return myRejected;
  }
}
