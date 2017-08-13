/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.IndentGuideDescriptor;
import com.intellij.openapi.editor.IndentsModel;

import java.util.List;

public class EmptyIndentsModel implements IndentsModel {

  @Override
  public IndentGuideDescriptor getCaretIndentGuide() {
    return null;
  }

  @Override
  public IndentGuideDescriptor getDescriptor(int startLine, int endLine) {
    return null;
  }

  @Override
  public void assumeIndents(List<IndentGuideDescriptor> descriptors) {
  }
}
