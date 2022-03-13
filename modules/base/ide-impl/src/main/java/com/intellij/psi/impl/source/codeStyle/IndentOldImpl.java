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

package com.intellij.psi.impl.source.codeStyle;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.IndentOld;

public class IndentOldImpl implements IndentOld {
  private final CodeStyleSettings mySettings;
  private final int myIndentLevel;
  private final int mySpaceCount;
  private final FileType myFileType;

  public IndentOldImpl(CodeStyleSettings settings, int indentLevel, int spaceCount, FileType fileType) {
    mySettings = settings;
    myIndentLevel = indentLevel;
    mySpaceCount = spaceCount;
    myFileType = fileType;
  }

  int getIndentLevel() {
    return myIndentLevel;
  }

  int getSpaceCount() {
    return mySpaceCount;
  }

  public boolean equals(Object o) {
    if (!(o instanceof IndentOldImpl)) return false;

    IndentOldImpl indent = (IndentOldImpl)o;

    if (myIndentLevel != indent.myIndentLevel) return false;
    if (mySpaceCount != indent.mySpaceCount) return false;
    if (!mySettings.equals(indent.mySettings)) return false;

    return true;
  }

  public int hashCode() {
    return myIndentLevel + mySpaceCount;
  }

  @Override
  public boolean isGreaterThan(IndentOld indent) {
    return getSize() > ((IndentOldImpl)indent).getSize();
  }

  @Override
  public IndentOld min(IndentOld anotherIndent) {
    return isGreaterThan(anotherIndent) ? anotherIndent : this;
  }

  @Override
  public IndentOld max(IndentOld anotherIndent) {
    return isGreaterThan(anotherIndent) ? this : anotherIndent;
  }

  @Override
  public IndentOld add(IndentOld indent) {
    IndentOldImpl indent1 = (IndentOldImpl)indent;
    return new IndentOldImpl(mySettings, myIndentLevel + indent1.myIndentLevel, mySpaceCount + indent1.mySpaceCount, myFileType);
  }

  @Override
  public IndentOld subtract(IndentOld indent) {
    IndentOldImpl indent1 = (IndentOldImpl)indent;
    return new IndentOldImpl(mySettings, myIndentLevel - indent1.myIndentLevel, mySpaceCount - indent1.mySpaceCount, myFileType);
  }

  @Override
  public boolean isZero() {
    return myIndentLevel == 0 && mySpaceCount == 0;
  }

  private int getSize(){
    return myIndentLevel * mySettings.getIndentSize(myFileType) + mySpaceCount;
  }
}
