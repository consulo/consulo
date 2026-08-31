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
package consulo.compiler.impl.internal;

import consulo.compiler.ValidityState;
import consulo.compiler.ValidityStateFactory;
import consulo.compiler.util.CompilerUtil;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;

import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since 2003-04-01
 */
public class FileProcessingCompilerStateCache {
    private static final Logger LOG = Logger.getInstance(FileProcessingCompilerStateCache.class);
    private final StateCache<MyState> myCache;

    public FileProcessingCompilerStateCache(File storeDirectory, final ValidityStateFactory stateFactory) throws IOException {
        myCache = new StateCache<>(new File(storeDirectory, "timestamps")) {
            @Override
            public MyState read(DataInput stream) throws IOException {
                return new MyState(stream.readLong(), stateFactory.createValidityState(stream));
            }

            @Override
            public void write(MyState state, DataOutput out) throws IOException {
                out.writeLong(state.getTimestamp());
                ValidityState extState = state.getExtState();
                if (extState != null) {
                    extState.save(out);
                }
            }
        };
    }

    public void update(Path file, ValidityState extState) throws IOException {
        myCache.update(pathOf(file), new MyState(CompilerUtil.lastModified(file), extState));
    }

    public void remove(Path file) throws IOException {
        myCache.remove(pathOf(file));
    }

    public long getTimestamp(Path file) throws IOException {
        MyState state = myCache.getState(pathOf(file));
        return (state != null) ? state.getTimestamp() : -1L;
    }

    public ValidityState getExtState(Path file) throws IOException {
        MyState state = myCache.getState(pathOf(file));
        return (state != null) ? state.getExtState() : null;
    }

    public void force() {
        myCache.force();
    }

    public Collection<Path> getFiles() throws IOException {
        List<Path> result = new ArrayList<>();
        for (String path : myCache.getPaths()) {
            result.add(Path.of(path));
        }
        return result;
    }

    public boolean wipe() {
        return myCache.wipe();
    }

    public void close() {
        try {
            myCache.close();
        }
        catch (IOException ignored) {
            LOG.info(ignored);
        }
    }

    private static String pathOf(Path file) {
        return FileUtil.toSystemIndependentName(file.toString());
    }

    private static class MyState {
        private final long myTimestamp;
        private final ValidityState myExtState;

        public MyState(long timestamp, @Nullable ValidityState extState) {
            myTimestamp = timestamp;
            myExtState = extState;
        }

        public long getTimestamp() {
            return myTimestamp;
        }

        public @Nullable ValidityState getExtState() {
            return myExtState;
        }
    }

}
