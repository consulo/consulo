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

package consulo.execution.debug.breakpoint;

import consulo.execution.debug.XSourcePosition;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a breakpoint. This interface isn't supposed to be implemented by a plugin. In order to support breakpoint provide
 * {@link XBreakpointType} or {@link XLineBreakpointType} implementation
 *
 * @author nik
 * @see XLineBreakpoint
 * @see XBreakpointManager
 */
public interface XBreakpoint<P extends XBreakpointProperties> extends UserDataHolder {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    @Nonnull
    XBreakpointType<?, P> getType();

    P getProperties();

    @Nullable
    XSourcePosition getSourcePosition();

    @Nullable
    Navigatable getNavigatable();

    @Nonnull
    SuspendPolicy getSuspendPolicy();

    void setSuspendPolicy(@Nonnull SuspendPolicy policy);

    boolean isLogMessage();

    void setLogMessage(boolean logMessage);

    /**
     * @deprecated use {@link #getLogExpressionObject()} instead
     */
    @Deprecated
    @Nullable
    String getLogExpression();

    void setLogExpression(@Nullable String expression);

    @Nullable
    XExpression getLogExpressionObject();

    void setLogExpressionObject(@Nullable XExpression expression);

    /**
     * @deprecated use {@link #getConditionExpression()} instead
     */
    @Deprecated
    @Nullable
    String getCondition();

    void setCondition(@Nullable String condition);

    @Nullable
    XExpression getConditionExpression();

    void setConditionExpression(@Nullable XExpression condition);

    long getTimeStamp();

    @Nonnull
    Project getProject();
}
