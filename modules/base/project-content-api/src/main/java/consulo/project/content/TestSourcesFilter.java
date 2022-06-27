// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.content.FileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * Implementations of this extension point can tell IDE whether some particular file is a test file.
 * <p>
 * By default, IntelliJ Platform considers files as tests only if they are located under test
 * sources root {@link FileIndex#isInTestSourceContent(VirtualFile)}.
 * <p>
 * However there are plenty frameworks and languages which keep test files just nearby production files.
 * E.g. *_test.go files are test files in Go language and some js/dart files are test files depending
 * on their content. The extensions allow IDE to highlight such files with a green background,
 * properly check if they are included in built-in search scopes, etc.
 *
 * @author zolotov
 * @see FileIndex#isInTestSourceContent(VirtualFile)
 * @see JpsModuleSourceRootType#isForTests()
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TestSourcesFilter {
  private static final ExtensionPointName<TestSourcesFilter> EP_NAME = ExtensionPointName.create(TestSourcesFilter.class);

  public static boolean isTestSources(@Nonnull VirtualFile file, @Nonnull Project project) {
    for (TestSourcesFilter filter : EP_NAME.getExtensionList()) {
      if (filter.isTestSource(file, project)) {
        return true;
      }
    }
    return false;
  }

  public abstract boolean isTestSource(@Nonnull VirtualFile file, @Nonnull Project project);
}
