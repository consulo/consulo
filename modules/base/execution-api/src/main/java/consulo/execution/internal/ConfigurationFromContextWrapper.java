/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.internal;

import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.action.RuntimeConfigurationProducer;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;

/**
 * Wraps a {@link RuntimeConfigurationProducer} in a {@link ConfigurationFromContext}.
 *
 * @author yole
 */
class ConfigurationFromContextWrapper extends ConfigurationFromContext {
  private final RuntimeConfigurationProducer myProducer;

  ConfigurationFromContextWrapper(RuntimeConfigurationProducer producer) {
    myProducer = producer;
  }

  @Override
  public void onFirstRun(ConfigurationContext context, Runnable startRunnable) {
    myProducer.perform(context, startRunnable);
  }

  @Nonnull
  @Override
  public RunnerAndConfigurationSettings getConfigurationSettings() {
    return myProducer.getConfiguration();
  }

  @Override
  public void setConfigurationSettings(RunnerAndConfigurationSettings configurationSettings) {
    myProducer.setConfiguration(configurationSettings);
  }

  @Nonnull
  @Override
  public PsiElement getSourceElement() {
    return myProducer.getSourceElement();
  }

  @Override
  public boolean isPreferredTo(ConfigurationFromContext other) {
    return other instanceof ConfigurationFromContextWrapper && myProducer.compareTo(((ConfigurationFromContextWrapper)other).myProducer) < 0;
  }
}
