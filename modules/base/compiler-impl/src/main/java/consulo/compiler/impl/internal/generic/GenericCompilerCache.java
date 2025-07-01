/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.compiler.impl.internal.generic;

import consulo.compiler.generic.GenericCompiler;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.PersistentEnumerator;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.DataExternalizer;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * @author nik
 */
public class GenericCompilerCache<Key, SourceState, OutputState> {
    private static final Logger LOG = Logger.getInstance(GenericCompilerCache.class);
    private PersistentHashMap<KeyAndTargetData<Key>, PersistentStateData<SourceState, OutputState>> myPersistentMap;
    private File myCacheFile;
    private final GenericCompiler<Key, SourceState, OutputState> myCompiler;

    public GenericCompilerCache(GenericCompiler<Key, SourceState, OutputState> compiler, File compilerCacheDir) throws IOException {
        myCompiler = compiler;
        myCacheFile = new File(compilerCacheDir, "timestamps");
        createMap();
    }

    private void createMap() throws IOException {
        try {
            myPersistentMap = new PersistentHashMap<>(myCacheFile, new SourceItemDataDescriptor(myCompiler.getItemKeyDescriptor()),
                new PersistentStateDataExternalizer(myCompiler)
            );
        }
        catch (PersistentEnumerator.CorruptedException e) {
            FileUtil.delete(myCacheFile);
            throw e;
        }
    }

    private KeyAndTargetData<Key> getKeyAndTargetData(Key key, int target) {
        return new KeyAndTargetData<>(target, key);
    }

    public void wipe() throws IOException {
        try {
            myPersistentMap.close();
        }
        catch (IOException ignored) {
        }
        PersistentHashMap.deleteFilesStartingWith(myCacheFile);
        createMap();
    }

    public void close() {
        try {
            myPersistentMap.close();
        }
        catch (IOException e) {
            LOG.info(e);
        }
    }

    public void remove(int targetId, Key key) throws IOException {
        myPersistentMap.remove(getKeyAndTargetData(key, targetId));
    }

    public PersistentStateData<SourceState, OutputState> getState(int targetId, Key key) throws IOException {
        return myPersistentMap.get(getKeyAndTargetData(key, targetId));
    }

    public void processSources(int targetId, Predicate<Key> processor) throws IOException {
        myPersistentMap.processKeysWithExistingMapping(
            data -> targetId != data.myTarget || processor.test(data.myKey)
        );
    }

    public void putState(
        int targetId,
        @Nonnull Key key,
        @Nonnull SourceState sourceState,
        @Nonnull OutputState outputState
    ) throws IOException {
        myPersistentMap.put(getKeyAndTargetData(key, targetId), new PersistentStateData<>(sourceState, outputState));
    }


    private static class KeyAndTargetData<Key> {
        public final int myTarget;
        public final Key myKey;

        private KeyAndTargetData(int target, Key key) {
            myTarget = target;
            myKey = key;
        }
    }

    public static class PersistentStateData<SourceState, OutputState> {
        public final SourceState mySourceState;
        public final OutputState myOutputState;

        private PersistentStateData(@Nonnull SourceState sourceState, @Nonnull OutputState outputState) {
            mySourceState = sourceState;
            myOutputState = outputState;
        }
    }

    private class SourceItemDataDescriptor implements KeyDescriptor<KeyAndTargetData<Key>> {
        private final KeyDescriptor<Key> myKeyDescriptor;

        public SourceItemDataDescriptor(KeyDescriptor<Key> keyDescriptor) {
            myKeyDescriptor = keyDescriptor;
        }

        @Override
        public boolean equals(KeyAndTargetData<Key> val1, KeyAndTargetData<Key> val2) {
            return val1.myTarget == val2.myTarget;
        }

        @Override
        public int hashCode(KeyAndTargetData<Key> value) {
            return value.myTarget + 239 * myKeyDescriptor.hashCode(value.myKey);
        }

        @Override
        public void save(DataOutput out, KeyAndTargetData<Key> value) throws IOException {
            out.writeInt(value.myTarget);
            myKeyDescriptor.save(out, value.myKey);
        }


        @Override
        public KeyAndTargetData<Key> read(DataInput in) throws IOException {
            int target = in.readInt();
            Key item = myKeyDescriptor.read(in);
            return getKeyAndTargetData(item, target);
        }
    }

    private class PersistentStateDataExternalizer implements DataExternalizer<PersistentStateData<SourceState, OutputState>> {
        private DataExternalizer<SourceState> mySourceStateExternalizer;
        private DataExternalizer<OutputState> myOutputStateExternalizer;

        public PersistentStateDataExternalizer(GenericCompiler<Key, SourceState, OutputState> compiler) {
            mySourceStateExternalizer = compiler.getSourceStateExternalizer();
            myOutputStateExternalizer = compiler.getOutputStateExternalizer();
        }

        @Override
        public void save(DataOutput out, PersistentStateData<SourceState, OutputState> value) throws IOException {
            mySourceStateExternalizer.save(out, value.mySourceState);
            myOutputStateExternalizer.save(out, value.myOutputState);
        }

        @Override
        public PersistentStateData<SourceState, OutputState> read(DataInput in) throws IOException {
            SourceState sourceState = mySourceStateExternalizer.read(in);
            OutputState outputState = myOutputStateExternalizer.read(in);
            return new PersistentStateData<>(sourceState, outputState);
        }
    }
}
