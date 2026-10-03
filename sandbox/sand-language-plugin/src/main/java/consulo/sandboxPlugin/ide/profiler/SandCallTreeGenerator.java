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
package consulo.sandboxPlugin.ide.profiler;

import consulo.execution.profiler.DummyCallTreeBuilder;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.NativeThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class SandCallTreeGenerator {
    private static final long MIN_SAMPLES_PER_THREAD = 200;

    private static final SandCallStackElement MAIN = new SandCallStackElement("", "main");
    private static final SandCallStackElement APPLICATION_INIT = new SandCallStackElement("Application", "init");
    private static final SandCallStackElement APPLICATION_RUN = new SandCallStackElement("Application", "run");
    private static final SandCallStackElement CONFIG_LOAD = new SandCallStackElement("Config", "load");
    private static final SandCallStackElement PARSER_PARSE = new SandCallStackElement("Parser", "parse");
    private static final SandCallStackElement PARSER_PARSE_EXPRESSION = new SandCallStackElement("Parser", "parseExpression");
    private static final SandCallStackElement PARSER_PARSE_PRIMARY = new SandCallStackElement("Parser", "parsePrimary");
    private static final SandCallStackElement LEXER_ADVANCE = new SandCallStackElement("Lexer", "advance");
    private static final SandCallStackElement RESOLVER_RESOLVE = new SandCallStackElement("Resolver", "resolve");
    private static final SandCallStackElement SCOPE_LOOKUP = new SandCallStackElement("Scope", "lookup");
    private static final SandCallStackElement SYMBOL_TABLE_GET = new SandCallStackElement("SymbolTable", "get");
    private static final SandCallStackElement INTERPRETER_EVAL = new SandCallStackElement("Interpreter", "eval");
    private static final SandCallStackElement INTERPRETER_CALL = new SandCallStackElement("Interpreter", "call");
    private static final SandCallStackElement INTERPRETER_EVAL_BINARY = new SandCallStackElement("Interpreter", "evalBinary");
    private static final SandCallStackElement BUILTINS_PRINT = new SandCallStackElement("Builtins", "print");
    private static final SandCallStackElement BUILTINS_CONCAT = new SandCallStackElement("Builtins", "concat");
    private static final SandCallStackElement WORKER_LOOP = new SandCallStackElement("Worker", "loop");
    private static final SandCallStackElement TASK_QUEUE_TAKE = new SandCallStackElement("TaskQueue", "take");
    private static final SandCallStackElement TASK_EXECUTE = new SandCallStackElement("Task", "execute");
    private static final SandCallStackElement CACHE_PUT = new SandCallStackElement("Cache", "put");
    private static final SandCallStackElement CACHE_RESIZE = new SandCallStackElement("Cache", "resize");

    private static final List<WeightedPath> MAIN_PATHS = List.of(
        path(2, MAIN, APPLICATION_RUN),
        path(3, MAIN, APPLICATION_INIT, CONFIG_LOAD),
        path(12, MAIN, APPLICATION_RUN, PARSER_PARSE, LEXER_ADVANCE),
        path(10, MAIN, APPLICATION_RUN, PARSER_PARSE, PARSER_PARSE_EXPRESSION, PARSER_PARSE_PRIMARY),
        path(6, MAIN, APPLICATION_RUN, PARSER_PARSE, PARSER_PARSE_EXPRESSION, LEXER_ADVANCE),
        path(9, MAIN, APPLICATION_RUN, RESOLVER_RESOLVE, SCOPE_LOOKUP),
        path(7, MAIN, APPLICATION_RUN, RESOLVER_RESOLVE, SCOPE_LOOKUP, SYMBOL_TABLE_GET),
        path(8, MAIN, APPLICATION_RUN, INTERPRETER_EVAL, INTERPRETER_CALL, BUILTINS_PRINT),
        path(5, MAIN, APPLICATION_RUN, INTERPRETER_EVAL, INTERPRETER_CALL, INTERPRETER_EVAL, INTERPRETER_CALL, BUILTINS_CONCAT),
        path(6, MAIN, APPLICATION_RUN, INTERPRETER_EVAL, INTERPRETER_EVAL_BINARY)
    );

    private static final List<WeightedPath> WORKER_PATHS = List.of(
        path(10, WORKER_LOOP, TASK_QUEUE_TAKE),
        path(6, WORKER_LOOP, TASK_EXECUTE, PARSER_PARSE, LEXER_ADVANCE),
        path(5, WORKER_LOOP, TASK_EXECUTE, INTERPRETER_EVAL, BUILTINS_PRINT),
        path(3, WORKER_LOOP, TASK_EXECUTE, CACHE_PUT, CACHE_RESIZE)
    );

    private SandCallTreeGenerator() {
    }

    public static List<NativeThread> createThreads(int count) {
        int threadCount = Math.max(1, count);
        List<NativeThread> threads = new ArrayList<>(threadCount);
        threads.add(new NativeThread(1, "main"));
        for (int i = 1; i < threadCount; i++) {
            threads.add(new NativeThread(i + 1, "sand-worker-" + i));
        }
        return List.copyOf(threads);
    }

    public static DummyCallTreeBuilder<BaseCallStackElement> generate(
        List<NativeThread> threads,
        long durationMs,
        int samplingIntervalMs,
        Random random
    ) {
        DummyCallTreeBuilder<BaseCallStackElement> builder = new DummyCallTreeBuilder<>();
        long samples = Math.max(MIN_SAMPLES_PER_THREAD, durationMs / Math.max(1, samplingIntervalMs));
        for (int i = 0; i < threads.size(); i++) {
            addSamples(builder, threads.get(i), i == 0 ? MAIN_PATHS : WORKER_PATHS, samples, random);
        }
        return builder;
    }

    private static void addSamples(
        DummyCallTreeBuilder<BaseCallStackElement> builder,
        NativeThread thread,
        List<WeightedPath> paths,
        long samples,
        Random random
    ) {
        int totalWeight = 0;
        for (WeightedPath path : paths) {
            totalWeight += path.weight();
        }

        for (WeightedPath path : paths) {
            double share = (double) path.weight() / totalWeight;
            long value = Math.round(samples * share * (0.6 + 0.8 * random.nextDouble()));
            if (value > 0) {
                builder.addStack(thread, path.frames(), value);
            }
        }
    }

    private static WeightedPath path(int weight, SandCallStackElement... framesRootFirst) {
        return new WeightedPath(weight, List.of(framesRootFirst));
    }

    private record WeightedPath(int weight, List<SandCallStackElement> frames) {
    }
}
