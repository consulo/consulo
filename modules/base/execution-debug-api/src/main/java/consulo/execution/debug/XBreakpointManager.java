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

package consulo.execution.debug;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.disposer.Disposable;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.event.XBreakpointListener;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Use {@link XDebuggerManager#getBreakpointManager()} to obtain instance of this service
 *
 * @author nik
 */
public interface XBreakpointManager {
    
    @RequiredWriteAction
    <T extends XBreakpointProperties> XBreakpoint<T> addBreakpoint(XBreakpointType<XBreakpoint<T>, T> type, @Nullable T properties);

    
    <T extends XBreakpointProperties> XLineBreakpoint<T> addLineBreakpoint(XLineBreakpointType<T> type,
                                                                           String fileUrl,
                                                                           int line,
                                                                           @Nullable T properties,
                                                                           boolean temporary);

    
    <T extends XBreakpointProperties> XLineBreakpoint<T> addLineBreakpoint(XLineBreakpointType<T> type,
                                                                           String fileUrl,
                                                                           int line,
                                                                           @Nullable T properties);

    void removeBreakpoint(XBreakpoint<?> breakpoint);

    
    XBreakpoint<?>[] getAllBreakpoints();

    
    <B extends XBreakpoint<?>> Collection<? extends B> getBreakpoints(XBreakpointType<B, ?> type);

    
    <B extends XBreakpoint<?>> Collection<? extends B> getBreakpoints(Class<? extends XBreakpointType<B, ?>> typeClass);

    <P extends XBreakpointProperties> @Nullable XLineBreakpoint<P> findBreakpointAtLine(XLineBreakpointType<P> type,
                                                                              VirtualFile file,
                                                                              int line);

    boolean isDefaultBreakpoint(XBreakpoint<?> breakpoint);

    <B extends XBreakpoint<?>> @Nullable B getDefaultBreakpoint(XBreakpointType<B, ?> type);

    <B extends XBreakpoint<P>, P extends XBreakpointProperties> void addBreakpointListener(XBreakpointType<B, P> type,
                                                                                           XBreakpointListener<B> listener);

    <B extends XBreakpoint<P>, P extends XBreakpointProperties> void removeBreakpointListener(XBreakpointType<B, P> type,
                                                                                              XBreakpointListener<B> listener);

    <B extends XBreakpoint<P>, P extends XBreakpointProperties> void addBreakpointListener(XBreakpointType<B, P> type,
                                                                                           XBreakpointListener<B> listener,
                                                                                           Disposable parentDisposable);

    @Deprecated
    @DeprecationInfo("Use MessageBus for listeners")
    void addBreakpointListener(XBreakpointListener<XBreakpoint<?>> listener);

    @Deprecated
    @DeprecationInfo("Use MessageBus for listeners")
    void removeBreakpointListener(XBreakpointListener<XBreakpoint<?>> listener);

    @Deprecated
    @DeprecationInfo("Use MessageBus for listeners")
    void addBreakpointListener(XBreakpointListener<XBreakpoint<?>> listener, Disposable parentDisposable);

    void updateBreakpointPresentation(XLineBreakpoint<?> breakpoint, @Nullable Image icon, @Nullable String errorMessage);

    
    Project getProject();

    
    XDependentBreakpointManager getDependentBreakpointManager();

    void editBreakpoint(Project project, Editor editor, Object breakpoint, GutterIconRenderer breakpointGutterRenderer);
}
