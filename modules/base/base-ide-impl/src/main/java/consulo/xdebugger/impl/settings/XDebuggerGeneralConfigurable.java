/*
 * Copyright 2013-2016 consulo.io
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
package consulo.xdebugger.impl.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.impl.settings.XDebuggerGeneralSettings;
import com.intellij.xdebugger.impl.settings.XDebuggerSettingManagerImpl;
import consulo.disposer.Disposable;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10-Dec-16.
 */
public class XDebuggerGeneralConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    XDebuggerGeneralSettings settings = XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings();

    VerticalLayout layout = VerticalLayout.create();
    CheckBox focusAppOnBreakpointCheckbox = CheckBox.create(XDebuggerBundle.message("setting.focus.app.on.breakpoint.label"));
    layout.add(focusAppOnBreakpointCheckbox);
    propertyBuilder.add(focusAppOnBreakpointCheckbox, settings::isMayBringFrameToFrontOnBreakpoint, settings::setMayBringFrameToFrontOnBreakpoint);

    CheckBox showDebugWindowOnBreakpointCheckbox = CheckBox.create(XDebuggerBundle.message("settings.show.window.label"));
    layout.add(showDebugWindowOnBreakpointCheckbox);
    propertyBuilder.add(showDebugWindowOnBreakpointCheckbox, settings::isShowDebuggerOnBreakpoint, settings::setShowDebuggerOnBreakpoint);

    CheckBox hideWindowCheckBox = CheckBox.create(XDebuggerBundle.message("setting.hide.window.label"));
    layout.add(hideWindowCheckBox);
    propertyBuilder.add(hideWindowCheckBox, settings::isHideDebuggerOnProcessTermination, settings::setHideDebuggerOnProcessTermination);

    CheckBox scrollToCenterCheckbox = CheckBox.create(XDebuggerBundle.message("settings.scroll.to.center"));
    layout.add(scrollToCenterCheckbox);
    propertyBuilder.add(scrollToCenterCheckbox, settings::isScrollToCenter, settings::setScrollToCenter);
    return layout;
  }
}
