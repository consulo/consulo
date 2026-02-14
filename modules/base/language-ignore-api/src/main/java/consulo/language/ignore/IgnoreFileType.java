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

import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * Describes Ignore file type.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.8
 */
public class IgnoreFileType extends LanguageFileType {
    /**
     * Current file type language.
     */
    @Nonnull
    private final IgnoreLanguage language;

    /**
     * Protected constructor to prevent direct object creation.
     */
    protected IgnoreFileType() {
        this(IgnoreLanguage.INSTANCE);
    }

    /**
     * Protected constructor to prevent direct object creation.
     */
    protected IgnoreFileType(@Nonnull IgnoreLanguage language) {
        super(language);
        this.language = language;
    }

    /**
     * Returns the name of the file type. The name must be unique among all file types registered in the system.
     *
     * @return The file type name.
     */
    @Nonnull
    @Override
    public String getId() {
        return language.getID() + ".file";
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return language.getDisplayName();
    }

    /**
     * Returns the user-readable description of the file type.
     *
     * @return The file type description.
     */
    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return language.getDisplayName();
    }

    /**
     * Returns the default extension for files of the type.
     *
     * @return The extension, not including the leading '.'.
     */
    @Nonnull
    @Override
    public String getDefaultExtension() {
        return language.getExtension();
    }

    /**
     * Returns the icon used for showing files of the type.
     *
     * @return The icon instance, or null if no icon should be shown.
     */
    @Nonnull
    @Override
    public Image getIcon() {
        return language.getIcon();
    }

    /**
     * Returns hashCode of the current {@link IgnoreLanguage}.
     *
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return language.hashCode();
    }
}
