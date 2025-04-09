// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi.stub;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeDetector;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@ExtensionImpl
public final class FileTypeIndexImpl extends ScalarIndexExtension<FileType> implements FileBasedIndex.InputFilter, KeyDescriptor<FileType>, DataIndexer<FileType, Void, FileContent> {
    static final ID<FileType, Void> NAME = FileTypeIndex.NAME;

    @Nonnull
    @Override
    public ID<FileType, Void> getName() {
        return NAME;
    }

    @Nonnull
    @Override
    public DataIndexer<FileType, Void, FileContent> getIndexer() {
        return this;
    }

    @Nonnull
    @Override
    public KeyDescriptor<FileType> getKeyDescriptor() {
        return this;
    }

    @Nonnull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return this;
    }

    @Override
    public boolean dependsOnFileContent() {
        return false;
    }

    @Override
    public int getVersion() {
        FileType[] types = FileTypeRegistry.getInstance().getRegisteredFileTypes();
        int version = 2;
        for (FileType type : types) {
            version += type.getId().hashCode();
        }

        version *= 31;
        for (FileTypeDetector detector : FileTypeDetector.EP_NAME.getExtensionList()) {
            version += detector.getVersion();
        }
        return version;
    }

    @Override
    public boolean acceptInput(Project project, @Nonnull VirtualFile file) {
        return !file.isDirectory();
    }

    @Override
    public void save(@Nonnull DataOutput out, FileType value) throws IOException {
        EnumeratorStringDescriptor.INSTANCE.save(out, value.getId());
    }

    @Override
    public FileType read(@Nonnull DataInput in) throws IOException {
        String read = EnumeratorStringDescriptor.INSTANCE.read(in);
        return FileTypeRegistry.getInstance().findFileTypeByName(read);
    }

    @Override
    public int hashCode(FileType value) {
        return value.getId().hashCode();
    }

    @Override
    public boolean equals(FileType val1, FileType val2) {
        if (val1 instanceof SubstitutedFileType) {
            val1 = ((SubstitutedFileType)val1).getOriginalFileType();
        }
        if (val2 instanceof SubstitutedFileType) {
            val2 = ((SubstitutedFileType)val2).getOriginalFileType();
        }
        return Comparing.equal(val1, val2);
    }

    @Nonnull
    @Override
    public Map<FileType, Void> map(@Nonnull FileContent inputData) {
        return Collections.singletonMap(inputData.getFileType(), null);
    }
}
