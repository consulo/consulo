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
package consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XBreakpointUtil;
import consulo.project.Project;
import consulo.ui.ex.popup.Balloon;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class BreakpointsDialogFactory {

  private Project myProject;
  private Balloon myBalloonToHide;
  private Object myBreakpoint;
  private BreakpointsDialog myDialogShowing;

  @Inject
  public BreakpointsDialogFactory(Project project) {
    myProject = project;
  }

  public void setBalloonToHide(Balloon balloonToHide, Object breakpoint) {
    myBalloonToHide = balloonToHide;
    myBreakpoint = breakpoint;
  }

  public static BreakpointsDialogFactory getInstance(Project project) {
    return ServiceManager.getService(project, BreakpointsDialogFactory.class);
  }

  public boolean isBreakpointPopupShowing() {
    return (myBalloonToHide != null && !myBalloonToHide.isDisposed()) || myDialogShowing != null;
  }

  public void showDialog(@Nullable Object initialBreakpoint) {
    if (myDialogShowing != null) {
      return;
    }

    final BreakpointsDialog dialog = new BreakpointsDialog(myProject, initialBreakpoint != null ? initialBreakpoint : myBreakpoint, XBreakpointUtil.collectPanelProviders()) {
      @Override
      protected void dispose() {
        for (BreakpointPanelProvider provider : XBreakpointUtil.collectPanelProviders()) {
          provider.onDialogClosed(myProject);
        }
        myDialogShowing = null;

        super.dispose();
      }
    };

    if (myBalloonToHide != null) {
      if (!myBalloonToHide.isDisposed()) {
        myBalloonToHide.hide();
      }
      myBalloonToHide = null;
    }
    myDialogShowing = dialog;

    dialog.show();
  }
}
