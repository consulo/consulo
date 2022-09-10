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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.AnnotationSourceSwitcher;
import consulo.ide.impl.idea.util.containers.ContainerUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
class SwitchAnnotationSourceAction extends AnAction {
  private final static String ourShowMerged = VcsBundle.message("annotation.switch.to.merged.text");
  private final static String ourHideMerged = VcsBundle.message("annotation.switch.to.original.text");
  private final AnnotationSourceSwitcher mySwitcher;
  private final EditorGutterComponentEx myGutter;
  private final List<Consumer<AnnotationSource>> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private boolean myShowMerged;

  SwitchAnnotationSourceAction(final AnnotationSourceSwitcher switcher, final EditorGutterComponentEx gutter) {
    mySwitcher = switcher;
    myGutter = gutter;
    myShowMerged = mySwitcher.getDefaultSource().showMerged();
  }

  public void addSourceSwitchListener(final Consumer<AnnotationSource> listener) {
    myListeners.add(listener);
  }

  @Override
  public void update(final AnActionEvent e) {
    e.getPresentation().setText(myShowMerged ? ourHideMerged : ourShowMerged);
  }

  public void actionPerformed(AnActionEvent e) {
    myShowMerged = !myShowMerged;
    final AnnotationSource newSource = AnnotationSource.getInstance(myShowMerged);
    mySwitcher.switchTo(newSource);
    for (Consumer<AnnotationSource> listener : myListeners) {
      listener.accept(newSource);
    }
    myGutter.revalidateMarkup();
  }
}
