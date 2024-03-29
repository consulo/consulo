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
package consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.grouping;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroup;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XBreakpointUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

public class XBreakpointTypeGroup extends XBreakpointGroup {

  private XBreakpointType myBreakpointType;

  public XBreakpointTypeGroup(XBreakpointType type) {
    myBreakpointType = type;
  }

  @Nonnull
  @Override
  public String getName() {
    return myBreakpointType.getTitle();
  }

  public XBreakpointType getBreakpointType() {
    return myBreakpointType;
  }

  @Override
  public Image getIcon(boolean isOpen) {
    return myBreakpointType.getEnabledIcon();
  }

  @Override
  public int compareTo(XBreakpointGroup o) {
    if (o instanceof XBreakpointTypeGroup) {
      return indexOfType(myBreakpointType) - indexOfType(((XBreakpointTypeGroup)o).getBreakpointType());
    }
    return -o.compareTo(this);
  }

  private static int indexOfType(XBreakpointType type) {
    return ContainerUtil.indexOf(XBreakpointUtil.getBreakpointTypes(), type);
  }
}
