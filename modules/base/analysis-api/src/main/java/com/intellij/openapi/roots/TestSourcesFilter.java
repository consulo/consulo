// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

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
public abstract class TestSourcesFilter {
  private static final ExtensionPointName<TestSourcesFilter> EP_NAME = ExtensionPointName.create("com.intellij.testSourcesFilter");

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
