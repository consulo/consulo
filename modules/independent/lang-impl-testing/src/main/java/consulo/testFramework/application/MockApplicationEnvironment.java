/*
 * Copyright 2013-2017 consulo.io
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
package consulo.testFramework.application;

import com.intellij.application.options.PathMacrosImpl;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.ide.macro.Macro;
import com.intellij.ide.macro.MacroManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.openapi.application.PathMacros;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10-Sep-17
 */
public class MockApplicationEnvironment extends CoreApplicationEnvironment {
  public MockApplicationEnvironment(@Nonnull Disposable parentDisposable) {
    super(parentDisposable);

    registerApplicationExtensionPoint(Macro.EP_NAME, Macro.class);
    registerApplicationExtensionPoint(PathMacroFilter.EP_NAME, PathMacroFilter.class);


    registerApplicationComponent(PathMacros.class, new PathMacrosImpl());
    registerApplicationService(MacroManager.class, new MacroManager());
    registerApplicationService(UISettings.class, new UISettings());
  }
}
