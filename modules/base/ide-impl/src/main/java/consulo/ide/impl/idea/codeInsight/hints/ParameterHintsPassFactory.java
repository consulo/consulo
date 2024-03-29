/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import consulo.ide.impl.idea.codeInsight.hints.filtering.Matcher;
import consulo.ide.impl.idea.codeInsight.hints.filtering.MatcherConstructor;
import consulo.ide.impl.idea.codeInsight.hints.settings.Diff;
import consulo.ide.impl.idea.codeInsight.hints.settings.ParameterNameHintsSettings;
import consulo.language.Language;
import consulo.application.Application;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.ide.impl.idea.openapi.editor.ex.util.CaretVisualPositionKeeper;
import consulo.application.progress.ProgressIndicator;
import consulo.language.editor.inlay.InlayInfo;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.inlay.MethodInfo;
import consulo.util.lang.Pair;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SyntaxTraverser;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.util.dataholder.Key;
import gnu.trove.TIntObjectHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@ExtensionImpl
public class ParameterHintsPassFactory implements TextEditorHighlightingPassFactory {
  private static final Key<Boolean> REPEATED_PASS = Key.create("RepeatedParameterHintsPass");

  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
  }

  @Nullable
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
    if (!Application.get().isSwingApplication()) {
      return null;
    }
    if (editor.isOneLineMode()) {
      return null;
    }
    return new ParameterHintsPass(file, editor);
  }

  private static class ParameterHintsPass extends EditorBoundHighlightingPass {
    private final Map<Integer, Pair<String, InlayParameterHintsProvider>> myAnnotations = new HashMap<>();

    private ParameterHintsPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
      super(editor, file, true);
    }

    @RequiredReadAction
    @Override
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
      assert myDocument != null;
      myAnnotations.clear();
      if (!isEnabled()) return;

      Language language = myFile.getLanguage();
      InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
      if (provider == null) return;

      Set<String> blackList = getBlackList(language);
      Language dependentLanguage = provider.getBlackListDependencyLanguage();
      if (dependentLanguage != null) {
        blackList.addAll(getBlackList(dependentLanguage));
      }

      List<Matcher> matchers = blackList.stream().map(MatcherConstructor::createMatcher).filter(Objects::nonNull).collect(Collectors.toList());

      SyntaxTraverser.psiTraverser(myFile).forEach(element -> process(element, provider, matchers));
    }

    private static Set<String> getBlackList(Language language) {
      InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
      if (provider != null) {
        ParameterNameHintsSettings settings = ParameterNameHintsSettings.getInstance();
        Diff diff = settings.getBlackListDiff(language);
        return diff.applyOn(provider.getDefaultBlackList());
      }
      return ContainerUtil.newHashOrEmptySet(ContainerUtil.emptyIterable());
    }

    private static boolean isEnabled() {
      return EditorSettingsExternalizable.getInstance().isShowParameterNameHints();
    }

    private static boolean isMatchedByAny(MethodInfo info, List<Matcher> matchers) {
      return matchers.stream().anyMatch((e) -> e.isMatching(info.getFullyQualifiedName(), info.getParamNames()));
    }

    private void process(PsiElement element, InlayParameterHintsProvider provider, List<Matcher> blackListMatchers) {
      List<InlayInfo> hints = provider.getParameterHints(element);
      if (hints.isEmpty()) return;
      MethodInfo info = provider.getMethodInfo(element);
      if (info == null || !isMatchedByAny(info, blackListMatchers)) {
        hints.forEach((h) -> myAnnotations.put(h.getOffset(), Pair.create(h.getText(), provider)));
      }
    }

    @Override
    public void doApplyInformationToEditor() {
      assert myDocument != null;
      boolean firstTime = myEditor.getUserData(REPEATED_PASS) == null;
      ParameterHintsPresentationManager presentationManager = ParameterHintsPresentationManager.getInstance();
      Set<String> removedHints = new HashSet<>();
      TIntObjectHashMap<Caret> caretMap = new TIntObjectHashMap<>();
      CaretVisualPositionKeeper keeper = new CaretVisualPositionKeeper(myEditor);
      for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
        caretMap.put(caret.getOffset(), caret);
      }
      for (Inlay inlay : myEditor.getInlayModel().getInlineElementsInRange(0, myDocument.getTextLength())) {
        if (!presentationManager.isParameterHint(inlay)) continue;
        int offset = inlay.getOffset();
        Pair<String, InlayParameterHintsProvider> pair = myAnnotations.remove(offset);
        String newText = pair == null ? null : pair.getFirst();
        InlayParameterHintsProvider parameterHintsProvider = pair == null ? null : pair.getSecond();
        if (delayRemoval(inlay, caretMap)) continue;
        String oldText = presentationManager.getHintText(inlay);
        if (!Objects.equals(newText, oldText)) {
          if (newText == null) {
            removedHints.add(oldText);
            presentationManager.deleteHint(myEditor, inlay);
          }
          else {
            presentationManager.replaceHint(myEditor, inlay, parameterHintsProvider, newText);
          }
        }
      }
      for (Map.Entry<Integer, Pair<String, InlayParameterHintsProvider>> e : myAnnotations.entrySet()) {
        int offset = e.getKey();
        Pair<String, InlayParameterHintsProvider> value = e.getValue();
        String text = value.getFirst();
        InlayParameterHintsProvider provider = value.getSecond();
        presentationManager.addHint(myEditor, offset, provider, text, !firstTime && !removedHints.contains(text));
      }
      keeper.restoreOriginalLocation();
      myEditor.putUserData(REPEATED_PASS, Boolean.TRUE);
    }

    private boolean delayRemoval(Inlay inlay, TIntObjectHashMap<Caret> caretMap) {
      int offset = inlay.getOffset();
      Caret caret = caretMap.get(offset);
      if (caret == null) return false;
      char afterCaret = myEditor.getDocument().getImmutableCharSequence().charAt(offset);
      if (afterCaret != ',' && afterCaret != ')') return false;
      VisualPosition afterInlayPosition = myEditor.offsetToVisualPosition(offset, true, false);
      // check whether caret is to the right of inlay
      if (!caret.getVisualPosition().equals(afterInlayPosition)) return false;
      return true;
    }
  }
}
