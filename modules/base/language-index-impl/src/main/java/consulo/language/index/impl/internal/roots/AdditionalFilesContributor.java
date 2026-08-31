// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.psi.stub.AdditionalIndexableFileSet;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.language.index.impl.internal.roots.IndexableFilesContributor;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@ExtensionImpl
public class AdditionalFilesContributor implements IndexableFilesContributor {
    @Override
    public List<IndexableFilesIterator> getIndexableFiles(Project project) {
        List<IndexableFilesIterator> iterators = new ArrayList<>();
        Application.get().getExtensionPoint(IndexableSetContributor.class).forEach(contributor -> {
            iterators.add(new IndexableSetContributorFilesIterator(contributor, true));
            iterators.add(new IndexableSetContributorFilesIterator(contributor, false));
        });
        return iterators;
    }

    @Override
    public Predicate<VirtualFile> getOwnFilePredicate(Project project) {
        AdditionalIndexableFileSet additionalFilesContributor = new AdditionalIndexableFileSet();
        return additionalFilesContributor::isInSet;
    }
}
