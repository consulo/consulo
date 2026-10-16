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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.project.Project;
import consulo.ui.Popup;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.util.Map;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class BreakpointsDialogFactory {

  private Project myProject;
  private @Nullable Popup myPopupToHide;
  private @Nullable Object myBreakpoint;

  @Inject
  public BreakpointsDialogFactory(Project project) {
    myProject = project;
  }

  public void setPopupToHide(Popup popupToHide, Object breakpoint) {
    myPopupToHide = popupToHide;
    myBreakpoint = breakpoint;
  }

  public static BreakpointsDialogFactory getInstance(Project project) {
    return project.getInstance(BreakpointsDialogFactory.class);
  }

  public boolean isBreakpointPopupShowing() {
    Popup popup = myPopupToHide;
    return popup != null && popup.isVisible();
  }

  @RequiredUIAccess
  public void showDialog(@Nullable Object initialBreakpoint) {
    Object breakpoint = initialBreakpoint != null ? initialBreakpoint : myBreakpoint;

    Popup popup = myPopupToHide;
    if (popup != null) {
      if (popup.isVisible()) {
        popup.close();
      }
      myPopupToHide = null;
    }

    Map<String, String> params = breakpoint instanceof XBreakpointBase<?, ?, ?> base
      ? Map.of(BreakpointsEditorProvider.BREAKPOINT_ID, BreakpointsEditorProvider.breakpointId(base))
      : Map.of();

    myProject.getApplication()
      .getInstance(ConfigurationFileEditorManager.class)
      .open(myProject, BreakpointsEditorProvider.class, params);
  }
}
