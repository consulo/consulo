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

package com.intellij.codeInspection.ui;

import consulo.ui.image.Image;

/**
 * @author max
 */
public class InspectionGroupNode extends InspectionTreeNode {
  private static final Image EMPTY = Image.empty(0, Image.DEFAULT_ICON_SIZE);

  public InspectionGroupNode(String groupTitle) {
    super(groupTitle);
  }

  public String getGroupTitle() {
    return (String) getUserObject();
  }

  @Override
  public Image getIcon() {
    return EMPTY;
  }

  @Override
  public boolean appearsBold() {
    return true;
  }
}
