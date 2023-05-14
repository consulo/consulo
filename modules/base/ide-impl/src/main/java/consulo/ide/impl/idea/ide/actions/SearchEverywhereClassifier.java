// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Philipp Smorygo
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SearchEverywhereClassifier {
  class EP_Manager {
    private EP_Manager() {
    }

    public static boolean isClass(@Nullable Object o) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        if (classifier.isClass(o)) return true;
      }
      return false;
    }

    public static boolean isSymbol(@Nullable Object o) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        if (classifier.isSymbol(o)) return true;
      }
      return false;
    }

    @Nullable
    public static VirtualFile getVirtualFile(@Nonnull Object o) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        VirtualFile virtualFile = classifier.getVirtualFile(o);
        if (virtualFile != null) return virtualFile;
      }
      return null;
    }

    @Nullable
    public static Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        Component component = classifier.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (component != null) return component;
      }
      return null;
    }

    @Nullable
    public static GlobalSearchScope getProjectScope(@Nonnull Project project) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        GlobalSearchScope scope = classifier.getProjectScope(project);
        if (scope != null) return scope;
      }
      return null;
    }
  }

  ExtensionPointName<SearchEverywhereClassifier> EP_NAME = ExtensionPointName.create(SearchEverywhereClassifier.class);

  boolean isClass(@Nullable Object o);

  boolean isSymbol(@Nullable Object o);

  @Nullable
  VirtualFile getVirtualFile(@Nonnull Object o);

  @Nullable
  Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus);

  @Nullable
  default GlobalSearchScope getProjectScope(@Nonnull Project project) {
    return null;
  }
}
