/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/14/12
 * Time: 4:31 PM
 */
public class MergeTexts {
  private final String myLeft;
  private final String myRight;
  private final String myBase;

  public MergeTexts(String left, String right, String base) {
    myLeft = left;
    myRight = right;
    myBase = base;
  }

  public String getLeft() {
    return myLeft;
  }

  public String getRight() {
    return myRight;
  }

  public String getBase() {
    return myBase;
  }
}
