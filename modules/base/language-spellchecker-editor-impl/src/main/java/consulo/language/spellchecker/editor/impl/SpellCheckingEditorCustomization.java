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
package consulo.language.spellchecker.editor.impl;

import consulo.codeEditor.EditorEx;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileWrapper;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.collection.Maps;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Allows to enforce editors to use/don't use spell checking ignoring user-defined spelling inspection settings.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since Aug 20, 2010 3:54:42 PM
 */
public class SpellCheckingEditorCustomization implements Consumer<EditorEx> {
  private final String mySpellcheckerEngineId;
  private final boolean myEnabled;

  public SpellCheckingEditorCustomization(boolean enabled, String spellcheckerEngineId) {
    myEnabled = enabled;
    mySpellcheckerEngineId = spellcheckerEngineId;
  }

  @Override
  public void accept(@Nonnull EditorEx editor) {
    boolean apply = myEnabled;

    Project project = editor.getProject();
    if (project == null) {
      return;
    }

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      return;
    }

    Function<InspectionProfile, InspectionProfileWrapper> strategy = file.getUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY);
    if (strategy == null) {
      file.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, strategy = new MyInspectionProfileStrategy(mySpellcheckerEngineId));
    }

    if (!(strategy instanceof MyInspectionProfileStrategy)) {
      return;
    }

    ((MyInspectionProfileStrategy)strategy).setUseSpellCheck(apply);

    if (apply) {
      editor.putUserData(IntentionManager.SHOW_INTENTION_OPTIONS_KEY, false);
    }

    // Update representation.
    DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(project);
    analyzer.restart(file);
  }

  private static class MyInspectionProfileStrategy implements Function<InspectionProfile, InspectionProfileWrapper> {
    private final String mySpellcheckerEngineId;
    private final Map<InspectionProfile, MyInspectionProfileWrapper> myWrappers = Maps.newWeakHashMap();
    private boolean myUseSpellCheck;

    public MyInspectionProfileStrategy(String spellcheckerEngineId) {
      mySpellcheckerEngineId = spellcheckerEngineId;
    }

    @Override
    public InspectionProfileWrapper apply(InspectionProfile delegate) {
      MyInspectionProfileWrapper wrapper = myWrappers.get(delegate);
      if (wrapper == null) {
        myWrappers.put(delegate, wrapper = new MyInspectionProfileWrapper(delegate, mySpellcheckerEngineId));
      }
      wrapper.setUseSpellCheck(myUseSpellCheck);
      return wrapper;
    }

    public void setUseSpellCheck(boolean useSpellCheck) {
      myUseSpellCheck = useSpellCheck;
    }
  }

  private static class MyInspectionProfileWrapper extends InspectionProfileWrapper {
    private boolean myUseSpellCheck;
    private final String mySpellcheckerEngineId;

    MyInspectionProfileWrapper(InspectionProfile delegate, String spellcheckerEngineId) {
      super(delegate);
      mySpellcheckerEngineId = spellcheckerEngineId;
    }

    @Override
    public boolean isToolEnabled(HighlightDisplayKey key, PsiElement element) {
      if (!myUseSpellCheck) {
        return false;
      }
      return SpellcheckerInspections.isOwneedSpellcheckerInspection(mySpellcheckerEngineId, key.getID());
    }

    public void setUseSpellCheck(boolean useSpellCheck) {
      myUseSpellCheck = useSpellCheck;
    }
  }
}
