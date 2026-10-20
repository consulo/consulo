/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package consulo.language.ignore.impl.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.ast.IFileElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.ignore.IgnoreFileType;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.psi.IgnoreFile;
import consulo.language.ignore.psi.IgnoreTypes;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElementVisitor;
import consulo.virtualFileSystem.fileType.FileType;

import java.util.ArrayList;
import java.util.Set;

/**
 * Base representation of IgnoreFile for different {@link IgnoreFileType}.
 * This class does not extend {@link consulo.language.impl.psi.PsiFileBase}
 * since it support multiple ignore language by leverage the same hierarchy of {@link IgnoreTypes}
 * and reuse the same {@link consulo.language.ignore.impl.internal.IgnoreParserDefinition}
 */
public class IgnoreFileImpl extends PsiFileImpl implements IgnoreFile {
    private final Language myLanguage;

    private final ParserDefinition myParserDefinition;

    private final IgnoreFileType myFileType;

    public IgnoreFileImpl(FileViewProvider viewProvider, IgnoreFileType fileType) {
        super(viewProvider);

        myFileType = fileType;
        myLanguage = findLanguage(fileType.getLanguage(), viewProvider);

        ParserDefinition parserDefinition = ParserDefinition.forLanguage(myLanguage);
        if (parserDefinition == null) {
            throw new RuntimeException(
                "IgnoreFile: language.getParserDefinition() returned null for: " + myLanguage
            );
        }
        myParserDefinition = parserDefinition;

        IFileElementType nodeType = parserDefinition.getFileNodeType();
        init(nodeType, nodeType);
    }

    /**
     * Searches for the matching language in {@link FileViewProvider}.
     *
     * @param baseLanguage language to look for
     * @param viewProvider current {@link FileViewProvider}
     * @return matched {@link Language}
     */
    private static Language findLanguage(Language baseLanguage, FileViewProvider viewProvider) {
        Set<Language> languages = viewProvider.getLanguages();

        for (Language actualLanguage : languages) {
            if (actualLanguage.isKindOf(baseLanguage) && actualLanguage instanceof IgnoreLanguage) {
                return actualLanguage;
            }
        }

        throw new AssertionError("Language " + baseLanguage + " doesn't participate in view provider " +
            viewProvider + ": " + new ArrayList<>(languages));
    }

    /**
     * Passes the element to the specified visitor.
     *
     * @param visitor the visitor to pass the element to.
     */
    @Override
    public void accept(PsiElementVisitor visitor) {
        visitor.visitFile(this);
    }

    /**
     * Returns current language.
     *
     * @return current {@link Language}
     */
    @Override
    @RequiredReadAction
    public final Language getLanguage() {
        return myLanguage;
    }

    /**
     * Returns current parser definition.
     *
     * @return current {@link ParserDefinition}
     */
    public ParserDefinition getParserDefinition() {
        return myParserDefinition;
    }

    /**
     * Returns the file type for the file.
     *
     * @return the file type instance.
     */
    @Override
    public FileType getFileType() {
        return myFileType;
    }

    /**
     * Returns {@link IgnoreFileType} string interpretation.
     *
     * @return string interpretation
     */
    @Override
    public String toString() {
        return myFileType.getId();
    }
}
