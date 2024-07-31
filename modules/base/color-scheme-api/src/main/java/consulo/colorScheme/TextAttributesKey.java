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

import consulo.annotation.DeprecationInfo;
import consulo.util.xml.serializer.JDOMExternalizerUtil;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.application.Application;
import consulo.util.collection.Maps;
import consulo.util.lang.lazy.LazyValue;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;


/**
 * A type of item with a distinct highlighting in an editor or in other views.
 */
public final class TextAttributesKey implements Comparable<TextAttributesKey> {
    /**
     * Registers a text attribute key with the specified identifier and default attributes.
     *
     * @param externalName      the unique identifier of the key.
     * @param defaultAttributes the default text attributes associated with the key.
     * @return the new key instance, or an existing instance if the key with the same
     * identifier was already registered.
     * @deprecated Use {@link #createTextAttributesKey(String, TextAttributesKey)} to guarantee compatibility with generic color schemes.
     */
    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #of()")
    public static TextAttributesKey createTextAttributesKey(@Nonnull String externalName, TextAttributes defaultAttributes) {
        TextAttributesKey key = find(externalName);
        if (key.myDefaultAttributes == null || key.myDefaultAttributes == NULL_ATTRIBUTES) {
            key.myDefaultAttributes = defaultAttributes;
        }
        return key;
    }

    /**
     * Registers a text attribute key with the specified identifier and a fallback key. If text attributes for the key are not defined in
     * a color scheme, they will be acquired by the fallback key if possible.
     * <p>Fallback keys can be chained, for example, text attribute key
     * A can depend on key B which in turn can depend on key C. So if text attributes neither for A nor for B are found, they will be
     * acquired by the key C.
     * <p>Fallback keys can be used from any place including language's own definitions. Note that there is a common set of keys called
     * <code>DefaultLanguageHighlighterColors</code> which can be used as a base. Scheme designers are supposed to set colors for these
     * keys primarily and using them guarantees that most (if not all) text attributes will be shown correctly for the language
     * regardless of a color scheme.
     *
     * @param externalName         the unique identifier of the key.
     * @param fallbackAttributeKey the fallback key to use if text attributes for this key are not defined.
     * @return the new key instance, or an existing instance if the key with the same
     * identifier was already registered.
     */
    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #of()")
    public static TextAttributesKey createTextAttributesKey(@Nonnull String externalName, TextAttributesKey fallbackAttributeKey) {
        return of(externalName, fallbackAttributeKey);
    }

    /**
     * Registers a text attribute key with the specified identifier.
     *
     * @param externalName the unique identifier of the key.
     * @return the new key instance, or an existing instance if the key with the same
     * identifier was already registered.
     */
    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #of()")
    public static TextAttributesKey createTextAttributesKey(@Nonnull String externalName) {
        return find(externalName);
    }

    /**
     * Registers a text attribute key with the specified identifier.
     *
     * @param externalName the unique identifier of the key.
     * @return the new key instance, or an existing instance if the key with the same
     * identifier was already registered.
     */
    @Nonnull
    public static TextAttributesKey of(@Nonnull String externalName) {
        return find(externalName);
    }

    /**
     * Registers a text attribute key with the specified identifier and a fallback key. If text attributes for the key are not defined in
     * a color scheme, they will be acquired by the fallback key if possible.
     * <p>Fallback keys can be chained, for example, text attribute key
     * A can depend on key B which in turn can depend on key C. So if text attributes neither for A nor for B are found, they will be
     * acquired by the key C.
     * <p>Fallback keys can be used from any place including language's own definitions. Note that there is a common set of keys called
     * <code>DefaultLanguageHighlighterColors</code> which can be used as a base. Scheme designers are supposed to set colors for these
     * keys primarily and using them guarantees that most (if not all) text attributes will be shown correctly for the language
     * regardless of a color scheme.
     *
     * @param externalName         the unique identifier of the key.
     * @param fallbackAttributeKey the fallback key to use if text attributes for this key are not defined.
     * @return the new key instance, or an existing instance if the key with the same
     * identifier was already registered.
     */
    @Nonnull
    public static TextAttributesKey of(@Nonnull String externalName, TextAttributesKey fallbackAttributeKey) {
        TextAttributesKey key = find(externalName);
        key.setFallbackAttributeKey(fallbackAttributeKey);
        return key;
    }

    private static final TextAttributes NULL_ATTRIBUTES = new TextAttributes();
    private static final ConcurrentMap<String, TextAttributesKey> ourRegistry = new ConcurrentHashMap<>();
    private static final Supplier<TextAttributeKeyDefaultsProvider> ourDefaultsProvider =
        LazyValue.notNull(() -> Application.get().getInstance(TextAttributeKeyDefaultsProvider.class));

    private final String myExternalName;
    private TextAttributes myDefaultAttributes = NULL_ATTRIBUTES;
    private TextAttributesKey myFallbackAttributeKey;

    private TextAttributesKey(String externalName) {
        myExternalName = externalName;
    }

    //read external only
    public TextAttributesKey(@Nonnull Element element) {
        this(JDOMExternalizerUtil.readField(element, "myExternalName"));
        Element myDefaultAttributesElement = JDOMExternalizerUtil.getOption(element, "myDefaultAttributes");
        if (myDefaultAttributesElement != null) {
            myDefaultAttributes = new TextAttributes(myDefaultAttributesElement);
        }
    }

    @Nonnull
    public static TextAttributesKey find(@Nonnull String externalName) {
        return Maps.cacheOrGet(ourRegistry, externalName, new TextAttributesKey(externalName));
    }

    @Override
    public String toString() {
        return myExternalName;
    }

    public String getExternalName() {
        return myExternalName;
    }

    @Override
    public int compareTo(@Nonnull TextAttributesKey key) {
        return myExternalName.compareTo(key.myExternalName);
    }


    public void writeExternal(Element element) throws WriteExternalException {
        JDOMExternalizerUtil.writeField(element, "myExternalName", myExternalName);

        if (myDefaultAttributes != NULL_ATTRIBUTES) {
            Element option = JDOMExternalizerUtil.writeOption(element, "myDefaultAttributes");
            myDefaultAttributes.writeExternal(option);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TextAttributesKey that = (TextAttributesKey)o;

        return myExternalName.equals(that.myExternalName);
    }

    @Override
    public int hashCode() {
        return myExternalName.hashCode();
    }

    /**
     * Returns the default text attributes associated with the key.
     *
     * @return the text attributes.
     */
    public TextAttributes getDefaultAttributes() {
        if (myDefaultAttributes == NULL_ATTRIBUTES) {
            myDefaultAttributes = null;
            final TextAttributeKeyDefaultsProvider provider = ourDefaultsProvider.get();
            if (provider != null) {
                myDefaultAttributes = provider.getDefaultAttributes(this);
            }
        }
        else if (myDefaultAttributes == null) {
            myDefaultAttributes = NULL_ATTRIBUTES;
        }
        return myDefaultAttributes;
    }

    public TextAttributesKey getFallbackAttributeKey() {
        return myFallbackAttributeKey;
    }

    public void setFallbackAttributeKey(TextAttributesKey fallbackAttributeKey) {
        myFallbackAttributeKey = fallbackAttributeKey;
    }
}
