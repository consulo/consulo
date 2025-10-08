/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.editor.impl.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.DocumentWindow;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.impl.internal.intention.QuickEditHandler;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.internal.InjectedLanguageManagerInternal;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.Balloon;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * "Quick Edit Language" intention action that provides an editor which shows an injected language
 * fragment's complete prefix and suffix in non-editable areas and allows to edit the fragment
 * without having to consider any additional escaping rules (e.g. when editing regexes in String
 * literals).
 *
 * @author Gregory Shrago
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "platform.inject.language", fileExtensions = "txt", categories = "Language Injection")
public class QuickEditAction implements IntentionAction, LowPriorityAction {
    public static final Key<QuickEditHandler> QUICK_EDIT_HANDLER = Key.create("QUICK_EDIT_HANDLER");
    public static final Key<Boolean> EDIT_ACTION_AVAILABLE = Key.create("EDIT_ACTION_AVAILABLE");
    private String myLastLanguageName;

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return getRangePair(file, editor) != null;
    }

    @Nullable
    @RequiredReadAction
    protected Pair<PsiElement, TextRange> getRangePair(PsiFile file, Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        PsiLanguageInjectionHost host = PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiLanguageInjectionHost.class, false);
        if (host == null || ElementManipulators.getManipulator(host) == null) {
            return null;
        }
        List<Pair<PsiElement, TextRange>> injections = InjectedLanguageManager.getInstance(host.getProject()).getInjectedPsiFiles(host);
        if (injections == null || injections.isEmpty()) {
            return null;
        }
        int offsetInElement = offset - host.getTextRange().getStartOffset();
        Pair<PsiElement, TextRange> rangePair = ContainerUtil.find(injections, pair -> pair.second.containsRange(offsetInElement, offsetInElement));
        if (rangePair != null) {
            Language language = rangePair.first.getContainingFile().getLanguage();
            Object action = language.getUserData(EDIT_ACTION_AVAILABLE);
            if (action != null && action.equals(false)) {
                return null;
            }

            myLastLanguageName = language.getDisplayName().get();
        }
        return rangePair;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        invokeImpl(project, editor, file);
    }

    @RequiredReadAction
    public QuickEditHandler invokeImpl(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int offset = editor.getCaretModel().getOffset();
        Pair<PsiElement, TextRange> pair = ObjectUtil.assertNotNull(getRangePair(file, editor));

        PsiFile injectedFile = (PsiFile) pair.first;
        QuickEditHandler handler = getHandler(project, injectedFile, editor, file);

        InjectedLanguageManagerInternal injectedLanguageManager =
            (InjectedLanguageManagerInternal) InjectedLanguageManager.getInstance(project);

        DocumentWindow documentWindow = injectedLanguageManager.getDocumentWindow(injectedFile);
        if (documentWindow != null) {
            handler.navigate(injectedLanguageManager.hostToInjectedUnescaped(documentWindow, offset));
        }

        return handler;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Nonnull
    private QuickEditHandler getHandler(Project project, PsiFile injectedFile, Editor editor, PsiFile origFile) {
        QuickEditHandler handler = getExistingHandler(injectedFile);
        if (handler != null && handler.isValid()) {
            return handler;
        }
        handler = new QuickEditHandler(project, injectedFile, origFile, editor, this);
        return handler;
    }

    public static QuickEditHandler getExistingHandler(@Nonnull PsiFile injectedFile) {
        InjectedLanguageManagerInternal injectedLanguageManager = (InjectedLanguageManagerInternal) InjectedLanguageManager.getInstance(injectedFile.getProject());

        PsiLanguageInjectionHost.Place shreds = injectedLanguageManager.getShreds(injectedFile);

        DocumentWindow documentWindow = injectedLanguageManager.getDocumentWindow(injectedFile);
        if (shreds == null || documentWindow == null) {
            return null;
        }

        TextRange hostRange = TextRange.create(shreds.get(0).getHostRangeMarker().getStartOffset(), shreds.get(shreds.size() - 1).getHostRangeMarker().getEndOffset());
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor.getDocument() != documentWindow.getDelegate()) {
                continue;
            }
            QuickEditHandler handler = editor.getUserData(QUICK_EDIT_HANDLER);
            if (handler != null && handler.changesRange(hostRange)) {
                return handler;
            }
        }
        return null;
    }

    public boolean isShowInBalloon() {
        return false;
    }

    @Nullable
    public JComponent createBalloonComponent(@Nonnull PsiFile file) {
        return null;
    }

    @Override
    @Nonnull
    public LocalizeValue getText() {
        if (myLastLanguageName == null) {
            return LocalizeValue.localizeTODO("Edit Injected Fragment");
        }
        return LocalizeValue.localizeTODO("Edit " + StringUtil.notNullize(myLastLanguageName, "Injected") + " Fragment");
    }

    public static Balloon.Position getBalloonPosition(Editor editor) {
        int line = editor.getCaretModel().getVisualPosition().line;
        Rectangle area = editor.getScrollingModel().getVisibleArea();
        int startLine = area.y / editor.getLineHeight() + 1;
        return (line - startLine) * editor.getLineHeight() < 200 ? Balloon.Position.below : Balloon.Position.above;
    }
}
