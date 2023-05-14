/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.execution.process.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/11/2022
 */
@ServiceImpl
@Singleton
public class ProcessHandlerBuilderFactoryImpl implements ProcessHandlerBuilderFactory {
  @Nonnull
  @Override
  public ProcessHandlerBuilder newBuilder(@Nonnull GeneralCommandLine cmd) {
    return new ProcessHandlerBuilderImpl(cmd);
  }
}
