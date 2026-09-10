// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.fileType.FileType;

public abstract class BaseWeakBinaryFileInputFilter extends BaseFileTypeInputFilter {
    private final BinaryFileTypePolicy myBinary;

    BaseWeakBinaryFileInputFilter(BinaryFileTypePolicy binary, FileTypeSubstitutionStrategy fileTypeSubstitutionStrategy) {
        super(fileTypeSubstitutionStrategy);
        myBinary = binary;
    }

    // Don't do like this, this is not correct (see IDEA-303356 for example):
    //    val ext = fileNameSuffix.substringAfterLast(".")
    //    val weakFileType = if (ext != fileNameSuffix) FileTypeManager.getInstance().getFileTypeByExtension(ext) else null
    //
    // Reasons: 1. this does not work with FileTypeOverrider (which can assign any type to files with given suffix)
    //          2. this does not work with FileTypeIdentifiableByVirtualFile (same reason)
    //          3. this does not work with FileTypeDetector (same reason)
    //          4. this does not work with HashBang patterns (same reason)
    //          5. this does not work with autodetection which assigns PlainText to text files, not UnknownFileType as inferred by extension

    @Override
    public ThreeState acceptFileType(FileType fileType) {
        if ((myBinary == BinaryFileTypePolicy.BINARY && !fileType.isBinary())
            || (myBinary == BinaryFileTypePolicy.NON_BINARY && fileType.isBinary())) {
            return ThreeState.NO;
        }
        return ThreeState.UNSURE; // check exact filename
    }
}
