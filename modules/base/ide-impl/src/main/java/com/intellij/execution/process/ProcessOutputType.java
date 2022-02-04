// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.process;

import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a stream (stdout/stderr/system) output type. It can be a base output type or a colored output type.
 * Base stdout/stderr output types are constants: {@link ProcessOutputTypes#STDOUT}, {@link ProcessOutputTypes#STDERR} and
 * {@link ProcessOutputTypes#SYSTEM}.<br/>
 * A colored stdout/stderr output type corresponds to an unique ANSI color attributes info passed as
 * {@code name} constructor parameter, and base stream output type (stdout/stderr) - {@code streamType} parameter.
 * <p/>
 * Use {@link com.intellij.execution.ui.ConsoleViewContentType#getConsoleViewType} to get TextAttributes for an instance
 * of this class.
 * <p/>
 *
 * @see com.intellij.execution.process.ColoredOutputTypeRegistry
 * @see com.intellij.execution.ui.ConsoleViewContentType#registerNewConsoleViewType
 */
public class ProcessOutputType extends Key<Object> {
  public static final ProcessOutputType SYSTEM = new ProcessOutputType("system");

  /**
   * Base type for data from process standard output stream.<p>
   * Colored texts from stdout are represented by different instances of {@link ProcessOutputType} having
   * {@code ProcessOutputType.STDOUT} base type:
   * <pre>{@code coloredStdoutType.getBaseOutputType() == ProcessOutputType.STDOUT}</pre>
   * <p/>
   * Thus to check whether a process output type is from stdout:
   * <pre>{@code ProcessOutputType.isStdout(key)}</pre>
   * instead of
   * <pre>{@code ProcessOutputType.STDOUT.equals(key)} or ProcessOutputType.STDOUT == key</pre>
   */
  public static final ProcessOutputType STDOUT = new ProcessOutputType("stdout");

  /**
   * Base type for data from process standard error stream.<p>
   * Colored texts from stderr are represented by different instances of {@link ProcessOutputType} having
   * {@code ProcessOutputType.STDERR} base type:
   * <pre>{@code coloredStderrType.getBaseOutputType() == ProcessOutputType.STDERR}</pre>
   * <p/>
   * Thus to check whether a process output type is from stderr:
   * <pre>{@code ProcessOutputType.isStderr(key)}</pre>
   * instead of
   * <pre>{@code ProcessOutputType.STDERR.equals(key)} or ProcessOutputType.STDERR == key</pre>
   */
  public static final ProcessOutputType STDERR = new ProcessOutputType("stderr");

  private final ProcessOutputType myStreamType;

  public ProcessOutputType(@Nonnull String name, @Nonnull ProcessOutputType streamType) {
    super(name);
    myStreamType = streamType.getBaseOutputType();
  }

  private ProcessOutputType(@Nonnull String name) {
    super(name);
    myStreamType = null;
  }

  @Nonnull
  public ProcessOutputType getBaseOutputType() {
    return myStreamType != null ? myStreamType : this;
  }

  public boolean isStdout() {
    return getBaseOutputType() == STDOUT;
  }

  public boolean isStderr() {
    return getBaseOutputType() == STDERR;
  }

  public static boolean isStderr(@Nonnull Key<?> key) {
    return key instanceof ProcessOutputType && ((ProcessOutputType)key).isStderr();
  }

  public static boolean isStdout(@Nonnull Key<?> key) {
    return key instanceof ProcessOutputType && ((ProcessOutputType)key).isStdout();
  }

  @Nonnull
  public static String getKeyNameForLogging(@Nonnull Key<?> key) {
    return key.toString().replace("\u001B", "ESC");
  }

  @Nullable
  public static ProcessOutputType tryCast(@Nonnull Key<?> key) {
    return key instanceof ProcessOutputType ? (ProcessOutputType)key : null;
  }
}
