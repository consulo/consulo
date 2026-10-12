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
package consulo.sandboxPlugin.ide.debug;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class SandSuspendContext extends XSuspendContext {
    private final SandExecutionStack myMainStack;
    private final SandExecutionStack myWorkerStack;

    public SandSuspendContext(int step) {
        SandValue point = new SandValue("point", "Point", "{x=" + step + ", y=" + (step * 2) + "}", List.of(
            new SandValue("x", "int", String.valueOf(step)),
            new SandValue("y", "int", String.valueOf(step * 2))
        ));
        SandValue names = new SandValue("names", "String[3]", "size = 3", List.of(
            new SandValue("[0]", "String", "\"sand\""),
            new SandValue("[1]", "String", "\"dune\""),
            new SandValue("[2]", "String", "\"desert\"")
        ));

        myMainStack = new SandExecutionStack("main", ExecutionDebugIconGroup.threadThreadcurrent(), List.of(
            new SandStackFrame("calculate", 10 + step, List.of(
                new SandValue("step", "int", String.valueOf(step)),
                point,
                new SandValue("message", "String", "\"Hello from Sand\"")
            )),
            new SandStackFrame("process", 24, List.of(names, new SandValue("enabled", "boolean", "true"))),
            new SandStackFrame("main", 3, List.of(new SandValue("args", "String[0]", "size = 0")))
        ));

        myWorkerStack = new SandExecutionStack("worker", ExecutionDebugIconGroup.threadThreadsuspended(), List.of(
            new SandStackFrame("waitForTask", 42, List.of(new SandValue("timeout", "long", "1000"))),
            new SandStackFrame("run", 7, List.of())
        ));
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
        return myMainStack;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
        return new XExecutionStack[]{myMainStack, myWorkerStack};
    }
}
