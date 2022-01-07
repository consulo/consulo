/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.xdebugger.breakpoints;

import consulo.disposer.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Use {@link com.intellij.xdebugger.XDebuggerManager#getBreakpointManager()} to obtain instance of this service
 *
 * @author nik
 */
public interface XBreakpointManager {
  @Nonnull
  <T extends XBreakpointProperties> XBreakpoint<T> addBreakpoint(XBreakpointType<XBreakpoint<T>, T> type, @Nullable T properties);

  @Nonnull
  <T extends XBreakpointProperties> XLineBreakpoint<T> addLineBreakpoint(XLineBreakpointType<T> type,
                                                                         @Nonnull String fileUrl,
                                                                         int line,
                                                                         @Nullable T properties,
                                                                         boolean temporary);

  @Nonnull
  <T extends XBreakpointProperties> XLineBreakpoint<T> addLineBreakpoint(XLineBreakpointType<T> type,
                                                                         @Nonnull String fileUrl,
                                                                         int line,
                                                                         @Nullable T properties);

  void removeBreakpoint(@Nonnull XBreakpoint<?> breakpoint);

  @Nonnull
  XBreakpoint<?>[] getAllBreakpoints();

  @Nonnull
  <B extends XBreakpoint<?>> Collection<? extends B> getBreakpoints(@Nonnull XBreakpointType<B, ?> type);

  @Nonnull
  <B extends XBreakpoint<?>> Collection<? extends B> getBreakpoints(@Nonnull Class<? extends XBreakpointType<B, ?>> typeClass);

  @Nullable
  <P extends XBreakpointProperties> XLineBreakpoint<P> findBreakpointAtLine(@Nonnull XLineBreakpointType<P> type,
                                                                            @Nonnull VirtualFile file,
                                                                            int line);

  boolean isDefaultBreakpoint(@Nonnull XBreakpoint<?> breakpoint);

  @Nullable
  <B extends XBreakpoint<?>> B getDefaultBreakpoint(@Nonnull XBreakpointType<B, ?> type);

  <B extends XBreakpoint<P>, P extends XBreakpointProperties> void addBreakpointListener(@Nonnull XBreakpointType<B, P> type,
                                                                                         @Nonnull XBreakpointListener<B> listener);

  <B extends XBreakpoint<P>, P extends XBreakpointProperties> void removeBreakpointListener(@Nonnull XBreakpointType<B, P> type,
                                                                                            @Nonnull XBreakpointListener<B> listener);

  <B extends XBreakpoint<P>, P extends XBreakpointProperties> void addBreakpointListener(@Nonnull XBreakpointType<B, P> type,
                                                                                         @Nonnull XBreakpointListener<B> listener,
                                                                                         Disposable parentDisposable);

  void addBreakpointListener(@Nonnull XBreakpointListener<XBreakpoint<?>> listener);

  void removeBreakpointListener(@Nonnull XBreakpointListener<XBreakpoint<?>> listener);

  void addBreakpointListener(@Nonnull XBreakpointListener<XBreakpoint<?>> listener, @Nonnull Disposable parentDisposable);

  void updateBreakpointPresentation(@Nonnull XLineBreakpoint<?> breakpoint, @Nullable Image icon, @Nullable String errorMessage);
}
