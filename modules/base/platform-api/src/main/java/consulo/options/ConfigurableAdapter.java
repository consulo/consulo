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
package consulo.options;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import consulo.annotations.RequiredDispatchThread;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 06.01.2016
 */
public class ConfigurableAdapter implements Configurable {
  @Nls
  @Override
  public String getDisplayName() {
    return null;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @RequiredDispatchThread
  @Nullable
  @Override
  public JComponent createComponent() {
    return null;
  }

  @RequiredDispatchThread
  @Override
  public boolean isModified() {
    return false;
  }

  @RequiredDispatchThread
  @Override
  public void apply() throws ConfigurationException {
  }

  @RequiredDispatchThread
  @Override
  public void reset() {
  }
}
