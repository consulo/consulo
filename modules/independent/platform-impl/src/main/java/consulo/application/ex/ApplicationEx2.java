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
package consulo.application.ex;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 12:05/12.08.13
 */
public interface ApplicationEx2 extends ApplicationEx {
  @Nonnull
  IApplicationStore getStateStore();

  void init();

  boolean isComponentsCreated();

  void executeSuspendingWriteAction(@Nullable Project project, @Nonnull String title, @Nonnull Runnable runnable);

  void executeByImpatientReader(@Nonnull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException;

  boolean runWriteActionWithProgressInDispatchThread(@Nonnull String title,
                                                     @Nullable Project project,
                                                     @Nullable JComponent parentComponent,
                                                     @Nullable String cancelText,
                                                     @Nonnull Consumer<ProgressIndicator> action);

  ComponentConfig[] getComponentConfigurations();
}
