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
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;

import java.util.Set;

public interface Template {
    String END = "END";
    String SELECTION = "SELECTION";
    String SELECTION_START = "SELECTION_START";
    String SELECTION_END = "SELECTION_END";
    String ARG = "ARG";

    Set<String> INTERNAL_VARS_SET = Set.of(END, SELECTION, SELECTION_START, SELECTION_END);

    void addTextSegment(@Nonnull String text);

    void addVariableSegment(String name);

    default Variable addVariable(String name, @Nonnull Expression defaultValueExpression, boolean isAlwaysStopAt) {
        return addVariable(name, defaultValueExpression, defaultValueExpression, isAlwaysStopAt);
    }

    Variable addVariable(Expression expression, boolean isAlwaysStopAt);

    default Variable addVariable(String name, Expression expression, Expression defaultValueExpression, boolean isAlwaysStopAt) {
        return addVariable(name, expression, defaultValueExpression, isAlwaysStopAt, false);
    }

    Variable addVariable(String name, Expression expression, Expression defaultValueExpression, boolean isAlwaysStopAt, boolean skipOnStart);

    Variable addVariable(String name, String expression, String defaultValueExpression, boolean isAlwaysStopAt);

    void addEndVariable();

    void addSelectionStartVariable();

    void addSelectionEndVariable();

    String getId();

    String getKey();

    void setKey(String key);

    String getDescription();

    void setDescription(String description);

    void setToReformat(boolean toReformat);

    void setToIndent(boolean toIndent);

    void setInline(boolean isInline);

    int getSegmentsCount();

    void parseSegments();

    String getSegmentName(int segmentIndex);

    int getSegmentOffset(int segmentIndex);

    String getTemplateText();

    String getGroupName();

    boolean isDeactivated();

    int getEndSegmentNumber();

    boolean isSelectionTemplate();

    int getSelectionStartSegmentNumber();

    int getSelectionEndSegmentNumber();

    void setDeactivated(boolean isDeactivated);

    boolean isToReformat();

    char getShortcutChar();

    void setShortcutChar(char shortcutChar);

    int getVariableCount();

    void removeVariable(int i);

    String getVariableNameAt(int i);

    String getExpressionStringAt(int i);

    Expression getExpressionAt(int i);

    String getDefaultValueStringAt(int i);

    boolean isAlwaysStopAt(int i);

    Expression getDefaultValueAt(int i);

    void setString(String string);

    String getString();

    @Nonnull
    Template copy();

    @Nonnull
    TemplateContext getTemplateContext();

    void setOption(@Nonnull KeyWithDefaultValue<Boolean> key, boolean value);

    boolean getOption(@Nonnull KeyWithDefaultValue<Boolean> key);

    boolean containsOption(@Nonnull KeyWithDefaultValue<Boolean> key);
}
