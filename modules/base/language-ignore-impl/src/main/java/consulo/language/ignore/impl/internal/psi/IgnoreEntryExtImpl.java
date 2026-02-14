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
import consulo.language.ast.ASTNode;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.Syntax;
import consulo.language.ignore.psi.*;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * Custom {@link IgnoreElementImpl} implementation.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5
 */
public abstract class IgnoreEntryExtImpl extends IgnoreElementImpl implements IgnoreEntry {
    /**
     * Constructor.
     */
    public IgnoreEntryExtImpl(ASTNode node) {
        super(node);
    }

    /**
     * Checks if the first child is negated - i.e. `!file.txt` entry.
     *
     * @return first child is negated
     */
    @Override
    @RequiredReadAction
    public boolean isNegated() {
        return getFirstChild() instanceof IgnoreNegation;
    }

    /**
     * Checks if current entry is a directory - i.e. `dir/`.
     *
     * @return is directory
     */
    public boolean isDirectory() {
        return this instanceof IgnoreEntryFile;
    }

    /**
     * Checks if current entry is a file - i.e. `file.txt`.
     *
     * @return is file
     */
    public boolean isFile() {
        return !isDirectory();
    }

    /**
     * Returns element syntax.
     *
     * @return syntax
     */
    @Nonnull
    @Override
    @RequiredReadAction
    public Syntax getSyntax() {
        PsiElement previous = getPrevSibling();
        while (previous != null) {
            if (previous.getNode().getElementType().equals(IgnoreTypes.SYNTAX)) {
                Syntax syntax =  Syntax.find(((IgnoreSyntax) previous).getValue().getText());
                if (syntax != null) {
                    return syntax;
                }
            }
            previous = previous.getPrevSibling();
        }
        return ((IgnoreLanguage) getContainingFile().getLanguage()).getDefaultSyntax();
    }

    /**
     * Returns entry value without leading `!` if entry is negated.
     *
     * @return entry value without `!` negation sign
     */
    @Nonnull
    @Override
    @RequiredReadAction
    public String getValue() {
        String value = getText();
        if (isNegated()) {
            value = StringUtil.trimStart(value, "!");
        }
        return value;
    }
}
