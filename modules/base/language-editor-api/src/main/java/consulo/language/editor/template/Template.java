/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.template;

import consulo.language.editor.template.context.TemplateContext;

import jakarta.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public abstract class Template {
  public static final String END = "END";
  public static final String SELECTION = "SELECTION";
  public static final String SELECTION_START = "SELECTION_START";
  public static final String SELECTION_END = "SELECTION_END";
  public static final String ARG = "ARG";

  public static final Set<String> INTERNAL_VARS_SET = Set.of(END, SELECTION, SELECTION_START, SELECTION_END);

  public enum Property {
    USE_STATIC_IMPORT_IF_POSSIBLE
  }

  private static final Map<Property, Boolean> DEFAULT_PROPERTIES = new EnumMap<Property, Boolean>(Property.class);

  static {
    DEFAULT_PROPERTIES.put(Property.USE_STATIC_IMPORT_IF_POSSIBLE, false);
  }

  private final Map<Property, Boolean> myProperties = new EnumMap<Property, Boolean>(Property.class);

  public abstract void addTextSegment(@Nonnull String text);

  public abstract void addVariableSegment(String name);

  public Variable addVariable(String name, @Nonnull Expression defaultValueExpression, boolean isAlwaysStopAt) {
    return addVariable(name, defaultValueExpression, defaultValueExpression, isAlwaysStopAt);
  }

  public abstract Variable addVariable(Expression expression, boolean isAlwaysStopAt);

  public Variable addVariable(String name, Expression expression, Expression defaultValueExpression, boolean isAlwaysStopAt) {
    return addVariable(name, expression, defaultValueExpression, isAlwaysStopAt, false);
  }

  public abstract Variable addVariable(String name, Expression expression, Expression defaultValueExpression, boolean isAlwaysStopAt, boolean skipOnStart);

  public abstract Variable addVariable(String name, String expression, String defaultValueExpression, boolean isAlwaysStopAt);

  public abstract void addEndVariable();

  public abstract void addSelectionStartVariable();

  public abstract void addSelectionEndVariable();

  public abstract String getId();

  public abstract String getKey();

  public abstract void setKey(String key);

  public abstract String getDescription();

  public abstract void setDescription(String description);

  public abstract void setToReformat(boolean toReformat);

  public abstract void setToIndent(boolean toIndent);

  public abstract void setInline(boolean isInline);

  public abstract int getSegmentsCount();

  public abstract String getSegmentName(int segmentIndex);

  public abstract int getSegmentOffset(int segmentIndex);

  public abstract String getTemplateText();

  public abstract String getGroupName();

  public abstract boolean isToShortenLongNames();

  public abstract void setToShortenLongNames(boolean toShortenLongNames);

  public abstract boolean isDeactivated();

  public abstract int getEndSegmentNumber();

  public abstract boolean isSelectionTemplate();

  public abstract int getSelectionStartSegmentNumber();

  public abstract int getSelectionEndSegmentNumber();

  public abstract void setDeactivated(boolean isDeactivated);

  public abstract boolean isToReformat();

  public abstract char getShortcutChar();

  public abstract void setShortcutChar(char shortcutChar);

  public abstract int getVariableCount();

  public abstract void removeVariable(int i);

  public abstract String getVariableNameAt(int i);

  public abstract String getExpressionStringAt(int i);

  public abstract Expression getExpressionAt(int i);

  public abstract String getDefaultValueStringAt(int i);

  public abstract boolean isAlwaysStopAt(int i);

  public abstract Expression getDefaultValueAt(int i);

  public abstract void setString(String string);

  public abstract String getString();

  @Nonnull
  public abstract Template copy();

  @Nonnull
  public abstract TemplateContext getTemplateContext();

  public boolean getValue(@Nonnull Property key) {
    Boolean result = myProperties.get(key);
    return result == null ? getDefaultValue(key) : result;
  }

  public void setValue(@Nonnull Property key, boolean value) {
    myProperties.put(key, value);
  }

  public static boolean getDefaultValue(@Nonnull Property key) {
    Boolean result = DEFAULT_PROPERTIES.get(key);
    return result == null ? false : result;
  }
}
