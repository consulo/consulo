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
package consulo.configurable;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import javax.swing.*;

@Deprecated
@DeprecationInfo("Use consulo.ide.impl.options.SimpleConfigurable")
public interface IdeaConfigurableUi<S> {
  void reset(@Nonnull S settings);

  boolean isModified(@Nonnull S settings);

  void apply(@Nonnull S settings) throws ConfigurationException;

  @Nonnull
  JComponent getComponent(Disposable disposable);
}