/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.template;

import consulo.component.persist.scheme.SchemeElement;
import consulo.language.ast.IElementType;
import consulo.language.editor.internal.TemplateConstants;
import consulo.language.editor.template.Expression;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateOptionalProcessor;
import consulo.language.editor.template.Variable;
import consulo.language.editor.template.context.TemplateContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class TemplateImpl implements Template, SchemeElement {
    private static record Segment(String name, int offset) {
    }

    private String myKey;
    private String myString = null;
    private String myDescription;
    private String myGroupName;
    private char myShortcutChar = TemplateConstants.DEFAULT_CHAR;
    private final ArrayList<Variable> myVariables = new ArrayList<>();
    private ArrayList<Segment> mySegments = null;
    private String myTemplateText = null;
    private String myId;
    private Map<String, Boolean> myOptions = new LinkedHashMap<>();

    private boolean myIsToReformat = false;
    private boolean myToParseSegments = true;
    private TemplateContext myTemplateContext = new TemplateContext();
    private boolean myIsToIndent = true;
    private boolean myIsInline = false;

    private boolean isDeactivated = false;

    public TemplateImpl(@Nonnull String key, String group) {
        this(key, null, group);
        myToParseSegments = false;
        myTemplateText = "";
        mySegments = new ArrayList<>();
    }

    public TemplateImpl(@Nonnull String key, String string, String group) {
        myKey = key;
        myString = string;
        myGroupName = group;
    }

    public boolean isInline() {
        return myIsInline;
    }

    @Override
    public void setInline(boolean isInline) {
        myIsInline = isInline;
    }

    @Override
    public void addTextSegment(@Nonnull String text) {
        text = StringUtil.convertLineSeparators(text);
        myTemplateText += text;
    }

    @Override
    public void addVariableSegment(String name) {
        mySegments.add(new Segment(name, myTemplateText.length()));
    }

    @Override
    public Variable addVariable(Expression expression, boolean isAlwaysStopAt) {
        return addVariable("__Variable" + myVariables.size(), expression, isAlwaysStopAt);
    }

    @Override
    public Variable addVariable(String name, Expression expression, Expression defaultValueExpression, boolean isAlwaysStopAt, boolean skipOnStart) {
        if (mySegments != null) {
            Segment segment = new Segment(name, myTemplateText.length());
            mySegments.add(segment);
        }
        Variable variable = new Variable(name, expression, defaultValueExpression, isAlwaysStopAt, skipOnStart);
        myVariables.add(variable);
        return variable;
    }

    @Override
    public void addEndVariable() {
        Segment segment = new Segment(END, myTemplateText.length());
        mySegments.add(segment);
    }

    @Override
    public void addSelectionStartVariable() {
        Segment segment = new Segment(SELECTION_START, myTemplateText.length());
        mySegments.add(segment);
    }

    @Override
    public void addSelectionEndVariable() {
        Segment segment = new Segment(SELECTION_END, myTemplateText.length());
        mySegments.add(segment);
    }

    @Override
    public String getId() {
        return myId;
    }

    @Nonnull
    @Override
    public TemplateImpl copy() {
        TemplateImpl template = new TemplateImpl(myKey, myString, myGroupName);
        template.myId = myId;
        template.myDescription = myDescription;
        template.myShortcutChar = myShortcutChar;
        template.myIsToReformat = myIsToReformat;
        template.myIsInline = myIsInline;
        template.myTemplateContext = myTemplateContext.createCopy();
        template.isDeactivated = isDeactivated;
        for (Variable variable : myVariables) {
            template.addVariable(variable.getName(), variable.getExpressionString(), variable.getDefaultValueString(), variable.isAlwaysStopAt());
        }
        template.myOptions.putAll(myOptions);
        return template;
    }

    @Override
    public boolean isToReformat() {
        return myIsToReformat;
    }

    @Override
    public void setToReformat(boolean toReformat) {
        myIsToReformat = toReformat;
    }

    @Override
    public void setToIndent(boolean toIndent) {
        myIsToIndent = toIndent;
    }

    public boolean isToIndent() {
        return myIsToIndent;
    }

    @Override
    public void setDeactivated(boolean isDeactivated) {
        this.isDeactivated = isDeactivated;
    }

    @Override
    public boolean isDeactivated() {
        return isDeactivated;
    }

    @Nonnull
    @Override
    public TemplateContext getTemplateContext() {
        return myTemplateContext;
    }

    @Override
    public int getEndSegmentNumber() {
        return getVariableSegmentNumber(END);
    }

    @Override
    public int getSelectionStartSegmentNumber() {
        return getVariableSegmentNumber(SELECTION_START);
    }

    @Override
    public int getSelectionEndSegmentNumber() {
        return getVariableSegmentNumber(SELECTION_END);
    }

    public int getVariableSegmentNumber(String variableName) {
        parseSegments();
        for (int i = 0; i < mySegments.size(); i++) {
            Segment segment = mySegments.get(i);
            if (segment.name.equals(variableName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getTemplateText() {
        parseSegments();
        return myTemplateText;
    }

    @Override
    public String getSegmentName(int i) {
        parseSegments();
        return mySegments.get(i).name;
    }

    @Override
    public int getSegmentOffset(int i) {
        parseSegments();
        return mySegments.get(i).offset;
    }

    @Override
    public int getSegmentsCount() {
        parseSegments();
        return mySegments.size();
    }

    @Override
    public void parseSegments() {
        if (!myToParseSegments) {
            return;
        }
        if (mySegments != null) {
            return;
        }

        if (myString == null) {
            myString = "";
        }
        myString = StringUtil.convertLineSeparators(myString);
        mySegments = new ArrayList<>();
        StringBuilder buffer = new StringBuilder("");
        TemplateTextLexer lexer = new TemplateTextLexer();
        lexer.start(myString);

        while (true) {
            IElementType tokenType = lexer.getTokenType();
            if (tokenType == null) {
                break;
            }
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            String token = myString.substring(start, end);
            if (tokenType == TemplateTokenType.VARIABLE) {
                String name = token.substring(1, token.length() - 1);
                Segment segment = new Segment(name, buffer.length());
                mySegments.add(segment);
            }
            else if (tokenType == TemplateTokenType.ESCAPE_DOLLAR) {
                buffer.append("$");
            }
            else {
                buffer.append(token);
            }
            lexer.advance();
        }
        myTemplateText = buffer.toString();
    }

    public void removeAllParsed() {
        myVariables.clear();
        mySegments = null;
    }

    @Override
    public Variable addVariable(String name, String expression, String defaultValue, boolean isAlwaysStopAt) {
        Variable variable = new Variable(name, expression, defaultValue, isAlwaysStopAt);
        myVariables.add(variable);
        return variable;
    }

    @Override
    public void removeVariable(int i) {
        myVariables.remove(i);
    }

    @Override
    public int getVariableCount() {
        return myVariables.size();
    }

    @Override
    public String getVariableNameAt(int i) {
        return myVariables.get(i).getName();
    }

    @Override
    public String getExpressionStringAt(int i) {
        return myVariables.get(i).getExpressionString();
    }

    @Override
    public Expression getExpressionAt(int i) {
        return myVariables.get(i).getExpression();
    }

    @Override
    public String getDefaultValueStringAt(int i) {
        return myVariables.get(i).getDefaultValueString();
    }

    @Override
    public Expression getDefaultValueAt(int i) {
        return myVariables.get(i).getDefaultValueExpression();
    }

    @Override
    public boolean isAlwaysStopAt(int i) {
        return myVariables.get(i).isAlwaysStopAt();
    }

    @Override
    public String getKey() {
        return myKey;
    }

    @Override
    public void setKey(String key) {
        myKey = key;
    }

    @Override
    public String getString() {
        parseSegments();
        return myString;
    }

    @Override
    public void setString(String string) {
        myString = string;
    }

    @Override
    public String getDescription() {
        return myDescription;
    }

    @Override
    public void setDescription(String description) {
        myDescription = description;
    }

    @Override
    public char getShortcutChar() {
        return myShortcutChar;
    }

    @Override
    public void setShortcutChar(char shortcutChar) {
        myShortcutChar = shortcutChar;
    }

    @Override
    public String getGroupName() {
        return myGroupName;
    }

    @Override
    public void setGroupName(String groupName) {
        myGroupName = groupName;
    }

    @Override
    public boolean isSelectionTemplate() {
        for (Variable v : myVariables) {
            if (v.getName().equals(SELECTION)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean getOption(@Nonnull KeyWithDefaultValue<Boolean> key) {
        return myOptions.getOrDefault(key.toString(), key.getDefaultValue());
    }

    @Override
    public boolean containsOption(@Nonnull KeyWithDefaultValue<Boolean> key) {
        return myOptions.containsKey(key.toString());
    }

    @Override
    public void setOption(@Nonnull KeyWithDefaultValue<Boolean> key, boolean value) {
        myOptions.put(key.toString(), value);
    }

    public Map<String, Boolean> getOptions() {
        return myOptions;
    }

    public void setOptionViaString(String key, boolean value) {
        myOptions.put(key, value);
    }

    public boolean hasArgument() {
        for (Variable v : myVariables) {
            if (v.getName().equals(ARG)) {
                return true;
            }
        }
        return false;
    }

    public void setId(@Nullable String id) {
        myId = id;
    }

    public Map<TemplateContextType, Boolean> createContext() {
        Map<TemplateContextType, Boolean> context = new LinkedHashMap<>();
        TemplateContextType.EP_NAME.forEachExtensionSafe(it -> context.put(it, getTemplateContext().isEnabled(it)));
        return context;
    }

    public boolean contextsEqual(TemplateImpl defaultTemplate) {
        return getTemplateContext().getDifference(defaultTemplate.getTemplateContext()).isEmpty();
    }

    public void applyOptions(Map<TemplateOptionalProcessor, Boolean> context) {
        for (Map.Entry<TemplateOptionalProcessor, Boolean> entry : context.entrySet()) {
            entry.getKey().setEnabled(this, entry.getValue());
        }
    }

    public void applyContext(Map<TemplateContextType, Boolean> context) {
        for (Map.Entry<TemplateContextType, Boolean> entry : context.entrySet()) {
            getTemplateContext().setEnabled(entry.getKey(), entry.getValue());
        }
    }

    public boolean skipOnStart(int i) {
        return myVariables.get(i).skipOnStart();
    }

    public ArrayList<Variable> getVariables() {
        return myVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemplateImpl template)) {
            return false;
        }

        //noinspection SimplifiableIfStatement
        if (myId != null && template.myId != null && myId.equals(template.myId)) {
            return true;
        }

        return myIsToReformat == template.myIsToReformat
            && myShortcutChar == template.myShortcutChar
            && Objects.equals(myDescription, template.myDescription)
            && Objects.equals(myGroupName, template.myGroupName)
            && Objects.equals(myKey, template.myKey)
            && Objects.equals(myString, template.myString)
            && Objects.equals(myTemplateText, template.myTemplateText)
            && new HashSet<>(myVariables).equals(new HashSet<>(template.myVariables))
            && isDeactivated == template.isDeactivated
            && myOptions.equals(template.myOptions);
    }

    @Override
    public int hashCode() {
        if (myId != null) {
            return myId.hashCode();
        }
        int result;
        result = myKey.hashCode();
        result = 29 * result + (myString == null ? 0 : myString.hashCode());
        result = 29 * result + myGroupName.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return myGroupName + "/" + myKey;
    }
}
