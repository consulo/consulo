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
package consulo.compiler.impl.internal;

import consulo.compiler.CompilerMessageCategory;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2026-02-24
 */
public class CompileCounters {
    public static final Key<CompileCounters> KEY = Key.of(CompileCounters.class);

    private Map<CompilerMessageCategory, AtomicInteger> myCounters = new ConcurrentHashMap<>();

    private Set<VirtualFile> myErrorFiles = ConcurrentHashMap.newKeySet();

    public void inc(CompilerMessageCategory category) {
        myCounters.computeIfAbsent(category, category1 -> new AtomicInteger()).incrementAndGet();
    }

    public int get(CompilerMessageCategory category) {
        AtomicInteger val = myCounters.get(category);
        return val == null ? 0 : val.get();
    }

    public void addErrorFile(VirtualFile file) {
        myErrorFiles.add(file);
    }

    public Set<VirtualFile> getErrorFiles() {
        return myErrorFiles;
    }
}
