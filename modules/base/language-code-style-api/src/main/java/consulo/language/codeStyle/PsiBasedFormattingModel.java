/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.document.DocumentWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.InjectedLanguageManagerUtil;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class PsiBasedFormattingModel implements FormattingModelEx {
    private static final Logger LOG = Logger.getInstance(PsiBasedFormattingModel.class);

    private final Project myProject;
    private final ASTNode myASTNode;
    private final FormattingDocumentModel myDocumentModel;
    @Nonnull
    private final Block myRootBlock;
    protected boolean myCanModifyAllWhiteSpaces = false;

    public PsiBasedFormattingModel(PsiFile file, @Nonnull Block rootBlock, FormattingDocumentModel documentModel) {
        myASTNode = file.getNode();
        myDocumentModel = documentModel;
        myRootBlock = rootBlock;
        myProject = file.getProject();
    }

    @Override
    @RequiredReadAction
    public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
        return replaceWhiteSpace(textRange, null, whiteSpace);
    }

    @Override
    @RequiredReadAction
    public TextRange replaceWhiteSpace(TextRange textRange, ASTNode nodeAfter, String whiteSpace) {
        String whiteSpaceToUse =
            myDocumentModel.adjustWhiteSpaceIfNecessary(whiteSpace, textRange.getStartOffset(), textRange.getEndOffset(), nodeAfter, true)
                .toString();
        String wsReplaced = replaceWithPSI(textRange, whiteSpaceToUse);

        if (wsReplaced != null) {
            return new TextRange(textRange.getStartOffset(), textRange.getStartOffset() + wsReplaced.length());
        }
        else {
            return textRange;
        }
    }

    @Override
    public TextRange shiftIndentInsideRange(ASTNode node, TextRange textRange, int shift) {
        return textRange; // TODO: Remove this method from here...
    }

    @Override
    public void commitChanges() {
    }

    @Nullable
    @RequiredReadAction
    private String replaceWithPSI(TextRange textRange, String whiteSpace) {
        int offset = textRange.getEndOffset();
        ASTNode leafElement = findElementAt(offset);

        if (leafElement != null) {
            PsiFile hostFile = myASTNode.getPsi().getContainingFile();
            TextRange effectiveRange = textRange;
            List<DocumentWindow> injections = InjectedLanguageManager.getInstance(hostFile.getProject())
                .getCachedInjectedDocumentsInRange(hostFile, TextRange.from(offset, 0));
            if (!injections.isEmpty()) {
                PsiElement injectedElement = PsiDocumentManager.getInstance(myProject).getPsiFile(injections.get(0));
                PsiLanguageInjectionHost host = InjectedLanguageManagerUtil.findInjectionHost(injectedElement);
                TextRange corrected = host == null ? null : correctRangeByInjection(textRange, host);
                if (corrected != null) {
                    effectiveRange = corrected;
                }
            }

            if (leafElement.getPsi() instanceof PsiFile) {
                return null;
            }
            else {
                LOG.assertTrue(leafElement.getPsi().isValid());
                return replaceWithPsiInLeaf(effectiveRange, whiteSpace, leafElement);
            }
        }
        else if (textRange.getEndOffset() == myASTNode.getTextLength()) {
            CodeStyleManager.getInstance(myProject)
                .performActionWithFormatterDisabled((Runnable)() -> FormatterUtil.replaceLastWhiteSpace(myASTNode, whiteSpace, textRange));
            return whiteSpace;
        }
        else {
            return null;
        }
    }

    @RequiredReadAction
    private static TextRange correctRangeByInjection(TextRange textRange, PsiLanguageInjectionHost host) {
        ElementManipulator<PsiLanguageInjectionHost> manipulator = ElementManipulators.getManipulator(host);
        if (manipulator == null) {
            return null;
        }

        TextRange injectionRangeInHost = manipulator.getRangeInElement(host);
        int hostStartOffset = host.getTextRange().getStartOffset();

        int injectedDocumentStartOffset = hostStartOffset + injectionRangeInHost.getStartOffset();
        int injectedDocumentEndOffset = hostStartOffset + injectionRangeInHost.getEndOffset();

        if (textRange.getEndOffset() < injectedDocumentStartOffset || textRange.getStartOffset() > injectedDocumentEndOffset) {
            return null;
        }

        return textRange.shiftLeft(injectedDocumentStartOffset);
    }

    @Nullable
    protected String replaceWithPsiInLeaf(TextRange textRange, String whiteSpace, ASTNode leafElement) {
        if (!myCanModifyAllWhiteSpaces) {
            if (leafElement.getElementType() == TokenType.WHITE_SPACE) {
                return null;
            }
        }

        CodeStyleManager.getInstance(myProject).performActionWithFormatterDisabled(
            (Runnable)() -> FormatterUtil.replaceWhiteSpace(whiteSpace, leafElement, TokenType.WHITE_SPACE, textRange)
        );

        return whiteSpace;
    }

    @Nullable
    @RequiredReadAction
    protected ASTNode findElementAt(int offset) {
        PsiFile containingFile = myASTNode.getPsi().getContainingFile();
        Project project = containingFile.getProject();

        assert !PsiDocumentManager.getInstance(project).isUncommited(myDocumentModel.getDocument());
        // TODO:default project can not be used for injections, because latter might wants (unavailable) indices

        PsiElement psiElement = project.isDefault()
            ? null
            : InjectedLanguageManager.getInstance(containingFile.getProject()).findInjectedElementAt(containingFile, offset);

        if (psiElement != null) {
            return psiElement.getNode();
        }

        psiElement = containingFile.findElementAt(offset);
        return psiElement != null ? psiElement.getNode() : null;
    }

    @Override
    @Nonnull
    public FormattingDocumentModel getDocumentModel() {
        return myDocumentModel;
    }

    @Override
    @Nonnull
    public Block getRootBlock() {
        return myRootBlock;
    }

    public void canModifyAllWhiteSpaces() {
        myCanModifyAllWhiteSpaces = true;
    }
}
