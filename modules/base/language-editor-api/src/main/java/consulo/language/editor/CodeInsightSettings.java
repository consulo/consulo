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

package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.action.SmartBackspaceMode;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializationException;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.OptionTag;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.MagicConstant;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Singleton
@State(name = "CodeInsightSettings", storages = @Storage("editor.codeinsight.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class CodeInsightSettings implements PersistentStateComponent<Element>, Cloneable {
  private static final Logger LOG = Logger.getInstance(CodeInsightSettings.class);

  public static CodeInsightSettings getInstance() {
    return Application.get().getInstance(CodeInsightSettings.class);
  }

  @Override
  @Nullable
  public CodeInsightSettings clone() {
    try {
      return (CodeInsightSettings)super.clone();
    }
    catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public boolean AUTO_POPUP_PARAMETER_INFO = true;
  public int PARAMETER_INFO_DELAY = 1000;
  public boolean AUTO_POPUP_JAVADOC_INFO = false;
  public int JAVADOC_INFO_DELAY = 1000;
  public boolean AUTO_POPUP_COMPLETION_LOOKUP = true;

  @MagicConstant(intValues = {ALL, NONE, FIRST_LETTER})
  public int COMPLETION_CASE_SENSITIVE = FIRST_LETTER;
  public static final int ALL = 1;
  public static final int NONE = 2;
  public static final int FIRST_LETTER = 3;

  public boolean SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = false;
  public boolean AUTOCOMPLETE_ON_CODE_COMPLETION = true;
  public boolean AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = true;
  @Deprecated
  public boolean AUTOCOMPLETE_ON_CLASS_NAME_COMPLETION = false;
  public boolean AUTOCOMPLETE_COMMON_PREFIX = true;
  public boolean SHOW_STATIC_AFTER_INSTANCE = false;

  public boolean SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION;
  public boolean SHOW_FULL_SIGNATURES_IN_PARAMETER_INFO = false;

  @OptionTag("SMART_BACKSPACE") // explicit name makes it work also for obfuscated private field's name
  private int SMART_BACKSPACE = SmartBackspaceMode.AUTOINDENT.ordinal();

  public boolean SMART_INDENT_ON_ENTER = true;
  public boolean INSERT_BRACE_ON_ENTER = true;
  public boolean INSERT_SCRIPTLET_END_ON_ENTER = true;
  public boolean JAVADOC_STUB_ON_ENTER = true;
  public boolean SMART_END_ACTION = true;

  public boolean SURROUND_SELECTION_ON_QUOTE_TYPED = false;

  public boolean AUTOINSERT_PAIR_BRACKET = true;
  public boolean AUTOINSERT_PAIR_QUOTE = true;
  public boolean REFORMAT_BLOCK_ON_RBRACE = true;

  public static final int NO_REFORMAT = 1;
  public static final int INDENT_BLOCK = 2;
  public static final int INDENT_EACH_LINE = 3;
  public static final int REFORMAT_BLOCK = 4;

  @MagicConstant(intValues = {NO_REFORMAT, INDENT_BLOCK, INDENT_EACH_LINE, REFORMAT_BLOCK})
  public int REFORMAT_ON_PASTE = INDENT_EACH_LINE;

  public boolean INDENT_TO_CARET_ON_PASTE = false;

  @MagicConstant(intValues = {YES, NO, ASK})
  public int ADD_IMPORTS_ON_PASTE = ASK;
  public static final int YES = 1;
  public static final int NO = 2;
  public static final int ASK = 3;

  public boolean HIGHLIGHT_BRACES = true;
  public boolean HIGHLIGHT_SCOPE = false;

  public boolean HIGHLIGHT_IDENTIFIER_UNDER_CARET = true;

  public boolean OPTIMIZE_IMPORTS_ON_THE_FLY = false;
  public boolean ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY = false;
  public boolean ADD_MEMBER_IMPORTS_ON_THE_FLY = true;
  public boolean JSP_ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY = false;

  @Property(surroundWithTag = false)
  @AbstractCollection(surroundWithTag = false, elementTag = "EXCLUDED_PACKAGE", elementValueAttribute = "NAME")
  public String[] EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY;

  public boolean TAB_EXITS_BRACKETS_AND_QUOTES = true;

  public boolean isSelectAutopopupSuggestionsByChars() {
    return SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS;
  }

  @Transient
  @Nonnull
  public SmartBackspaceMode getBackspaceMode() {
    SmartBackspaceMode[] values = SmartBackspaceMode.values();
    return SMART_BACKSPACE >= 0 && SMART_BACKSPACE < values.length ? values[SMART_BACKSPACE] : SmartBackspaceMode.OFF;
  }

  @Transient
  public void setBackspaceMode(@Nonnull SmartBackspaceMode mode) {
    SMART_BACKSPACE = mode.ordinal();
  }

  @Override
  public void loadState(final Element state) {
    try {
      XmlSerializer.deserializeInto(this, state);
    }
    catch (XmlSerializationException e) {
      LOG.info(e);
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    writeExternal(element);
    return element;
  }

  public void writeExternal(final Element element) {
    try {
      XmlSerializer.serializeInto(this, element, new SkipDefaultValuesSerializationFilters());
    }
    catch (XmlSerializationException e) {
      LOG.info(e);
    }
  }
}
