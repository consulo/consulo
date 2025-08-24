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
package consulo.process.impl.internal.local;

import consulo.process.ExecutionException;
import consulo.process.KillableProcessHandler;
import consulo.process.cmd.GeneralCommandLine;

import jakarta.annotation.Nonnull;
import java.nio.charset.Charset;

/**
 * This process handler supports both ANSI coloring (see {@link ColoredProcessHandlerImpl})
 * and "soft-kill" feature (see {@link KillableProcessHandlerImpl}).
 */
public class KillableColoredProcessHandlerImpl extends ColoredProcessHandlerImpl implements KillableProcessHandler {
  public KillableColoredProcessHandlerImpl(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    this(commandLine, false);
  }

  /**
   * Starts a process with a {@link RunnerMediator mediator} when {@code withMediator} is set to {@code true} and the platform is Windows.
   */
  public KillableColoredProcessHandlerImpl(@Nonnull GeneralCommandLine commandLine, boolean withMediator) throws ExecutionException {
    super(mediate(commandLine, withMediator));
    setShouldKillProcessSoftly(true);
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public KillableColoredProcessHandlerImpl(@Nonnull Process process, /*@NotNull*/ String commandLine) {
    super(process, commandLine);
    setShouldKillProcessSoftly(true);
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public KillableColoredProcessHandlerImpl(@Nonnull Process process, /*@NotNull*/ String commandLine, @Nonnull Charset charset) {
    super(process, commandLine, charset);
    setShouldKillProcessSoftly(true);
  }

  /** @deprecated use {@link #KillableColoredProcessHandlerImpl(GeneralCommandLine, boolean)} (to be removed in IDEA 17) */
  @SuppressWarnings("unused")
  public static KillableColoredProcessHandlerImpl create(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return new KillableColoredProcessHandlerImpl(commandLine, true);
  }
}