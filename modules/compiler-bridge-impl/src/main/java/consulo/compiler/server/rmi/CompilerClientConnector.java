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
package consulo.compiler.server.rmi;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 15:50/19.08.13
 */
public abstract class CompilerClientConnector {
  @NotNull
  public static CompilerClientConnector getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, CompilerClientConnector.class);
  }

  public abstract void setClientConnection(@Nullable CompilerClientInterface clientConnection);

  public abstract void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum);

  public void showOrHide(boolean hide) {
    // ignored
  }

  public void selectFirstMessage() {
    // ignored
  }
}
