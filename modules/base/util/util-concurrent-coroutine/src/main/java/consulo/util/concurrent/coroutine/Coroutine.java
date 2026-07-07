//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package consulo.util.concurrent.coroutine;

import consulo.util.concurrent.coroutine.internal.CoroutineImpl;
import consulo.util.concurrent.coroutine.internal.EmptyCoroutine;
import consulo.util.dataholder.CopyableUserDataHolder;
import consulo.util.dataholder.UserDataHolderEx;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * A pure Java implementation of cooperative concurrency, also known as
 * coroutines. Coroutines implement lightweight multiprocessing where all
 * running coroutines share the available execution threads, shortly suspending
 * execution between processing steps to give other coroutines the chance to
 * execute. Furthermore, coroutines can suspend their execution indefinitely
 * when waiting for other coroutines or external resources (e.g. data to be sent
 * or received), giving other code the chance to use the available threads.
 *
 * <p>
 * To achieve this functionality, the implementation makes use of several modern
 * Java features of which some are available starting with Java 8. The execution
 * of coroutine steps is done with {@link CompletableFuture} which in turn runs
 * the code by default in the
 * {@link ForkJoinPool#commonPool() common thread pool} defined in the
 * {@link ForkJoinPool} class. But if necessary the {@link Executor} used for
 * running coroutines can be changed.
 * </p>
 *
 * <p>
 * To provide a fluent and readable declaration the API makes extensive use of
 * the new functional programming features of Java and the (recommended) use of
 * static imports. By using these, especially by applying lambda expressions and
 * method references, a concise and easily understandable declaration of
 * coroutines is possible.
 * </p>
 *
 * <p>
 * From the outside a coroutine is a function that receives an input value,
 * processes it, and return an output value as the result of the execution. This
 * is similar to the {@link Function} interface introduced with Java 8. If
 * invoked {@link #runBlocking(CoroutineScope, Object)} blocking} it will
 * behave exactly like a standard function, blocking the current thread until
 * the processing has finished and the result value has been produced. But if
 * invoked {@link #runAsync(CoroutineScope, Object)} asynchronously} the
 * coroutine will be executed in parallel to the current thread, suspending it's
 * execution shortly between processing steps or even pausing until data is
 * available.
 * </p>
 *
 * <p>
 * Besides this Coroutine class there are a few other classes that play an
 * important role in the execution of coroutines:
 * </p>
 *
 * <ul>
 * <li>{@link CoroutineContext}: coroutines run in a certain context. If different
 * coroutines need to communicate during their execution they need to run in the
 * same context. The context can also be used to provide configuration for the
 * coroutines running in it.</li>
 * <li>{@link CoroutineScope}: coroutines can only be launched inside a scope.
 * The scope provides the runtime environment for an arbitrary number of
 * coroutines. It also serves as a defined entry and exit-point into coroutine
 * executions: a scope will block execution of the launching thread until all
 * coroutines in it have finished execution (either successfully, by
 * cancellation, or with an error). This follows the pattern of <i>structured
 * concurrency</i> which prevents "forgotten" executions running in the
 * background or terminating silently with an error. The scope also provides
 * configuration and shared state for the coroutines in it, overriding any
 * configuration in the context.</li>
 * <li>{@link Continuation}: Every execution of a coroutine is associated with a
 * continuation object that contains the current execution state. It is local to
 * a single execution and thus not shared with other running instances of the
 * same or other coroutines.</li>
 * <li>{@link CoroutineStep}: The base for all steps that can be executed in a
 * coroutine. Like the coroutine itself it is basically a function that receives
 * an input value and produces a result. When executing asynchronously a step
 * implementation can suspend it's execution by stopping the background
 * execution completely until the condition that caused the suspension no longer
 * exists (e.g. a resource becomes available). Several standard steps are
 * defined in the 'step' sub-package but the base class can also be extended to
 * create new kinds of coroutine steps.</li>
 * <li>{@link Suspension}: If a step signals to suspend it's asynchronous
 * execution a suspension object is created. The suspension contains the
 * suspended execution state, mainly by referencing the associated
 * {@link Continuation}. When the suspending condition is resolved the
 * suspension can be used to resume the asynchronous execution of the
 * coroutine.</li>
 * <li>{@link Channel}: The previous classes are always involved when building
 * and executing coroutines. Channels are an optional but important feature
 * because they allow multiple coroutines to communicate without blocking their
 * threads. A coroutine will automatically suspend it's execution if a channel
 * that is used for receiving or sending has no data or capacity available. As
 * soon as the channel becomes available again the coroutine will continue to
 * run. Channels are managed by either the scope or the context the coroutine
 * runs in. If coroutines in different scopes need to communicate through a
 * channel their scopes need to have the same context and the channel must be
 * created in the context (not in the scope which is the default).</li>
 * </ul>
 *
 * <p>
 * A coroutine can either be created by invoking the public
 * {@link #Coroutine(CoroutineStep) constructor} with the first
 * {@link CoroutineStep} to execute or by invoking the factory method
 * {@link #first(CoroutineStep)}. The latter allows to declare a coroutine in a
 * fluent way with better readability. There is a slight limitation caused by
 * the generic type system of Java: if the result of
 * {@link #first(CoroutineStep) first()} is assigned to a variable with a
 * specific input type it may be necessary to declare the input type explicitly
 * in the parameters of a lambda expression. For example, the following example
 * (using a static import of first() may cause a compiler error (depending on
 * the Java version used):
 * </p>
 * <code>Coroutine&lt;String, String&gt; toUpper = first(apply(s -&gt;
 * s.toUpperCase()));</code>
 *
 * <p>
 * To make the code compile, the type of the lambda argument needs to be
 * declared explicitly:
 * </p>
 * <code>Coroutine&lt;String, String&gt; toUpper = first(apply((String s) -&gt;
 * s.toUpperCase()));</code>
 *
 * <p>
 * After a coroutine has been created it can be extended with additional steps
 * by invoking the instance method {@link #then(CoroutineStep)} on it. This
 * method takes the next step to be executed and <b>returns a new coroutine
 * instance</b>. This means that <b>coroutines are immutable</b>, i.e. they
 * cannot be modified after they have been created. Only new coroutines can be
 * created from them. This allows to declare coroutine templates which can be
 * extended by adding additional processing steps without the risk of changing
 * the original. Thus the {@link #first(CoroutineStep) first()} and
 * {@link #then(CoroutineStep) then()} methods implement a builder pattern where
 * each invocation creates a new coroutine instance.
 * </p>
 *
 * <p>
 * The immutability of coroutines initially only covers their code (i.e. the
 * step sequence). The Coroutine class also extends {@link RelatedObject} and
 * therefore allows to set arbitrary relations on it which can be used to
 * configure step behavior or set default data, for example. To also make the
 * relations of an instance immutable (basically sealing the coroutine template)
 * just set the flag relation {@link MetaTypes#IMMUTABLE IMMUTABLE} on it which
 * will prevent the further modification of relations. This flag will not be
 * copied on started coroutines (which are always a copy of the original
 * coroutine). This allows to modify the instances of immutable coroutine
 * templates.
 * </p>
 *
 * <p>
 * When a coroutine is executed a copy of it is created and then associated with
 * a new {@link Continuation} instance. That prevents running code to modify the
 * coroutine (template) it has been started from but gives it access to any of
 * it's relations. The actual runtime state is stored in the continuation object
 * and may be modified freely by the coroutine code. It is recommended that
 * coroutine steps use the continuation if they need to share data with other
 * steps besides the standard input and output parameters.
 * </p>
 *
 * <p>
 * Accessing state in the continuation can be done without further
 * synchronization because the steps in a coroutine are executed sequentially
 * and never concurrently (unless stated otherwise by some special step
 * implementations). But if steps access variables outside the continuation they
 * must apply the same caution like other multi-threaded code in Java because
 * access to such resource may (and will probably) need synchronization to avoid
 * concurrency issues (which are notoriously difficult to debug). This includes
 * {@link CoroutineScope} and {@link CoroutineContext} which are shared by
 * multiple running coroutine instances. There are no synchronization mechanisms
 * for access to the relations in these objects. If a step implementation wants
 * to modify relations in the scope or context it must perform the necessary
 * synchronization itself.
 * </p>
 *
 * <p>
 * <b>Attention:</b> Any synchronization between coroutines should be applied
 * with caution. Coroutines implement <b>cooperative multi-tasking</b> by
 * executing their steps in a thread pool. These pools assume that the code
 * running in a pool thread only occupies it as long as needed for the
 * processing. Blocking such a thread in some way (like waiting for a lock,
 * accessing a synchronized resource, or just sleeping) counteracts the purpose
 * of the thread pool in particular and of cooperative multi-tasking in general.
 * </p>
 *
 * <p>
 * Therefore it is strongly advised to not perform "classical" synchronizations
 * from coroutine steps. Instead it should be checked whether it is possible to
 * implement this in a cooperative way by suspending the coroutine execution
 * while waiting for a resource. This can be achieved by using an asynchronous
 * API for accessing a resource, like that provided by the java.nio package (see
 * the sub-package 'step.nio' for examples).
 * </p>
 *
 * @author eso
 */
public interface Coroutine<I extends @Nullable Object, O extends @Nullable Object> extends UserDataHolderEx, CopyableUserDataHolder {
    /**
     * A factory method that creates a new coroutine which starts with the
     * execution of a certain code function.
     *
     * @param step fCode The function containing the starting code of the
     *             coroutine
     * @return A new coroutine instance
     */
    static <I extends @Nullable Object, O extends @Nullable Object> Coroutine<I, O> first(CoroutineStep<I, O> step) {
        return new CoroutineImpl<>(step);
    }

    /**
     * A variant of {@link #first(CoroutineStep)} that also sets an explicit
     * step name. Naming steps can help debugging coroutines.
     *
     * @param stepName A name that identifies this step in this coroutine
     * @param step     The step to execute
     * @return The new coroutine
     */
    static <I extends @Nullable Object, O extends @Nullable Object> Coroutine<I, O> first(String stepName, CoroutineStep<I, O> step) {
        return first(step.withName(stepName));
    }

    /**
     * Returns an empty coroutine placeholder that performs no processing. Can be
     * used as a placeholder where a coroutine is required but no work has to be
     * performed. Running it does nothing and chaining additional steps with
     * {@link #then(CoroutineStep)} is not supported.
     *
     * @return The empty coroutine instance
     */
    static <I extends @Nullable Object, O extends @Nullable Object> Coroutine<I, O> empty() {
        return EmptyCoroutine.instance();
    }

    /**
     * Runs a copy of this coroutine asynchronously in a certain scope. This
     * method returns a {@link Continuation} that contains the execution state
     * and provides access to the coroutine result AFTER it finishes. Because
     * the execution happens asynchronously (i.e. in another thread) the
     * receiving code must always use the corresponding continuation methods to
     * check for completion before accessing the continuation state.
     *
     * <p>
     * Because a copy of this coroutine is executed, the continuation also
     * references the copy and not this instance. If the running code tries to
     * modify state of the coroutine it will only modify the copy, not the
     * original instance.
     * </p>
     *
     * <p>
     * If multiple coroutines need to communicate through
     * {@link Channel channels} they must run in the same context because
     * channels are managed by the context based on the channel ID.
     * </p>
     *
     * @param scope The scope to run in
     * @param input The input value
     * @return A {@link Continuation} that provides access to the execution
     * result
     */
    Continuation<O> runAsync(CoroutineScope scope, I input);

    /**
     * Runs a copy of this coroutine in a certain scope and blocks the current
     * thread until the coroutine finished. Returns a {@link Continuation} that
     * contains the execution state and provides access to the coroutine
     * result.
     *
     * <p>
     * Because a copy of this coroutine is executed, the continuation also
     * references the copy and not this instance. If the running code tries to
     * modify state of the coroutine it will only modify the copy, not the
     * original instance.
     * </p>
     *
     * @param scope The scope to run in
     * @param input The input value
     * @return A {@link Continuation} that provides access to the execution
     * result
     */
    Continuation<O> runBlocking(CoroutineScope scope, I input);

    /**
     * Returns a new coroutine that executes additional code after that of this
     * instance. This and the related methods implement a builder pattern for
     * coroutines. The initial coroutine is created with the first step, either
     * from the static factory methods like {@link #first(CoroutineStep)} or
     * with the public constructor. Invoking a builder method creates a new
     * coroutine with the combined code while the original coroutine remains
     * unchanged (or is discarded in the case of a builder chain).
     *
     * <p>
     * Each invocation of a builder method creates a coroutine suspension point
     * at which the execution will be interrupted to allow other code to run on
     * the current thread (e.g. another coroutine). Some steps may suspend the
     * execution until values from another coroutine or some external source
     * become available.
     * </p>
     *
     * <p>
     * An extended coroutine re-uses the original code of the coroutine it is
     * derived from. Therefore it is necessary to ensure that the code in shared
     * (base) coroutines contains no external dependencies that could change the
     * behavior of all derived coroutines if modified (unless desired, but
     * beware of side-effects). The best way to achieve this is by using
     * correctly defined closures when declaring step. If steps need to share
     * information during execution that can be achieved by setting relations on
     * the {@link Continuation} which is always local to the respective
     * execution.
     * </p>
     *
     * @param step The step to execute
     * @return The new coroutine
     */
    <T> Coroutine<I, T> then(CoroutineStep<O, T> step);

    /**
     * A variant of {@link #then(CoroutineStep)} that also sets an explicit step
     * name. Naming steps can help debugging coroutines.
     *
     * @param stepName A name that identifies this step in this coroutine
     * @param step     The step to execute
     * @return The new coroutine
     */
    default <T> Coroutine<I, T> then(String stepName, CoroutineStep<O, T> step) {
        return then(step.withName(stepName));
    }

    @Nullable String getName();

    Coroutine<I, O> withName(String name);
}
