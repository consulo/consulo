/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.configuration;

import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.execution.action.Location;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.collection.SmartList;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author traff
 */
public class RunConfigurationExtensionsManager<U extends RunConfigurationBase, T extends RunConfigurationExtensionBase<U>> {
  private final Class<T> myExtensionClass;

  public RunConfigurationExtensionsManager(Class<T> extensionClass) {
    myExtensionClass = extensionClass;
  }

  public void validateConfiguration(U configuration, boolean isExecution) throws Exception {
    // only for enabled extensions
    for (T extension : getEnabledExtensions(configuration, null)) {
      extension.validateConfiguration(configuration, isExecution);
    }
  }

  public void extendCreatedConfiguration(U configuration, Location location) {
    for (T extension : getApplicableExtensions(configuration)) {
      extension.extendCreatedConfiguration(configuration, location);
    }
  }

  public void extendTemplateConfiguration(U configuration) {
    for (T extension : getApplicableExtensions(configuration)) {
      extension.extendTemplateConfiguration(configuration);
    }
  }

  public void patchCommandLine(U configuration, RunnerSettings runnerSettings, GeneralCommandLine cmdLine, String runnerId)
          throws ExecutionException {
    // only for enabled extensions
    for (T extension : getEnabledExtensions(configuration, runnerSettings)) {
      extension.patchCommandLine(configuration, runnerSettings, cmdLine, runnerId);
    }
  }

  public void attachExtensionsToProcess(U configuration, ProcessHandler handler, RunnerSettings runnerSettings) {
    // only for enabled extensions
    for (T extension : getEnabledExtensions(configuration, runnerSettings)) {
      extension.attachToProcess(configuration, handler, runnerSettings);
    }
  }

  protected void processApplicableExtensions(U configuration, Consumer<T> acceptor) {
      ExtensionPoint<T> point = Application.get().getExtensionPoint(myExtensionClass);

      point.forEachExtensionSafe(t -> {
          if (t.isApplicableFor(configuration)) {
              acceptor.accept(t);
          }
      });
  }

  
  protected List<T> getApplicableExtensions(U configuration) {
    List<T> extensions = new SmartList<>();
    for (T extension : Application.get().getExtensionList(myExtensionClass)) {
      if (extension.isApplicableFor(configuration)) {
        extensions.add(extension);
      }
    }
    return extensions;
  }

  
  protected List<T> getEnabledExtensions(U configuration, @Nullable RunnerSettings runnerSettings) {
    List<T> extensions = new SmartList<>();
    for (T extension : Application.get().getExtensionList(myExtensionClass)) {
      if (extension.isApplicableFor(configuration) && extension.isEnabledFor(configuration, runnerSettings)) {
        extensions.add(extension);
      }
    }
    return extensions;
  }
}
