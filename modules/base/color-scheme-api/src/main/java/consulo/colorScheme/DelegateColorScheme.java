/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.colorScheme;

import consulo.ui.color.ColorValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.awt.*;
import java.util.Map;

/**
 * @author spLeaner
 */
public abstract class DelegateColorScheme implements EditorColorsScheme {
    private EditorColorsScheme myDelegate;

    public DelegateColorScheme(@Nonnull EditorColorsScheme delegate) {
        myDelegate = delegate;
    }

    public EditorColorsScheme getDelegate() {
        return myDelegate;
    }

    public void setDelegate(@Nonnull EditorColorsScheme delegate) {
        myDelegate = delegate;
    }

    @Override
    public void setName(String name) {
        myDelegate.setName(name);
    }

    @Override
    public TextAttributes getAttributes(TextAttributesKey key) {
        return myDelegate.getAttributes(key);
    }

    @Override
    public void setAttributes(TextAttributesKey key, TextAttributes attributes) {
        myDelegate.setAttributes(key, attributes);
    }

    @Nonnull
    @Override
    public ColorValue getDefaultBackground() {
        return myDelegate.getDefaultBackground();
    }

    @Nonnull
    @Override
    public ColorValue getDefaultForeground() {
        return myDelegate.getDefaultForeground();
    }

    @Nullable
    @Override
    public ColorValue getColor(EditorColorKey key) {
        return myDelegate.getColor(key);
    }

    @Override
    public void setColor(EditorColorKey key, @Nullable ColorValue color) {
        myDelegate.setColor(key, color);
    }

    @Nonnull
    @Override
    public FontPreferences getFontPreferences() {
        return myDelegate.getFontPreferences();
    }

    @Override
    public void setFontPreferences(@Nonnull FontPreferences preferences) {
        myDelegate.setFontPreferences(preferences);
    }

    @Override
    public int getEditorFontSize() {
        return myDelegate.getEditorFontSize();
    }

    @Override
    public int getEditorFontSize(boolean scale) {
        return myDelegate.getEditorFontSize(scale);
    }

    @Override
    public void setEditorFontSize(int fontSize) {
        myDelegate.setEditorFontSize(fontSize);
    }

    @Override
    public FontSize getQuickDocFontSize() {
        return myDelegate.getQuickDocFontSize();
    }

    @Override
    public void setQuickDocFontSize(@Nonnull FontSize fontSize) {
        myDelegate.setQuickDocFontSize(fontSize);
    }

    @Override
    public String getEditorFontName() {
        return myDelegate.getEditorFontName();
    }

    @Override
    public void setEditorFontName(String fontName) {
        myDelegate.setEditorFontName(fontName);
    }

    @Override
    public Font getFont(EditorFontType key) {
        return myDelegate.getFont(key);
    }

    @Override
    public void setFont(EditorFontType key, Font font) {
        myDelegate.setFont(key, font);
    }

    @Override
    public float getLineSpacing() {
        return myDelegate.getLineSpacing();
    }

    @Override
    public void setLineSpacing(float lineSpacing) {
        myDelegate.setLineSpacing(lineSpacing);
    }

    @Override
    public void readExternal(Element element) {
    }

    @Nonnull
    @Override
    public String getName() {
        return myDelegate.getName();
    }

    @Override
    public EditorColorsScheme clone() {
        return myDelegate.clone();
    }

    @Nonnull
    @Override
    public FontPreferences getConsoleFontPreferences() {
        return myDelegate.getConsoleFontPreferences();
    }

    @Override
    public void setConsoleFontPreferences(@Nonnull FontPreferences preferences) {
        myDelegate.setConsoleFontPreferences(preferences);
    }

    @Override
    public String getConsoleFontName() {
        return myDelegate.getConsoleFontName();
    }

    @Override
    public void setConsoleFontName(String fontName) {
        myDelegate.setConsoleFontName(fontName);
    }

    @Override
    public int getConsoleFontSize() {
        return myDelegate.getConsoleFontSize();
    }

    @Override
    public int getConsoleFontSize(boolean scale) {
        return myDelegate.getConsoleFontSize(scale);
    }

    @Override
    public void setConsoleFontSize(int fontSize) {
        myDelegate.setConsoleFontSize(fontSize);
    }

    @Override
    public float getConsoleLineSpacing() {
        return myDelegate.getConsoleLineSpacing();
    }

    @Override
    public void setConsoleLineSpacing(float lineSpacing) {
        myDelegate.setConsoleLineSpacing(lineSpacing);
    }
}
