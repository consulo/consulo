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
package consulo.execution.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.execution.configuration.RunProfile;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ProcessHandler;

import jakarta.annotation.Nonnull;
import java.util.EventListener;

/**
 * @author nik
 */
@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface ExecutionListener extends EventListener {

  default void processStartScheduled(@Nonnull String executorId, @Nonnull ExecutionEnvironment env) {
  }

  default void processStarting(@Nonnull String executorId, @Nonnull ExecutionEnvironment env) {
  }

  default void processNotStarted(@Nonnull String executorId, @Nonnull ExecutionEnvironment env) {
  }

  default void processStarted(@Nonnull String executorId, @Nonnull ExecutionEnvironment env, @Nonnull ProcessHandler handler) {
  }

  default void processTerminating(@Nonnull String executorId, @Nonnull ExecutionEnvironment env, @Nonnull ProcessHandler handler) {
    processTerminating(env.getRunProfile(), handler);
  }

  default void processTerminated(@Nonnull String executorId, @Nonnull ExecutionEnvironment env, @Nonnull ProcessHandler handler, int exitCode) {
    processTerminated(env.getRunProfile(), handler);
  }

  /**
   * @deprecated use {@link #processTerminating(String, ExecutionEnvironment, ProcessHandler)}
   */
  @Deprecated
  default void processTerminating(@Nonnull RunProfile runProfile, @Nonnull ProcessHandler handler) {
  }

  /**
   * @deprecated use {@link #processTerminated(String, ExecutionEnvironment, ProcessHandler, int)}
   */
  @Deprecated
  default void processTerminated(@Nonnull RunProfile runProfile, @Nonnull ProcessHandler handler) {
  }
}
