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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.project.Project;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * @author nik
 */
public abstract class BreakpointPanelProvider<B> {

  public abstract void createBreakpointsGroupingRules(Collection<XBreakpointGroupingRule> rules);

  public interface BreakpointsListener {
    void breakpointsChanged();
  }

  public abstract void addListener(BreakpointsListener listener, Project project, Disposable disposable);

  protected abstract void removeListener(BreakpointsListener listener);

  public abstract int getPriority();

  @Nullable
  public abstract B findBreakpoint(@Nonnull Project project, @Nonnull Document document, int offset);

  @Nullable
  public abstract GutterIconRenderer getBreakpointGutterIconRenderer(Object breakpoint);

  public abstract void onDialogClosed(final Project project);

  public abstract void provideBreakpointItems(Project project, Collection<BreakpointItem> items);
}
