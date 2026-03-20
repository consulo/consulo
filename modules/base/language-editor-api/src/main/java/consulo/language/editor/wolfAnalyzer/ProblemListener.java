// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.wolfAnalyzer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.virtualFileSystem.VirtualFile;

@TopicAPI(ComponentScope.PROJECT)
public interface ProblemListener {

  default void problemsAppeared(VirtualFile file) {
  }

  default void problemsChanged(VirtualFile file) {
  }

  default void problemsDisappeared(VirtualFile file) {
  }
}
