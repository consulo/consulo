// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.virtualFileSystem.VirtualFile;
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
            return Application.get().getExtensionPoint(SearchEverywhereClassifier.class)
                .anyMatchSafe(classifier -> classifier.isClass(o));
        }

        public static boolean isSymbol(@Nullable Object o) {
            return Application.get().getExtensionPoint(SearchEverywhereClassifier.class)
                .anyMatchSafe(classifier -> classifier.isSymbol(o));
        }

        @Nullable
        public static VirtualFile getVirtualFile(@Nonnull Object o) {
            return Application.get().getExtensionPoint(SearchEverywhereClassifier.class)
                .computeSafeIfAny(classifier -> classifier.getVirtualFile(o));
        }

        @Nullable
        public static Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            return Application.get().getExtensionPoint(SearchEverywhereClassifier.class)
                .computeSafeIfAny(classifier -> classifier.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus));
        }
    }

    boolean isClass(@Nullable Object o);

    boolean isSymbol(@Nullable Object o);

    @Nullable
    VirtualFile getVirtualFile(@Nonnull Object o);

    @Nullable
    Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus);
}
