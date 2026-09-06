// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.IndexedFile;

public class ExactFileNameInputFilter extends BaseWeakBinaryFileInputFilter {
    private final String myFileName;
    private final boolean myIgnoreCase;

    public ExactFileNameInputFilter(String fileName, boolean ignoreCase) {
        this(fileName, ignoreCase, BinaryFileTypePolicy.BINARY_OR_NON_BINARY);
    }

    public ExactFileNameInputFilter(String fileName, boolean ignoreCase, BinaryFileTypePolicy binary) {
        super(binary, FileTypeSubstitutionStrategy.BEFORE_SUBSTITUTION);
        myFileName = fileName;
        myIgnoreCase = ignoreCase;
    }

    @Override
    public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
        return myIgnoreCase ? file.getFileName().equalsIgnoreCase(myFileName) : file.getFileName().equals(myFileName);
    }
}
