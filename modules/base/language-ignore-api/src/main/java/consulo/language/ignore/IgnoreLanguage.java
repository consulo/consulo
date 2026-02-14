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
package consulo.language.ignore;

import consulo.language.InjectableLanguage;
import consulo.language.Language;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Gitignore {@link Language} definition.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.8
 */
public class IgnoreLanguage extends Language implements InjectableLanguage {
    /**
     * The {@link IgnoreLanguage} instance.
     */
    public static final IgnoreLanguage INSTANCE = new IgnoreLanguage();

    /**
     * The dot.
     */
    private static final String DOT = ".";

    /**
     * The Ignore file extension suffix.
     */
    @Nonnull
    private final String myExtension;

    /**
     * The Ignore VCS directory name.
     */
    @Nullable
    private final String myDirectory;

    /**
     * The GitignoreLanguage icon.
     */
    @Nonnull
    private final Image icon;

    /**
     * {@link IgnoreLanguage} is a non-instantiable static class.
     */
    protected IgnoreLanguage() {
        this("Ignore", "ignore", null, PlatformIconGroup.filetypesIgnored());
    }

    /**
     * {@link IgnoreLanguage} is a non-instantiable static class.
     */
    protected IgnoreLanguage(
        @Nonnull String name,
        @Nonnull String extension,
        @Nullable String vcsDirectory,
        @Nonnull Image icon
    ) {
        super(name);
        this.myExtension = extension;
        this.myDirectory = vcsDirectory;
        this.icon = icon;
    }

    /**
     * Returns Ignore file extension suffix.
     *
     * @return extension
     */
    @Nonnull
    public String getExtension() {
        return myExtension;
    }

    /**
     * Returns Ignore VCS directory name.
     *
     * @return VCS directory name
     */
    @Nullable
    public String getDirectory() {
        return myDirectory;
    }

    /**
     * The Gitignore file filename.
     *
     * @return filename.
     */
    @Nonnull
    public String getFilename() {
        return DOT + getExtension();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(getFilename() + " (" + getID() + ")");
    }

    /**
     * Returns Ignore file icon.
     *
     * @return icon
     */
    @Nonnull
    public Image getIcon() {
        return icon;
    }

    /**
     * Returns <code>true</code> if `syntax: value` entry is supported by the language (i.e. Mercurial).
     *
     * @return <code>true</code> if `syntax: value` entry is supported
     */
    public boolean isSyntaxSupported() {
        return false;
    }

    /**
     * Returns default language syntax.
     *
     * @return default syntax
     */
    @Nonnull
    public Syntax getDefaultSyntax() {
        return Syntax.GLOB;
    }
}
