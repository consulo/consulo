// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing;

import consulo.ide.impl.idea.index.IndexImporterFactory;
import consulo.index.io.IndexExtension;
import consulo.logging.Logger;
import consulo.ide.impl.idea.util.indexing.impl.InputData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class IndexImporterMappingIndex<Key, Value, Input> implements SnapshotInputMappingIndex<Key, Value, Input> {
    private static final Logger LOG = Logger.getInstance(IndexImporterMappingIndex.class);

    private final List<SnapshotInputMappingIndex<Key, Value, Input>> myImporters;

    @Nullable
    static <Key, Value, Input> SnapshotInputMappingIndex<Key, Value, Input> wrap(
        @Nullable SnapshotInputMappingIndex<Key, Value, Input> index,
        @Nonnull IndexExtension<Key, Value, Input> indexExtension
    ) {
        SnapshotInputMappingIndex<Key, Value, Input> indexImporterMappingIndex = createImportersWrapper(indexExtension);
        if (indexImporterMappingIndex == null) {
            return index;
        }
        if (index == null) {
            return indexImporterMappingIndex;
        }
        if (index instanceof UpdatableSnapshotInputMappingIndex) {
            UpdatableSnapshotInputMappingIndex<Key, Value, Input> updatableIndex =
                (UpdatableSnapshotInputMappingIndex<Key, Value, Input>)index;
            return new UpdatableSnapshotInputMappingIndex<>() {
                @Nonnull
                @Override
                public Map<Key, Value> readData(int hashId) throws IOException {
                    return updatableIndex.readData(hashId);
                }

                @Override
                public InputData<Key, Value> putData(@Nonnull Input content, @Nonnull InputData<Key, Value> data) throws IOException {
                    return updatableIndex.putData(content, data);
                }

                @Override
                public void flush() throws IOException {
                    updatableIndex.flush();
                }

                @Override
                public void clear() throws IOException {
                    updatableIndex.clear();
                }

                @Nullable
                @Override
                public InputData<Key, Value> readData(@Nonnull Input content) throws IOException {
                    InputData<Key, Value> existedData;
                    try {
                        existedData = index.readData(content);
                    }
                    catch (IOException e) {
                        LOG.error(e);
                        existedData = null;
                    }
                    if (existedData != null) {
                        return existedData;
                    }
                    InputData<Key, Value> importedData = indexImporterMappingIndex.readData(content);
                    if (importedData != null) {
                        return updatableIndex.putData(content, importedData);
                    }
                    return null;
                }

                @Override
                public void close() throws IOException {
                    try {
                        updatableIndex.close();
                    }
                    finally {
                        indexImporterMappingIndex.close();
                    }
                }
            };
        }
        return new SnapshotInputMappingIndex<>() {
            @Nullable
            @Override
            public InputData<Key, Value> readData(@Nonnull Input content) throws IOException {
                InputData<Key, Value> data = index.readData(content);
                if (data != null) {
                    return data;
                }
                return indexImporterMappingIndex.readData(content);
            }

            @Override
            public void close() throws IOException {
                try {
                    index.close();
                }
                finally {
                    indexImporterMappingIndex.close();
                }
            }
        };
    }

    @Nullable
    private static <Key, Value, Input> SnapshotInputMappingIndex<Key, Value, Input>
    createImportersWrapper(@Nonnull IndexExtension<Key, Value, Input> indexExtension) {
        List<SnapshotInputMappingIndex<Key, Value, Input>> importers;
        try {
            importers = IndexImporterFactory.EP_NAME.getExtensionList()
                .stream()
                .map(f -> f.createImporter(indexExtension))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        catch (Exception e) {
            LOG.error(e);
            importers = Collections.emptyList();
        }
        return importers.isEmpty() ? null : new IndexImporterMappingIndex<>(importers);
    }

    private IndexImporterMappingIndex(@Nonnull List<SnapshotInputMappingIndex<Key, Value, Input>> importers) {
        myImporters = importers;
        LOG.assertTrue(!importers.isEmpty());
    }

    @Nullable
    @Override
    public InputData<Key, Value> readData(@Nonnull Input content) {
        for (SnapshotInputMappingIndex<Key, Value, Input> importer : myImporters) {
            try {
                InputData<Key, Value> data = importer.readData(content);
                if (data != null) {
                    return data;
                }
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        return null;
    }

    @Override
    public void close() {
        for (SnapshotInputMappingIndex<Key, Value, Input> importer : myImporters) {
            try {
                importer.close();
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
    }
}
