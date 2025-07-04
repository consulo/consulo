/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author egor
 */
@ActionImpl(id = "Debugger.ShowLibraryFrames")
public final class ShowLibraryFramesAction extends ToggleAction {
  // we should remember initial answer "isLibraryFrameFilterSupported" because on stop no debugger process, but UI is still shown
  // - we should avoid "jumping" (visible (start) - invisible (stop) - visible (start again))
  private static final String IS_LIBRARY_FRAME_FILTER_SUPPORTED = "isLibraryFrameFilterSupported";

  private static final String ourTextWhenShowIsOn = "Hide Frames from Libraries";
  private static final String ourTextWhenShowIsOff = "Show All Frames";

  private final Provider<XDebuggerSettingsManager> myXDebuggerSettingsManagerProvider;

  @Inject
  public ShowLibraryFramesAction(Provider<XDebuggerSettingsManager> provider) {
    super(LocalizeValue.empty(), LocalizeValue.empty(), PlatformIconGroup.generalFilter());
    myXDebuggerSettingsManagerProvider = provider;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);

    Presentation presentation = e.getPresentation();

    Object isSupported = presentation.getClientProperty(IS_LIBRARY_FRAME_FILTER_SUPPORTED);
    XDebugSession session = e.getData(XDebugSession.DATA_KEY);
    if (isSupported == null) {
      if (session == null) {
        // if session is null and isSupported is null - just return, it means that action created initially not in the xdebugger tab
        presentation.setVisible(false);
        return;
      }

      isSupported = session.getDebugProcess().isLibraryFrameFilterSupported();
      presentation.putClientProperty(IS_LIBRARY_FRAME_FILTER_SUPPORTED, isSupported);
    }

    if (Boolean.TRUE.equals(isSupported)) {
      presentation.setVisible(true);
      final boolean shouldShow = !Boolean.TRUE.equals(presentation.getClientProperty(SELECTED_PROPERTY));
      presentation.setText(shouldShow ? ourTextWhenShowIsOn : ourTextWhenShowIsOff);
    }
    else {
      presentation.setVisible(false);
    }
  }

  private XDebuggerSettingManagerImpl getSettingsImpl() {
    return (XDebuggerSettingManagerImpl)myXDebuggerSettingsManagerProvider.get();
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    return !getSettingsImpl().getDataViewSettings().isShowLibraryStackFrames();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean enabled) {
    boolean newValue = !enabled;
    XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().setShowLibraryStackFrames(newValue);
    XDebuggerUtil.getInstance().rebuildAllSessionsViews(e.getData(Project.KEY));
  }
}