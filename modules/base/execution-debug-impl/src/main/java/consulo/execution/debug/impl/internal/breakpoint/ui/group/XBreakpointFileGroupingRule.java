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
package consulo.execution.debug.impl.internal.breakpoint.ui.group;

import consulo.application.AllIcons;
import consulo.virtualFileSystem.VirtualFile;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.breakpoint.ui.XBreakpointsGroupingPriorities;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author nik
 */
public class XBreakpointFileGroupingRule<B> extends XBreakpointGroupingRule<B, XBreakpointFileGroup> {
  public XBreakpointFileGroupingRule() {
    super("by-file", XDebuggerBundle.message("rule.name.group.by.file"));
  }

  @Override
  public int getPriority() {
    return XBreakpointsGroupingPriorities.BY_FILE;
  }

  public XBreakpointFileGroup getGroup(@Nonnull final B breakpoint, @Nonnull final Collection<XBreakpointFileGroup> groups) {
    if (!(breakpoint instanceof XLineBreakpoint)) {
      return null;
    }
    XSourcePosition position = ((XLineBreakpoint)breakpoint).getSourcePosition();

    if (position == null) return null;

    VirtualFile file = position.getFile();
    for (XBreakpointFileGroup group : groups) {
      if (group.getFile().equals(file)) {
        return group;
      }
    }

    return new XBreakpointFileGroup(file);
  }

  @Nullable
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Text;
  }
}
