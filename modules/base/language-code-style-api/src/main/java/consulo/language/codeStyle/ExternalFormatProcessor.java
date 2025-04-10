// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalFormatProcessor {
    ExtensionPointName<ExternalFormatProcessor> EP_NAME = ExtensionPointName.create(ExternalFormatProcessor.class);

    /**
     * @param source the source file with code
     * @return true, if external processor selected as active (enabled) for the source file
     */
    boolean activeForFile(@Nonnull PsiFile source);

    /**
     * Formats the range in a source file.
     *
     * @param source                   the source file with code
     * @param range                    the range for formatting
     * @param canChangeWhiteSpacesOnly procedure can change only whitespaces
     * @return the range after formatting or null, if external format procedure cannot be applied to the source
     */
    @Nullable
    TextRange format(@Nonnull PsiFile source, @Nonnull TextRange range, boolean canChangeWhiteSpacesOnly);

    /**
     * Indents the line.
     *
     * @param source          the source file with code
     * @param lineStartOffset the offset of the indented line
     * @return the indentation String or null if nothing to be changed
     */
    @Nullable
    String indent(@Nonnull PsiFile source, int lineStartOffset);

    /**
     * @return the unique id for external formatter
     */
    @Nonnull
    String getId();

    /**
     * @param source the source file with code
     * @return true, if there is an active external (enabled) formatter for the source
     */
    static boolean useExternalFormatter(@Nonnull PsiFile source) {
        return EP_NAME.getExtensionList().stream().anyMatch(efp -> efp.activeForFile(source));
    }

    /**
     * @param externalFormatterId the unique id for external formatter
     * @return the external formatter with the unique id, if any
     */
    @Nonnull
    static Optional<ExternalFormatProcessor> findExternalFormatter(@Nonnull String externalFormatterId) {
        return EP_NAME.getExtensionList().stream().filter(efp -> externalFormatterId.equals(efp.getId())).findFirst();
    }

    @Nullable
    static ExternalFormatProcessor activeExternalFormatProcessor(@Nonnull PsiFile source) {
        for (ExternalFormatProcessor efp : EP_NAME.getExtensionList()) {
            if (efp.activeForFile(source)) {
                return efp;
            }
        }
        return null;
    }

    /**
     * Indents the line.
     *
     * @param source          the source file with code
     * @param lineStartOffset the offset of the indented line
     * @return the range after indentation or null if nothing to be changed
     */
    @Nullable
    static String indentLine(@Nonnull PsiFile source, int lineStartOffset) {
        ExternalFormatProcessor efp = activeExternalFormatProcessor(source);
        return efp != null ? efp.indent(source, lineStartOffset) : null;
    }

    /**
     * @param source                   the source file with code
     * @param range                    the range for formatting
     * @param canChangeWhiteSpacesOnly procedure can change only whitespaces
     * @return the range after formatting or null, if external format procedure was not found or inactive (disabled)
     */
    @Nullable
    static TextRange formatRangeInFile(@Nonnull PsiFile source, @Nonnull TextRange range, boolean canChangeWhiteSpacesOnly) {
        ExternalFormatProcessor efp = activeExternalFormatProcessor(source);
        return efp != null ? efp.format(source, range, canChangeWhiteSpacesOnly) : null;
    }

    /**
     * @param elementToFormat          the element from code file
     * @param range                    the range for formatting
     * @param canChangeWhiteSpacesOnly procedure can change only whitespaces
     * @return the element after formatting
     */
    @Nonnull
    @RequiredReadAction
    static PsiElement formatElement(@Nonnull PsiElement elementToFormat, @Nonnull TextRange range, boolean canChangeWhiteSpacesOnly) {
        PsiFile file = elementToFormat.getContainingFile();
        Document document = file.getViewProvider().getDocument();
        if (document != null) {
            TextRange rangeAfterFormat = formatRangeInFile(file, range, canChangeWhiteSpacesOnly);
            if (rangeAfterFormat != null) {
                PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
                if (!elementToFormat.isValid()) {
                    PsiElement elementAtStart = file.findElementAt(rangeAfterFormat.getStartOffset());
                    if (elementAtStart instanceof PsiWhiteSpace whiteSpace) {
                        elementAtStart = PsiTreeUtil.nextLeaf(whiteSpace);
                    }
                    if (elementAtStart != null) {
                        PsiElement parent = PsiTreeUtil.getParentOfType(elementAtStart, elementToFormat.getClass());
                        return parent != null ? parent : elementAtStart;
                    }
                }
            }
        }
        return elementToFormat;
    }
}
