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

import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.memory.TypeInfo;
import consulo.execution.debug.memory.XMemoryClasses;
import consulo.execution.debug.memory.XMemoryViewHandler;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fake instance counts of the sand program - more points on each step.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public class SandMemoryViewHandler implements XMemoryViewHandler {
    private record SandTypeInfo(String name) implements TypeInfo {
        @Override
        public boolean canGetInstanceInfo() {
            return true;
        }
    }

    private static final SandTypeInfo POINT = new SandTypeInfo("Point");
    private static final SandTypeInfo STRING = new SandTypeInfo("String");
    private static final SandTypeInfo THREAD = new SandTypeInfo("Thread");

    @Override
    public CompletableFuture<XMemoryClasses> computeClasses(XSuspendContext context) {
        int step = context instanceof SandSuspendContext sandContext ? sandContext.getStep() : 0;
        Map<TypeInfo, Long> counts = new LinkedHashMap<>();
        counts.put(POINT, (long) step + 1);
        counts.put(STRING, 3L);
        counts.put(THREAD, 2L);
        return CompletableFuture.completedFuture(new XMemoryClasses(List.copyOf(counts.keySet()), counts));
    }

    @Override
    public CompletableFuture<XValueChildrenList> computeInstances(TypeInfo type, int limit, XSuspendContext context) {
        int step = context instanceof SandSuspendContext sandContext ? sandContext.getStep() : 0;
        XValueChildrenList children = new XValueChildrenList();
        if (type.equals(POINT)) {
            for (int i = 0; i <= step && i < limit; i++) {
                children.add(new SandValue("Point@" + i, "Point", "{x=" + i + ", y=" + (i * 2) + "}", List.of(
                    new SandValue("x", "int", String.valueOf(i)),
                    new SandValue("y", "int", String.valueOf(i * 2))
                )));
            }
        }
        else if (type.equals(STRING)) {
            children.add(new SandValue("String@0", "String", "\"sand\""));
            children.add(new SandValue("String@1", "String", "\"dune\""));
            children.add(new SandValue("String@2", "String", "\"desert\""));
        }
        else if (type.equals(THREAD)) {
            children.add(new SandValue("Thread@0", "Thread", "main"));
            children.add(new SandValue("Thread@1", "Thread", "worker"));
        }
        return CompletableFuture.completedFuture(children);
    }

    @Override
    public @Nullable TypeInfo getTypeInfo(XValue value) {
        if (value instanceof SandValue sandValue) {
            String type = sandValue.getType();
            for (SandTypeInfo typeInfo : List.of(POINT, STRING, THREAD)) {
                if (typeInfo.name().equals(type)) {
                    return typeInfo;
                }
            }
        }
        return null;
    }
}
