// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.internal.psi.include;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.IOUtil;
import consulo.language.psi.include.FileIncludeInfo;
import consulo.language.psi.include.FileIncludeProvider;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FactoryMap;
import consulo.util.collection.MultiMap;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class FileIncludeIndex extends FileBasedIndexExtension<String, List<FileIncludeInfoImpl>> {

    public static final ID<String, List<FileIncludeInfoImpl>> INDEX_ID = ID.create("fileIncludes");

    private static final int BASE_VERSION = 6;

    @Nonnull
    public static List<FileIncludeInfo> getIncludes(@Nonnull VirtualFile file, @Nonnull Project project) {
        Map<String, List<FileIncludeInfoImpl>> data = FileBasedIndex.getInstance().getFileData(INDEX_ID, file, project);
        return ContainerUtil.flatten(data.values());
    }

    @Nonnull
    public static MultiMap<VirtualFile, FileIncludeInfoImpl> getIncludingFileCandidates(String fileName, @Nonnull GlobalSearchScope scope) {
        final MultiMap<VirtualFile, FileIncludeInfoImpl> result = new MultiMap<>();
        FileBasedIndex.getInstance().processValues(INDEX_ID, fileName, null, (file, value) -> {
            result.put(file, value);
            return true;
        }, scope);
        return result;
    }

    private static class Holder {
        private static final List<FileIncludeProvider> myProviders = FileIncludeProvider.EP_NAME.getExtensionList();
    }

    @Nonnull
    @Override
    public ID<String, List<FileIncludeInfoImpl>> getName() {
        return INDEX_ID;
    }

    @Nonnull
    @Override
    public DataIndexer<String, List<FileIncludeInfoImpl>, FileContent> getIndexer() {
        return new DataIndexer<>() {
            @Override
            @Nonnull
            public Map<String, List<FileIncludeInfoImpl>> map(@Nonnull FileContent inputData) {

                Map<String, List<FileIncludeInfoImpl>> map = FactoryMap.create(key -> new ArrayList<>());

                for (FileIncludeProvider provider : Holder.myProviders) {
                    if (!provider.acceptFile(inputData.getFile())) {
                        continue;
                    }
                    FileIncludeInfo[] infos = provider.getIncludeInfos(inputData);
                    if (infos.length == 0) {
                        continue;
                    }

                    for (FileIncludeInfo info : infos) {
                        FileIncludeInfoImpl impl = new FileIncludeInfoImpl(info.path, info.offset, info.runtimeOnly, provider.getId());
                        map.get(info.fileName).add(impl);
                    }
                }
                return map;
            }
        };
    }

    @Nonnull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Nonnull
    @Override
    public DataExternalizer<List<FileIncludeInfoImpl>> getValueExternalizer() {
        return new DataExternalizer<List<FileIncludeInfoImpl>>() {
            @Override
            public void save(@Nonnull DataOutput out, List<FileIncludeInfoImpl> value) throws IOException {
                out.writeInt(value.size());
                for (FileIncludeInfoImpl info : value) {
                    IOUtil.writeUTF(out, info.path);
                    out.writeInt(info.offset);
                    out.writeBoolean(info.runtimeOnly);
                    IOUtil.writeUTF(out, info.providerId);
                }
            }

            @Override
            public List<FileIncludeInfoImpl> read(@Nonnull DataInput in) throws IOException {
                int size = in.readInt();
                ArrayList<FileIncludeInfoImpl> infos = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    infos.add(new FileIncludeInfoImpl(IOUtil.readUTF(in), in.readInt(), in.readBoolean(), IOUtil.readUTF(in)));
                }
                return infos;
            }
        };
    }

    @Nonnull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.FileTypeSpecificInputFilter() {
            @Override
            public boolean acceptInput(Project project, @Nonnull VirtualFile file) {
                if (file.getFileSystem() instanceof ArchiveFileSystem) {
                    return false;
                }
                for (FileIncludeProvider provider : Holder.myProviders) {
                    if (provider.acceptFile(file)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void registerFileTypesUsedForIndexing(@Nonnull Consumer<FileType> fileTypeSink) {
                for (FileIncludeProvider provider : Holder.myProviders) {
                    provider.registerFileTypesUsedForIndexing(fileTypeSink);
                }
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        int version = BASE_VERSION;
        for (FileIncludeProvider provider : Holder.myProviders) {
            version = version * 31 + (provider.getVersion() ^ provider.getClass().getName().hashCode());
        }
        return version;
    }
}


