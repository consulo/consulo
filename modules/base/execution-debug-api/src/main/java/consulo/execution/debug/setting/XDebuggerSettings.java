/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.execution.debug.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.extension.ExtensionPointName;
import consulo.configurable.Configurable;
import consulo.execution.debug.XDebuggerUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;

/**
 * Implement this class to provide settings page for debugger. Settings page will be placed under 'Debugger' node in the 'Settings' dialog.
 *
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class XDebuggerSettings<T> implements PersistentStateComponent<T> {
  public static final ExtensionPointName<XDebuggerSettings> EXTENSION_POINT = ExtensionPointName.create(XDebuggerSettings.class);

  private final String myId;

  protected XDebuggerSettings(final @Nonnull @NonNls String id) {
    myId = id;
  }

  protected static <S extends XDebuggerSettings<?>> S getInstance(Class<S> aClass) {
    return XDebuggerUtil.getInstance().getDebuggerSettings(aClass);
  }

  public final String getId() {
    return myId;
  }

  @Nonnull
  public Collection<? extends Configurable> createConfigurables(@Nonnull DebuggerSettingsCategory category) {
    return Collections.emptyList();
  }

  public void generalApplied(@Nonnull DebuggerSettingsCategory category) {
  }
}
