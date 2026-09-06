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
package consulo.it.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.index.impl.internal.hints.FileTypeIndexingHint;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.IndexedFile;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contentless index used by the indexing integration tests to measure how often the indexing framework consults an
 * input filter. It accepts nothing but {@link HeadlessCountedFileType}, and answers through a file type hint, so a
 * framework which honours hints must decide per file type instead of per file.
 * <p>
 * {@link #fileTypeCalls()} counts the hint (per file type) invocations, {@link #perFileCalls()} counts every
 * invocation that was handed a concrete file.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessCountingIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> NAME = ID.create("consulo.it.counting");

    private static final Map<FileType, AtomicInteger> ourFileTypeCalls = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> ourPerFileCalls = new ConcurrentHashMap<>();

    private final CountingInputFilter myInputFilter = new CountingInputFilter();

    public static void reset() {
        ourFileTypeCalls.clear();
        ourPerFileCalls.clear();
    }

    public static int fileTypeCalls() {
        int total = 0;
        for (AtomicInteger count : ourFileTypeCalls.values()) {
            total += count.get();
        }
        return total;
    }

    public static int fileTypeCalls(FileType fileType) {
        AtomicInteger count = ourFileTypeCalls.get(fileType);
        return count == null ? 0 : count.get();
    }

    public static int perFileCalls() {
        int total = 0;
        for (AtomicInteger count : ourPerFileCalls.values()) {
            total += count.get();
        }
        return total;
    }

    public static int perFileCalls(String fileName) {
        AtomicInteger count = ourPerFileCalls.get(fileName);
        return count == null ? 0 : count.get();
    }

    public static Set<String> perFileNames() {
        return Set.copyOf(ourPerFileCalls.keySet());
    }

    private static final class CountingInputFilter implements FileBasedIndex.InputFilter, FileTypeIndexingHint {
        @Override
        public ThreeState acceptsFileTypeFastPath(FileType fileType) {
            ourFileTypeCalls.computeIfAbsent(fileType, ignored -> new AtomicInteger()).incrementAndGet();
            return ThreeState.fromBoolean(fileType == HeadlessCountedFileType.INSTANCE);
        }

        @Override
        public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
            ourPerFileCalls.computeIfAbsent(file.getFileName(), ignored -> new AtomicInteger()).incrementAndGet();
            return file.getFileType() == HeadlessCountedFileType.INSTANCE;
        }

        @Override
        public boolean acceptInput(@Nullable Project project, VirtualFile file) {
            ourPerFileCalls.computeIfAbsent(file.getName(), ignored -> new AtomicInteger()).incrementAndGet();
            return !file.isDirectory() && file.getFileType() == HeadlessCountedFileType.INSTANCE;
        }
    }

    @Override
    public ID<String, Void> getName() {
        return NAME;
    }

    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> Collections.singletonMap(inputData.getFileName(), null);
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return myInputFilter;
    }

    @Override
    public boolean dependsOnFileContent() {
        return false;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
