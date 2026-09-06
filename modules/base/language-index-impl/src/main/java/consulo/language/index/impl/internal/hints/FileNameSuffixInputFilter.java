// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.IndexedFile;
import consulo.util.lang.StringUtil;

public class FileNameSuffixInputFilter extends BaseWeakBinaryFileInputFilter {
    private final String myFileNameSuffix;
    private final boolean myIgnoreCase;

    public FileNameSuffixInputFilter(String fileNameSuffix, boolean ignoreCase) {
        this(fileNameSuffix, ignoreCase, BinaryFileTypePolicy.BINARY_OR_NON_BINARY);
    }

    public FileNameSuffixInputFilter(String fileNameSuffix, boolean ignoreCase, BinaryFileTypePolicy binary) {
        super(binary, FileTypeSubstitutionStrategy.BEFORE_SUBSTITUTION);
        myFileNameSuffix = fileNameSuffix;
        myIgnoreCase = ignoreCase;
    }

    @Override
    public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
        return myIgnoreCase
            ? StringUtil.endsWithIgnoreCase(file.getFileName(), myFileNameSuffix)
            : StringUtil.endsWith(file.getFileName(), myFileNameSuffix);
    }
}
