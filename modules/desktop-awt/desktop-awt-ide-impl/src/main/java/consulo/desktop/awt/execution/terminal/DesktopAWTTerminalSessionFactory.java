/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.execution.terminal;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.terminal.TerminalSession;
import consulo.execution.terminal.TerminalSessionFactory;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 15/04/2023
 */
@ServiceImpl
@Singleton
public class DesktopAWTTerminalSessionFactory implements TerminalSessionFactory {
  @Nonnull
  @Override
  public TerminalSession createLocal(@Nonnull String connectorName,
                                     @Nonnull String workDirectory,
                                     @Nonnull Supplier<String> shellPathGetter) {
    return new LocalTerminalDirectRunner(connectorName, workDirectory, shellPathGetter);
  }
}
