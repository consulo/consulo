/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal.annotate;

import consulo.codeEditor.EditorGutterComponentEx;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.Lists;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.AnnotationSourceSwitcher;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
class SwitchAnnotationSourceAction extends AnAction {
  private final AnnotationSourceSwitcher mySwitcher;
  private final EditorGutterComponentEx myGutter;
  private final List<Consumer<AnnotationSource>> myListeners = Lists.newLockFreeCopyOnWriteList();
  private boolean myShowMerged;

  SwitchAnnotationSourceAction(AnnotationSourceSwitcher switcher, EditorGutterComponentEx gutter) {
    mySwitcher = switcher;
    myGutter = gutter;
    myShowMerged = mySwitcher.getDefaultSource().showMerged();
  }

  public void addSourceSwitchListener(Consumer<AnnotationSource> listener) {
    myListeners.add(listener);
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setTextValue(
      myShowMerged
        ? VcsLocalize.annotationSwitchToOriginalText()
        : VcsLocalize.annotationSwitchToMergedText()
    );
  }

  public void actionPerformed(AnActionEvent e) {
    myShowMerged = !myShowMerged;
    AnnotationSource newSource = AnnotationSource.getInstance(myShowMerged);
    mySwitcher.switchTo(newSource);
    for (Consumer<AnnotationSource> listener : myListeners) {
      listener.accept(newSource);
    }
    myGutter.revalidateMarkup();
  }
}
