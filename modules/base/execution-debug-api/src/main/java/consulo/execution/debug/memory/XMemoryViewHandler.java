/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.debug.memory;

import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.navigation.Navigatable;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Returned by {@link HasMemoryView#getMemoryViewHandler()} - the data of the Memory view.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public interface XMemoryViewHandler {
    /**
     * The types loaded by the suspended runtime with their instance counts.
     */
    CompletableFuture<XMemoryClasses> computeClasses(XSuspendContext context);

    /**
     * At most {@code limit} instances of {@code type}, shown as values - they can be expanded and evaluated like any value.
     */
    CompletableFuture<XValueChildrenList> computeInstances(TypeInfo type, int limit, XSuspendContext context);

    /**
     * The type of the instances of which {@code value} is one - for showing them from a variable.
     */
    default @Nullable TypeInfo getTypeInfo(XValue value) {
        return null;
    }

    /**
     * The declaration of {@code type}.
     */
    default @Nullable Navigatable getTypeSource(TypeInfo type) {
        return null;
    }

    /**
     * Counts the new instances of {@code type} since the previous suspension, for the diff of the view.
     */
    default @Nullable TrackerForNewInstancesBase createNewInstancesTracker(TypeInfo type) {
        return null;
    }

    /**
     * The view is shown or hidden - the trackers of the new instances are enabled only while it is shown.
     */
    default void setActive(boolean active) {
    }
}
