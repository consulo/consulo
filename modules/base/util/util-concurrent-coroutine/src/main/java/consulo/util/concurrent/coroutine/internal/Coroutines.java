//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine.internal;

import consulo.util.concurrent.coroutine.*;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Contains global {@link Coroutine} management functions and relation types. If
 * not stated otherwise the configuration relation types can be set on any level
 * from coroutine to context.
 *
 * @author eso
 */
public class Coroutines {
    /**
     * Configuration: A handler for coroutine exceptions. The main purpose of
     * this is to process exception stacktraces when they occur. All coroutine
     * exceptions will also be available from the finished scope.The default
     * value prints the stacktrace of a failed coroutine execution to the
     * console.
     */
    public static final Key<Consumer<Throwable>> EXCEPTION_HANDLER = Key.create("Coroutines#EXCEPTION_HANDLER");

    /**
     * Configuration: coroutine event listeners that will be invoked when
     * coroutines are started or finished.
     */
    public static final Key<List<Consumer<CoroutineEvent>>>
        COROUTINE_LISTENERS = Key.create("Coroutines#COROUTINE_LISTENERS");

    /**
     * Configuration: a single listener for coroutine suspensions. This listener
     * will be invoked with the suspension and a boolean value after a coroutine
     * has been suspended (TRUE) or before it is resumed (FALSE). This relation
     * is intended mainly for debugging purposes.
     */
    public static final Key<BiConsumer<Suspension<?>, Boolean>>
        COROUTINE_SUSPENSION_LISTENER = Key.create("Coroutines#COROUTINE_SUSPENSION_LISTENER");

    /**
     * Configuration: a single listener for coroutine step executions. This
     * listener will be invoked with the step and continuation just before a
     * step is executed. This relation is intended mainly for debugging
     * purposes.
     */
    public static final Key<BiConsumer<CoroutineStep<?, ?>, Continuation<?>>>
        COROUTINE_STEP_LISTENER = Key.create("Coroutines#COROUTINE_STEP_LISTENER");

    /**
     * Private, only static use.
     */
    private Coroutines() {
    }

    /**
     * Iterates over all relations in the given state object that are annotated
     * with {@link MetaTypes#MANAGED} and closes them if they implement the
     * {@link AutoCloseable} interface. This is invoked automatically
     *
     * @param state        The state relatable to check for managed resources
     * @param errorHandler A consumer for exceptions that occur when closing a
     *                     resource
     */
    public static void closeManagedResources(UserDataHolder state, Consumer<Throwable> errorHandler) {
        AutoClosableRegister data = state.getUserData(AutoClosableRegister.KEY);
        if (data == null) {
            return;
        }

        data.closeAll(errorHandler);
    }
}
