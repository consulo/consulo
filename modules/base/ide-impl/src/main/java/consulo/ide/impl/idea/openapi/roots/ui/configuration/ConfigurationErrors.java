/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.component.messagebus.MessageBus;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.util.lang.function.PairProcessor;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * User: spLeaner
 */
@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.NONE)
public interface ConfigurationErrors {
  void addError(@Nonnull ConfigurationError error);

  void removeError(@Nonnull ConfigurationError error);

  class Bus {
    public static void addError(@Nonnull final ConfigurationError error, @Nonnull final Project project) {
      _do(error, project, (configurationErrors, configurationError) -> {
        configurationErrors.addError(configurationError);
        return false;
      });
    }

    public static void removeError(@Nonnull final ConfigurationError error, @Nonnull final Project project) {
      _do(error, project, (configurationErrors, configurationError) -> {
        configurationErrors.removeError(configurationError);
        return false;
      });
    }

    public static void _do(@Nonnull final ConfigurationError error, @Nonnull final Project project, @Nonnull final PairProcessor<ConfigurationErrors, ConfigurationError> fun) {
      if (!project.isInitialized()) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> fun.process(project.getMessageBus().syncPublisher(ConfigurationErrors.class), error));

        return;
      }

      final MessageBus bus = project.getMessageBus();
      if (EventQueue.isDispatchThread()) {
        fun.process(bus.syncPublisher(ConfigurationErrors.class), error);
      }
      else {
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> fun.process(bus.syncPublisher(ConfigurationErrors.class), error));
      }
    }
  }
}
