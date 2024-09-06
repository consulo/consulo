/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.rename.inplace;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.rename.PsiElementRenameHandler;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.editor.refactoring.rename.RenameHandlerRegistry;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "variable")
public class VariableInplaceRenameHandler implements RenameHandler {
    private static final ThreadLocal<String> ourPreventInlineRenameFlag = new ThreadLocal<>();
    private static final Logger LOG = Logger.getInstance(VariableInplaceRenameHandler.class);

    @Nonnull
    @Override
    public LocalizeValue getActionTitleValue() {
        return LocalizeValue.localizeTODO("Rename Variable...");
    }

    @Override
    @RequiredReadAction
    public final boolean isAvailableOnDataContext(final DataContext dataContext) {
        final PsiElement element = PsiElementRenameHandler.getElement(dataContext);
        final Editor editor = dataContext.getData(Editor.KEY);
        final PsiFile file = dataContext.getData(PsiFile.KEY);
        return editor != null && file != null && ourPreventInlineRenameFlag.get() == null && isAvailable(element, editor, file);
    }

    @RequiredReadAction
    protected boolean isAvailable(PsiElement element, Editor editor, PsiFile file) {
        final PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());

        RefactoringSupportProvider supportProvider =
            element == null ? null : RefactoringSupportProvider.forLanguage(element.getLanguage());
        return supportProvider != null
            && editor.getSettings().isVariableInplaceRenameEnabled()
            && supportProvider.isInplaceRenameAvailable(element, nameSuggestionContext);
    }

    @Override
    @RequiredReadAction
    public final boolean isRenaming(final DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }

    @Override
    @RequiredReadAction
    public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file, final DataContext dataContext) {
        PsiElement element = PsiElementRenameHandler.getElement(dataContext);
        if (element == null) {
            if (LookupManager.getActiveLookup(editor) != null) {
                final PsiElement elementUnderCaret = file.findElementAt(editor.getCaretModel().getOffset());
                if (elementUnderCaret != null) {
                    final PsiElement parent = elementUnderCaret.getParent();
                    if (parent instanceof PsiReference) {
                        element = ((PsiReference) parent).resolve();
                    }
                    else {
                        element = PsiTreeUtil.getParentOfType(elementUnderCaret, PsiNamedElement.class);
                    }
                }
                if (element == null) {
                    return;
                }
            }
            else {
                return;
            }
        }
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        if (checkAvailable(element, editor, dataContext)) {
            doRename(element, editor, dataContext);
        }
    }

    @Override
    @RequiredReadAction
    public void invoke(@Nonnull final Project project, @Nonnull final PsiElement[] elements, final DataContext dataContext) {
        PsiElement element = elements.length == 1 ? elements[0] : null;
        if (element == null) {
            element = PsiElementRenameHandler.getElement(dataContext);
        }
        LOG.assertTrue(element != null);
        Editor editor = dataContext.getData(Editor.KEY);
        if (checkAvailable(element, editor, dataContext)) {
            doRename(element, editor, dataContext);
        }
    }

    @RequiredReadAction
    protected boolean checkAvailable(final PsiElement elementToRename, final Editor editor, final DataContext dataContext) {
        if (!isAvailableOnDataContext(dataContext)) {
            LOG.error("Recursive invocation");
            RenameHandlerRegistry.getInstance().getRenameHandler(dataContext).invoke(
                elementToRename.getProject(),
                editor,
                elementToRename.getContainingFile(), dataContext
            );
            return false;
        }
        return true;
    }

    @Nullable
    public InplaceRefactoring doRename(final @Nonnull PsiElement elementToRename, final Editor editor, final DataContext dataContext) {
        VariableInplaceRenamer renamer = createRenamer(elementToRename, editor);
        boolean startedRename = renamer != null && renamer.performInplaceRename();

        if (!startedRename) {
            performDialogRename(elementToRename, editor, dataContext, renamer != null ? renamer.myInitialName : null);
        }
        return renamer;
    }

    protected static void performDialogRename(PsiElement elementToRename, Editor editor, DataContext dataContext) {
        performDialogRename(elementToRename, editor, dataContext, null);
    }

    protected static void performDialogRename(PsiElement elementToRename, Editor editor, DataContext dataContext, String initialName) {
        try {
            ourPreventInlineRenameFlag.set(initialName == null ? "" : initialName);
            RenameHandler handler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext);
            assert handler != null : elementToRename;
            handler.invoke(
                elementToRename.getProject(),
                editor,
                elementToRename.getContainingFile(), dataContext
            );
        }
        finally {
            ourPreventInlineRenameFlag.set(null);
        }
    }

    @Nullable
    public static String getInitialName() {
        final String str = ourPreventInlineRenameFlag.get();
        return StringUtil.isEmpty(str) ? null : str;
    }

    @Nullable
    protected VariableInplaceRenamer createRenamer(@Nonnull PsiElement elementToRename, Editor editor) {
        return new VariableInplaceRenamer((PsiNamedElement) elementToRename, editor);
    }
}
