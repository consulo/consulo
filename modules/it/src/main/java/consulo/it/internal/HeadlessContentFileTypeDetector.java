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
import consulo.util.io.ByteSequence;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeDetector;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Claims content starting with {@link #MARKER} as {@link HeadlessDetectedFileType}, and records what it was handed, so
 * a test can tell how often detection actually ran for a file and whether the content reached it as text.
 * <p>
 * It claims nothing without the marker, so every other file in the integration-test suite passes through it unchanged.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessContentFileTypeDetector implements FileTypeDetector {
    public static final String MARKER = "#!headless-detected";

    private static final Map<String, AtomicInteger> ourCalls = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> ourSawText = new ConcurrentHashMap<>();

    public static void reset() {
        ourCalls.clear();
        ourSawText.clear();
    }

    public static int detectCallCount(String fileName) {
        AtomicInteger calls = ourCalls.get(fileName);
        return calls == null ? 0 : calls.get();
    }

    public static @Nullable Boolean sawTextFor(String fileName) {
        return ourSawText.get(fileName);
    }

    @Override
    public @Nullable FileType detect(VirtualFile file, ByteSequence firstBytes, @Nullable CharSequence firstCharsIfText) {
        ourCalls.computeIfAbsent(file.getName(), name -> new AtomicInteger()).incrementAndGet();
        ourSawText.put(file.getName(), firstCharsIfText != null);

        if (firstCharsIfText == null) {
            return null;
        }
        return firstCharsIfText.length() >= MARKER.length() && firstCharsIfText.subSequence(0, MARKER.length()).toString().equals(MARKER)
            ? HeadlessDetectedFileType.INSTANCE
            : null;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
