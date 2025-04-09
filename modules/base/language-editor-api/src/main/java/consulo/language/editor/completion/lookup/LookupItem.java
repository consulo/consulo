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

package consulo.language.editor.completion.lookup;

import consulo.codeEditor.Editor;
import consulo.language.editor.completion.AutoCompletionPolicy;
import consulo.language.editor.completion.ClassConditionKey;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents an item of a lookup list.
 */
public class LookupItem<T> extends MutableLookupElement<T> implements Comparable {
    public static final ClassConditionKey<LookupItem> CLASS_CONDITION_KEY = ClassConditionKey.create(LookupItem.class);

    public static final Object HIGHLIGHTED_ATTR = Key.create("highlighted");
    public static final Object ICON_ATTR = Key.create("icon");
    public static final Object TYPE_TEXT_ATTR = Key.create("typeText");
    public static final Object TAIL_TEXT_ATTR = Key.create("tailText");
    public static final Object TAIL_TEXT_SMALL_ATTR = Key.create("tailTextSmall");

    public static final Object FORCE_QUALIFY = Key.create("FORCE_QUALIFY");

    public static final Object CASE_INSENSITIVE = Key.create("CASE_INSENSITIVE");

    public static final Key<TailType> TAIL_TYPE_ATTR = Key.create("myTailType"); // one of constants defined in SimpleTailType interface

    private Object myObject;
    private String myLookupString;
    private InsertHandler myInsertHandler;
    private double myPriority;
    private Map<Object, Object> myAttributes = null;
    public static final LookupItem[] EMPTY_ARRAY = new LookupItem[0];
    private final Set<String> myAllLookupStrings = new HashSet<>();
    private String myPresentable;
    private AutoCompletionPolicy myAutoCompletionPolicy = AutoCompletionPolicy.SETTINGS_DEPENDENT;

    /**
     * @deprecated use {@link LookupElementBuilder}
     */
    public LookupItem(T o, @Nonnull String lookupString) {
        setObject(o);
        setLookupString(lookupString);
    }

    public static LookupItem fromString(String s) {
        return new LookupItem<>(s, s);
    }

    public void setObject(@Nonnull T o) {
        myObject = o;

        if (o instanceof LookupValueWithPriority lookupValueWithPriority) {
            setPriority(lookupValueWithPriority.getPriority());
        }
    }

    @Override
    public boolean equals(Object o) {
        return o == this
            || o instanceof LookupItem item
            && Comparing.equal(myObject, item.myObject)
            && Comparing.equal(myLookupString, item.myLookupString)
            && Comparing.equal(myAllLookupStrings, item.myAllLookupStrings)
            && Comparing.equal(myAttributes, item.myAttributes);
    }

    @Override
    public int hashCode() {
        Object object = getObject();
        assert object != this : getClass().getName();
        return myAllLookupStrings.hashCode() * 239 + object.hashCode();
    }

    @Override
    public String toString() {
        return getLookupString();
    }

    /**
     * Returns a data object.  This object is used e.g. for rendering the node.
     */
    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T)myObject;
    }

    /**
     * Returns a string which will be inserted to the editor when this item is
     * choosen.
     */
    @Override
    @Nonnull
    public String getLookupString() {
        return myLookupString;
    }

    public void setLookupString(@Nonnull String lookupString) {
        if (myAllLookupStrings.contains("")) {
            myAllLookupStrings.remove("");
        }
        myLookupString = lookupString;
        myAllLookupStrings.add(lookupString);
    }

    public Object getAttribute(Object key) {
        if (myAttributes != null) {
            return myAttributes.get(key);
        }
        else {
            return null;
        }
    }

    public <T> T getAttribute(Key<T> key) {
        if (myAttributes != null) {
            //noinspection unchecked
            return (T)myAttributes.get(key);
        }
        else {
            return null;
        }
    }

    public void setAttribute(Object key, Object value) {
        if (myAttributes == null) {
            myAttributes = new HashMap<>(5);
        }
        myAttributes.put(key, value);
    }

    public <T> void setAttribute(Key<T> key, T value) {
        if (value == null && myAttributes != null) {
            myAttributes.remove(key);
            return;
        }

        if (myAttributes == null) {
            myAttributes = new HashMap<>(5);
        }
        myAttributes.put(key, value);
    }

    @Override
    public InsertHandler<? extends LookupItem> getInsertHandler() {
        return myInsertHandler;
    }

    @Override
    public boolean isBold() {
        return getAttribute(HIGHLIGHTED_ATTR) != null;
    }

    @Override
    @RequiredUIAccess
    public void handleInsert(@Nonnull InsertionContext context) {
        InsertHandler<? extends LookupElement> handler = getInsertHandler();
        if (handler != null) {
            //noinspection unchecked
            ((InsertHandler)handler).handleInsert(context, this);
        }
        if (getTailType() != TailType.UNKNOWN && myInsertHandler == null) {
            context.setAddCompletionChar(false);
            TailType type = handleCompletionChar(context.getEditor(), this, context.getCompletionChar());
            type.processTail(context.getEditor(), context.getTailOffset());
        }
    }

    @Nullable
    public static TailType getDefaultTailType(char completionChar) {
        return switch (completionChar) {
            case '.' -> new CharTailType('.', false);
            case ',' -> CommaTailType.INSTANCE;
            case ';' -> TailType.SEMICOLON;
            case '=' -> EqTailType.INSTANCE;
            case ' ' -> TailType.SPACE;
            case ':' -> TailType.CASE_COLON; //?
            default -> null;
        };
    }

    @Nonnull
    public static TailType handleCompletionChar(
        @Nonnull Editor editor,
        @Nonnull LookupElement lookupElement,
        char completionChar
    ) {
        TailType type = getDefaultTailType(completionChar);
        if (type != null) {
            return type;
        }

        if (lookupElement instanceof LookupItem) {
            LookupItem<?> item = (LookupItem)lookupElement;
            TailType attr = item.getAttribute(TAIL_TYPE_ATTR);
            if (attr != null) {
                return attr;
            }
        }
        return TailType.NONE;
    }


    @Nonnull
    public TailType getTailType() {
        TailType tailType = getAttribute(TAIL_TYPE_ATTR);
        return tailType != null ? tailType : TailType.UNKNOWN;
    }

    @Override
    @Nonnull
    public LookupItem<T> setTailType(@Nonnull TailType type) {
        setAttribute(TAIL_TYPE_ATTR, type);
        return this;
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        if (o instanceof String) {
            return getLookupString().compareTo((String)o);
        }
        if (!(o instanceof LookupItem)) {
            throw new RuntimeException("Trying to compare LookupItem with " + o.getClass() + "!!!");
        }
        return getLookupString().compareTo(((LookupItem)o).getLookupString());
    }

    @Override
    public LookupItem<T> setInsertHandler(@Nonnull InsertHandler<? extends LookupElement> handler) {
        myInsertHandler = handler;
        return this;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        for (ElementLookupRenderer renderer : ElementLookupRenderer.EP_NAME.getExtensionList()) {
            if (renderer.handlesItem(getObject())) {
                renderer.renderElement(this, getObject(), presentation);
                return;
            }
        }
        DefaultLookupItemRenderer.INSTANCE.renderElement(this, presentation);
    }

    @Override
    public LookupItem<T> setBold() {
        setAttribute(HIGHLIGHTED_ATTR, "");
        return this;
    }

    public LookupItem<T> forceQualify() {
        setAttribute(FORCE_QUALIFY, "");
        return this;
    }

    @Override
    public LookupItem<T> setAutoCompletionPolicy(AutoCompletionPolicy policy) {
        myAutoCompletionPolicy = policy;
        return this;
    }

    @Override
    public AutoCompletionPolicy getAutoCompletionPolicy() {
        return myAutoCompletionPolicy;
    }

    @Override
    @Nonnull
    public LookupItem<T> setIcon(Icon icon) {
        setAttribute(ICON_ATTR, icon);
        return this;
    }

    @Override
    @Nonnull
    public LookupItem<T> setPriority(double priority) {
        myPriority = priority;
        return this;
    }

    public final double getPriority() {
        return myPriority;
    }

    @Override
    @Nonnull
    public LookupItem<T> setPresentableText(@Nonnull String displayText) {
        myPresentable = displayText;
        return this;
    }

    @Nullable
    public String getPresentableText() {
        return myPresentable;
    }

    @Override
    @Nonnull
    public LookupItem<T> setTypeText(String text) {
        setAttribute(TYPE_TEXT_ATTR, text);
        return this;
    }

    @Nonnull
    @Override
    public MutableLookupElement<T> setTailText(String text, boolean grayed) {
        setAttribute(TAIL_TEXT_ATTR, text);
        setAttribute(TAIL_TEXT_SMALL_ATTR, Boolean.TRUE);
        return this;
    }

    @Override
    @Nonnull
    public LookupItem<T> setCaseSensitive(boolean caseSensitive) {
        setAttribute(CASE_INSENSITIVE, !caseSensitive);
        return this;
    }

    @Override
    public LookupItem<T> addLookupStrings(String... additionalLookupStrings) {
        ContainerUtil.addAll(myAllLookupStrings, additionalLookupStrings);
        return this;
    }

    @Override
    public Set<String> getAllLookupStrings() {
        return myAllLookupStrings;
    }

    @Override
    public boolean isCaseSensitive() {
        return !Boolean.TRUE.equals(getAttribute(CASE_INSENSITIVE));
    }
}
