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

package consulo.language.codeStyle.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.*;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.PsiToDocumentSynchronizer;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class FormattingDocumentModelImpl implements FormattingDocumentModel {

    private final WhiteSpaceFormattingStrategy myWhiteSpaceStrategy;
    //private final CharBuffer myBuffer = CharBuffer.allocate(1);
    @Nonnull
    private final Document myDocument;
    private final PsiFile myFile;

    private static final Logger LOG = Logger.getInstance(FormattingDocumentModelImpl.class);
    private final CodeStyleSettings mySettings;

    public FormattingDocumentModelImpl(@Nonnull Document document, @Nullable PsiFile file) {
        myDocument = document;
        myFile = file;
        if (file != null) {
            Language language = file.getLanguage();
            myWhiteSpaceStrategy = WhiteSpaceFormattingStrategyFactory.getStrategy(language);
        }
        else {
            myWhiteSpaceStrategy = WhiteSpaceFormattingStrategyFactory.getStrategy();
        }
        mySettings = CodeStyleSettingsManager.getSettings(file != null ? file.getProject() : null);
    }

    public static FormattingDocumentModelImpl createOn(PsiFile file) {
        Document document = getDocumentToBeUsedFor(file);
        if (document != null) {
            if (PsiDocumentManager.getInstance(file.getProject()).isUncommited(document)) {
                LOG.error("Document is uncommitted");
            }
            if (!file.textMatches(document.getImmutableCharSequence())) {
                LOG.error("Document and psi file texts should be equal: file " + file);
            }
            return new FormattingDocumentModelImpl(document, file);
        }
        else {
            DocumentFactory factory = DocumentFactory.getInstance();
            return new FormattingDocumentModelImpl(factory.createDocument(file.getText(), false), file);
        }

    }

    @Nullable
    public static Document getDocumentToBeUsedFor(PsiFile file) {
        Project project = file.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return null;
        }
        if (PsiDocumentManager.getInstance(project).isUncommited(document)) {
            return null;
        }
        PsiToDocumentSynchronizer synchronizer = ((PsiDocumentManagerBase) PsiDocumentManager.getInstance(file.getProject())).getSynchronizer();
        if (synchronizer.isDocumentAffectedByTransactions(document)) {
            return null;
        }

        return document;
    }

    @Override
    public int getLineNumber(int offset) {
        if (offset > myDocument.getTextLength()) {
            LOG.error(String.format("Invalid offset detected (%d). Document length: %d. Target file: %s", offset, myDocument.getTextLength(), myFile));
        }
        return myDocument.getLineNumber(offset);
    }

    @Override
    public int getLineStartOffset(int line) {
        return myDocument.getLineStartOffset(line);
    }

    @Override
    public CharSequence getText(TextRange textRange) {
        if (textRange.getStartOffset() < 0 || textRange.getEndOffset() > myDocument.getTextLength()) {
            LOG.error(String.format("Please submit a ticket to the tracker and attach current source file to it!%nInvalid processing detected: given text " +
                "range (%s) targets non-existing regions (the boundaries are [0; %d)). File's language: %s", textRange, myDocument.getTextLength(), myFile.getLanguage()));
        }
        return myDocument.getCharsSequence().subSequence(textRange.getStartOffset(), textRange.getEndOffset());
    }

    @Override
    public int getTextLength() {
        return myDocument.getTextLength();
    }

    @Nonnull
    @Override
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    @RequiredReadAction
    public boolean containsWhiteSpaceSymbolsOnly(int startOffset, int endOffset) {
        WhiteSpaceFormattingStrategy strategy = myWhiteSpaceStrategy;
        if (strategy.check(myDocument.getCharsSequence(), startOffset, endOffset) >= endOffset) {
            return true;
        }
        PsiElement injectedElement = myFile != null ? InjectedLanguageManager.getInstance(myFile.getProject()).findElementAtNoCommit(myFile, startOffset) : null;
        if (injectedElement != null) {
            Language injectedLanguage = injectedElement.getLanguage();
            if (!injectedLanguage.equals(myFile.getLanguage())) {
                WhiteSpaceFormattingStrategy localStrategy = WhiteSpaceFormattingStrategyFactory.getStrategy(injectedLanguage);
                if (localStrategy != null) {
                    return localStrategy.check(myDocument.getCharsSequence(), startOffset, endOffset) >= endOffset;
                }
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public CharSequence adjustWhiteSpaceIfNecessary(@Nonnull CharSequence whiteSpaceText, int startOffset, int endOffset, ASTNode nodeAfter, boolean changedViaPsi) {
        if (!changedViaPsi) {
            return myWhiteSpaceStrategy.adjustWhiteSpaceIfNecessary(whiteSpaceText, myDocument.getCharsSequence(), startOffset, endOffset, mySettings, nodeAfter);
        }

        PsiElement element = myFile.findElementAt(startOffset);
        if (element == null) {
            return whiteSpaceText;
        }
        else {
            return myWhiteSpaceStrategy.adjustWhiteSpaceIfNecessary(whiteSpaceText, element, startOffset, endOffset, mySettings);
        }
    }

    //@Override
    //public boolean isWhiteSpaceSymbol(char symbol) {
    //  myBuffer.put(0, symbol);
    //  return myWhiteSpaceStrategy.check(myBuffer, 0, 1) > 0;
    //}

    @RequiredReadAction
    public static boolean canUseDocumentModel(@Nonnull Document document, @Nonnull PsiFile file) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(file.getProject());
        return !psiDocumentManager.isUncommited(document)
            && !psiDocumentManager.isDocumentBlockedByPsi(document)
            && file.getText().equals(document.getText());
    }
}
