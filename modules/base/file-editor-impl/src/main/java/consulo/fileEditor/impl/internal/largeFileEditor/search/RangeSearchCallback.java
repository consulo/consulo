// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.largeFileEditor.search;

import consulo.fileEditor.internal.largeFileEditor.FileDataProviderForSearch;
import consulo.fileEditor.internal.largeFileEditor.SearchResult;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

public interface RangeSearchCallback {

    FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile);

    void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile);

}
