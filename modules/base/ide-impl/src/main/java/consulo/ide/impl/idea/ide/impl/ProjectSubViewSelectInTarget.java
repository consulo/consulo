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

package consulo.ide.impl.idea.ide.impl;

import consulo.localize.LocalizeValue;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class ProjectSubViewSelectInTarget implements SelectInTarget {
  private final ProjectViewSelectInTarget myBaseTarget;
  private final String mySubId;
  private final int myWeight;

  public ProjectSubViewSelectInTarget(ProjectViewSelectInTarget target, String subId, int weight) {
    myBaseTarget = target;
    mySubId = subId;
    myWeight = weight;
  }

  @Override
  public boolean canSelect(SelectInContext context) {
    return myBaseTarget.isSubIdSelectable(mySubId, context);
  }

  @Override
  public void selectIn(SelectInContext context, boolean requestFocus) {
    myBaseTarget.setSubId(mySubId);
    myBaseTarget.selectIn(context, requestFocus);
  }

  @Override
  public String getToolWindowId() {
    return myBaseTarget.getToolWindowId();
  }

  @Override
  public String getMinorViewId() {
    return myBaseTarget.getMinorViewId();
  }

  @Override
  public float getWeight() {
    return myWeight;
  }

  @Nonnull
  @Override
  public LocalizeValue getActionText() {
    return myBaseTarget.getSubIdPresentableName(mySubId);
  }
}
