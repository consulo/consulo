/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.codeStyle.setting;

import consulo.annotation.DeprecationInfo;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable.*;

/**
 * @author Roman.Shein
 * @since 2015-09-15
 */
public class CodeStyleSettingPresentation {
    public static class SettingsGroup {
        public final LocalizeValue name;

        public SettingsGroup(LocalizeValue name) {
            this.name = name;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof SettingsGroup that
                && name.isNotEmpty()
                && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        public boolean isNull() {
            return name.isEmpty();
        }
    }

    protected String myFieldName;

    protected LocalizeValue myUiName;

    public CodeStyleSettingPresentation(String fieldName, LocalizeValue uiName) {
        myFieldName = fieldName;
        myUiName = uiName;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public CodeStyleSettingPresentation(String fieldName, String uiName) {
        this(fieldName, LocalizeValue.of(uiName));
    }

    public String getFieldName() {
        return myFieldName;
    }

    public LocalizeValue getUiName() {
        return myUiName;
    }

    public void setUiName(LocalizeValue newName) {
        myUiName = newName;
    }

    public LocalizeValue getValueUiName(Object value) {
        return value instanceof LocalizeValue lv ? lv : LocalizeValue.of(value.toString());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CodeStyleSettingPresentation presentation && presentation.getFieldName().equals(getFieldName());
    }

    @Override
    public int hashCode() {
        return myFieldName.hashCode();
    }

    protected static void putGroupTop(
        Map<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> result,
        String fieldName,
        LocalizeValue uiName,
        int[] values,
        LocalizeValue[] valueUiNames
    ) {
        result.put(
            new SettingsGroup(LocalizeValue.empty()),
            List.of(new CodeStyleSelectSettingPresentation(fieldName, uiName, values, valueUiNames))
        );
    }

    protected static final Map<SettingsGroup, List<CodeStyleSettingPresentation>> BLANK_LINES_STANDARD_SETTINGS;
    protected static final Map<SettingsGroup, List<CodeStyleSettingPresentation>> SPACING_STANDARD_SETTINGS;
    protected static final Map<SettingsGroup, List<CodeStyleSettingPresentation>> WRAPPING_AND_BRACES_STANDARD_SETTINGS;
    protected static final Map<SettingsGroup, List<CodeStyleSettingPresentation>> INDENT_STANDARD_SETTINGS;

    static {
        //-----------------------------------BLANK_LINES_SETTINGS-----------------------------------------------------

        Map<SettingsGroup, List<CodeStyleSettingPresentation>> result = new LinkedHashMap<>();
        result.put(
            new SettingsGroup(BLANK_LINES_KEEP),
            List.of(
                new CodeStyleSettingPresentation(
                    "KEEP_BLANK_LINES_IN_DECLARATIONS",
                    CodeStyleLocalize.editboxKeepBlanklinesInDeclarations()
                ),
                new CodeStyleSettingPresentation("KEEP_BLANK_LINES_IN_CODE", CodeStyleLocalize.editboxKeepBlanklinesInCode()),
                new CodeStyleSettingPresentation("KEEP_BLANK_LINES_BEFORE_RBRACE", CodeStyleLocalize.editboxKeepBlanklinesBeforeRbrace())
            )
        );

        result.put(
            new SettingsGroup(BLANK_LINES),
            List.of(
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_BEFORE_PACKAGE",
                    CodeStyleLocalize.editboxBlanklinesBeforePackageStatement()
                ),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_AFTER_PACKAGE",
                    CodeStyleLocalize.editboxBlanklinesAfterPackageStatement()
                ),
                new CodeStyleSettingPresentation("BLANK_LINES_BEFORE_IMPORTS", CodeStyleLocalize.editboxBlanklinesBeforeImports()),
                new CodeStyleSettingPresentation("BLANK_LINES_AFTER_IMPORTS", CodeStyleLocalize.editboxBlanklinesAfterImports()),
                new CodeStyleSettingPresentation("BLANK_LINES_AROUND_CLASS", CodeStyleLocalize.editboxBlanklinesAroundClass()),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_AFTER_CLASS_HEADER",
                    CodeStyleLocalize.editboxBlanklinesAfterClassHeader()
                ),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_BEFORE_CLASS_END",
                    CodeStyleLocalize.editboxBlanklinesBeforeClassEnd()
                ),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_AFTER_ANONYMOUS_CLASS_HEADER",
                    CodeStyleLocalize.editboxBlanklinesAfterAnonymousClassHeader()
                ),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_AROUND_FIELD_IN_INTERFACE",
                    CodeStyleLocalize.editboxBlanklinesAroundFieldInInterface()
                ),
                new CodeStyleSettingPresentation("BLANK_LINES_AROUND_FIELD", CodeStyleLocalize.editboxBlanklinesAroundField()),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_AROUND_METHOD_IN_INTERFACE",
                    CodeStyleLocalize.editboxBlanklinesAroundMethodInInterface()
                ),
                new CodeStyleSettingPresentation("BLANK_LINES_AROUND_METHOD", CodeStyleLocalize.editboxBlanklinesAroundMethod()),
                new CodeStyleSettingPresentation(
                    "BLANK_LINES_BEFORE_METHOD_BODY",
                    CodeStyleLocalize.editboxBlanklinesBeforeMethodBody()
                )
            )
        );
        BLANK_LINES_STANDARD_SETTINGS = Collections.unmodifiableMap(result);

        //-----------------------------------SPACING_SETTINGS-----------------------------------------------------

        result = new LinkedHashMap<>();
        result.put(
            new SettingsGroup(SPACES_BEFORE_PARENTHESES),
            List.of(
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_METHOD_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesMethodDeclarationParentheses()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_METHOD_CALL_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesMethodCallParentheses()
                ),
                new CodeStyleSettingPresentation("SPACE_BEFORE_IF_PARENTHESES", CodeStyleLocalize.checkboxSpacesIfParentheses()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_FOR_PARENTHESES", CodeStyleLocalize.checkboxSpacesForParentheses()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_WHILE_PARENTHESES", CodeStyleLocalize.checkboxSpacesWhileParentheses()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_SWITCH_PARENTHESES", CodeStyleLocalize.checkboxSpacesSwitchParentheses()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_TRY_PARENTHESES", CodeStyleLocalize.checkboxSpacesTryParentheses()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_CATCH_PARENTHESES", CodeStyleLocalize.checkboxSpacesCatchParentheses()),
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_SYNCHRONIZED_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesSynchronizedParentheses()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_ANOTATION_PARAMETER_LIST",
                    CodeStyleLocalize.checkboxSpacesAnnotationParameters()
                )
            )
        );

        result.put(
            new SettingsGroup(SPACES_AROUND_OPERATORS),
            List.of(
                new CodeStyleSettingPresentation(
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                    CodeStyleLocalize.checkboxSpacesAssignmentOperators()
                ),
                new CodeStyleSettingPresentation("SPACE_AROUND_LOGICAL_OPERATORS", CodeStyleLocalize.checkboxSpacesLogicalOperators()),
                new CodeStyleSettingPresentation("SPACE_AROUND_EQUALITY_OPERATORS", CodeStyleLocalize.checkboxSpacesEqualityOperators()),
                new CodeStyleSettingPresentation(
                    "SPACE_AROUND_RELATIONAL_OPERATORS",
                    CodeStyleLocalize.checkboxSpacesRelationalOperators()
                ),
                new CodeStyleSettingPresentation("SPACE_AROUND_BITWISE_OPERATORS", CodeStyleLocalize.checkboxSpacesBitwiseOperators()),
                new CodeStyleSettingPresentation("SPACE_AROUND_ADDITIVE_OPERATORS", CodeStyleLocalize.checkboxSpacesAdditiveOperators()),
                new CodeStyleSettingPresentation(
                    "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                    CodeStyleLocalize.checkboxSpacesMultiplicativeOperators()
                ),
                new CodeStyleSettingPresentation("SPACE_AROUND_SHIFT_OPERATORS", CodeStyleLocalize.checkboxSpacesShiftOperators()),
                new CodeStyleSettingPresentation("SPACE_AROUND_UNARY_OPERATOR", CodeStyleLocalize.checkboxSpacesAroundUnaryOperator()),
                new CodeStyleSettingPresentation("SPACE_AROUND_LAMBDA_ARROW", CodeStyleLocalize.checkboxSpacesAroundLambdaArrow()),
                new CodeStyleSettingPresentation(
                    "SPACE_AROUND_METHOD_REF_DBL_COLON",
                    CodeStyleLocalize.checkboxSpacesAroundMethodRefDblColonArrow()
                )
            )
        );

        result.put(
            new SettingsGroup(SPACES_BEFORE_LEFT_BRACE),
            List.of(
                new CodeStyleSettingPresentation("SPACE_BEFORE_CLASS_LBRACE", CodeStyleLocalize.checkboxSpacesClassLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_METHOD_LBRACE", CodeStyleLocalize.checkboxSpacesMethodLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_IF_LBRACE", CodeStyleLocalize.checkboxSpacesIfLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_ELSE_LBRACE", CodeStyleLocalize.checkboxSpacesElseLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_FOR_LBRACE", CodeStyleLocalize.checkboxSpacesForLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_WHILE_LBRACE", CodeStyleLocalize.checkboxSpacesWhileLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_DO_LBRACE", CodeStyleLocalize.checkboxSpacesDoLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_SWITCH_LBRACE", CodeStyleLocalize.checkboxSpacesSwitchLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_TRY_LBRACE", CodeStyleLocalize.checkboxSpacesTryLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_CATCH_LBRACE", CodeStyleLocalize.checkboxSpacesCatchLeftBrace()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_FINALLY_LBRACE", CodeStyleLocalize.checkboxSpacesFinallyLeftBrace()),
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_SYNCHRONIZED_LBRACE",
                    CodeStyleLocalize.checkboxSpacesSynchronizedLeftBrace()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE",
                    CodeStyleLocalize.checkboxSpacesArrayInitializerLeftBrace()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_BEFORE_ANNOTATION_ARRAY_INITIALIZER_LBRACE",
                    CodeStyleLocalize.checkboxSpacesAnnotationArrayInitializerLeftBrace()
                )
            )
        );

        result.put(
            new SettingsGroup(SPACES_BEFORE_KEYWORD),
            List.of(
                new CodeStyleSettingPresentation("SPACE_BEFORE_ELSE_KEYWORD", CodeStyleLocalize.checkboxSpacesElseKeyword()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_WHILE_KEYWORD", CodeStyleLocalize.checkboxSpacesWhileKeyword()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_CATCH_KEYWORD", CodeStyleLocalize.checkboxSpacesCatchKeyword()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_FINALLY_KEYWORD", CodeStyleLocalize.checkboxSpacesFinallyKeyword())
            )
        );

        result.put(
            new SettingsGroup(SPACES_WITHIN),
            List.of(
                new CodeStyleSettingPresentation("SPACE_WITHIN_BRACES", CodeStyleLocalize.checkboxSpacesWithinBraces()),
                new CodeStyleSettingPresentation("SPACE_WITHIN_BRACKETS", CodeStyleLocalize.checkboxSpacesWithinBrackets()),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_ARRAY_INITIALIZER_BRACES",
                    CodeStyleLocalize.checkboxSpacesWithinArrayInitializerBraces()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_EMPTY_ARRAY_INITIALIZER_BRACES",
                    CodeStyleLocalize.checkboxSpacesWithinEmptyArrayInitializerBraces()
                ),
                new CodeStyleSettingPresentation("SPACE_WITHIN_PARENTHESES", CodeStyleLocalize.checkboxSpacesWithinParentheses()),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_METHOD_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesCheckboxSpacesMethodDeclarationParentheses()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_EMPTY_METHOD_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesCheckboxSpacesEmptyMethodDeclarationParentheses()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_METHOD_CALL_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesCheckboxSpacesMethodCallParentheses()
                ),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesCheckboxSpacesEmptyMethodCallParentheses()
                ),
                new CodeStyleSettingPresentation("SPACE_WITHIN_IF_PARENTHESES", CodeStyleLocalize.checkboxSpacesIfParentheses()),
                new CodeStyleSettingPresentation("SPACE_WITHIN_FOR_PARENTHESES", CodeStyleLocalize.checkboxSpacesForParentheses()),
                new CodeStyleSettingPresentation("SPACE_WITHIN_WHILE_PARENTHESES", CodeStyleLocalize.checkboxSpacesWhileParentheses()),
                new CodeStyleSettingPresentation("SPACE_WITHIN_SWITCH_PARENTHESES", CodeStyleLocalize.checkboxSpacesSwitchParentheses()),
                new CodeStyleSettingPresentation("SPACE_WITHIN_TRY_PARENTHESES", CodeStyleLocalize.checkboxSpacesTryParentheses()),
                new CodeStyleSettingPresentation("SPACE_WITHIN_CATCH_PARENTHESES", CodeStyleLocalize.checkboxSpacesCatchParentheses()),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_SYNCHRONIZED_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesSynchronizedParentheses()
                ),
                new CodeStyleSettingPresentation("SPACE_WITHIN_CAST_PARENTHESES", CodeStyleLocalize.checkboxSpacesTypeCastParentheses()),
                new CodeStyleSettingPresentation(
                    "SPACE_WITHIN_ANNOTATION_PARENTHESES",
                    CodeStyleLocalize.checkboxSpacesAnnotationParentheses()
                )
            )
        );

        result.put(
            new SettingsGroup(SPACES_IN_TERNARY_OPERATOR),
            List.of(
                new CodeStyleSettingPresentation("SPACE_BEFORE_QUEST", CodeStyleLocalize.checkboxSpacesBeforeQuestion()),
                new CodeStyleSettingPresentation("SPACE_AFTER_QUEST", CodeStyleLocalize.checkboxSpacesAfterQuestion()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_COLON", CodeStyleLocalize.checkboxSpacesBeforeColon()),
                new CodeStyleSettingPresentation("SPACE_AFTER_COLON", CodeStyleLocalize.checkboxSpacesAfterColon())
            )
        );

        result.put(
            new SettingsGroup(SPACES_WITHIN_TYPE_ARGUMENTS),
            List.of(new CodeStyleSettingPresentation("SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS", CodeStyleLocalize.checkboxSpacesAfterComma()))
        );

        result.put(
            new SettingsGroup(SPACES_IN_TYPE_ARGUMENTS),
            List.of(new CodeStyleSettingPresentation(
                "SPACE_BEFORE_TYPE_PARAMETER_LIST",
                CodeStyleLocalize.checkboxSpacesBeforeOpeningAngleBracket()
            ))
        );

        result.put(new SettingsGroup(SPACES_IN_TYPE_PARAMETERS), List.of());

        result.put(
            new SettingsGroup(SPACES_OTHER),
            List.of(
                new CodeStyleSettingPresentation("SPACE_BEFORE_COMMA", CodeStyleLocalize.checkboxSpacesBeforeComma()),
                new CodeStyleSettingPresentation("SPACE_AFTER_COMMA", CodeStyleLocalize.checkboxSpacesAfterComma()),
                new CodeStyleSettingPresentation("SPACE_BEFORE_SEMICOLON", CodeStyleLocalize.checkboxSpacesBeforeSemicolon()),
                new CodeStyleSettingPresentation("SPACE_AFTER_SEMICOLON", CodeStyleLocalize.checkboxSpacesAfterSemicolon()),
                new CodeStyleSettingPresentation("SPACE_AFTER_TYPE_CAST", CodeStyleLocalize.checkboxSpacesAfterTypeCast())
            )
        );
        SPACING_STANDARD_SETTINGS = Collections.unmodifiableMap(result);

        //-----------------------------------WRAPPING_AND_BRACES_SETTINGS-----------------------------------------------------

        result = new LinkedHashMap<>();
        result.put(
            new SettingsGroup(LocalizeValue.empty()),
            List.of(
                new CodeStyleBoundedIntegerSettingPresentation(
                    "RIGHT_MARGIN",
                    CodeStyleLocalize.editboxRightMarginColumns(),
                    0,
                    999,
                    -1,
                    CodeStyleLocalize.settingsCodeStyleDefaultGeneral()
                ),
                new CodeStyleSelectSettingPresentation(
                    "WRAP_ON_TYPING",
                    CodeStyleLocalize.wrappingWrapOnTyping(),
                    WRAP_ON_TYPING_VALUES,
                    WRAP_ON_TYPING_OPTIONS
                ),
                new CodeStyleSoftMarginsPresentation()
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_KEEP),
            List.of(
                new CodeStyleSettingPresentation("KEEP_LINE_BREAKS", CodeStyleLocalize.wrappingKeepLineBreaks()),
                new CodeStyleSettingPresentation("KEEP_FIRST_COLUMN_COMMENT", CodeStyleLocalize.wrappingKeepCommentAtFirstColumn()),
                new CodeStyleSettingPresentation(
                    "KEEP_CONTROL_STATEMENT_IN_ONE_LINE",
                    CodeStyleLocalize.checkboxKeepWhenReformattingControlStatementInOneLine()
                ),
                new CodeStyleSettingPresentation(
                    "KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE",
                    CodeStyleLocalize.wrappingKeepMultipleExpressionsInOneLine()
                ),
                new CodeStyleSettingPresentation("KEEP_SIMPLE_BLOCKS_IN_ONE_LINE", CodeStyleLocalize.wrappingKeepSimpleBlocksInOneLine()),
                new CodeStyleSettingPresentation("KEEP_SIMPLE_METHODS_IN_ONE_LINE", CodeStyleLocalize.wrappingKeepSimpleMethodsInOneLine()),
                new CodeStyleSettingPresentation("KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE", CodeStyleLocalize.wrappingKeepSimpleLambdasInOneLine()),
                new CodeStyleSettingPresentation("KEEP_SIMPLE_CLASSES_IN_ONE_LINE", CodeStyleLocalize.wrappingKeepSimpleClassesInOneLine())
            )
        );

        result.put(
            new SettingsGroup(LocalizeValue.empty()),
            List.of(new CodeStyleSettingPresentation("WRAP_LONG_LINES", CodeStyleLocalize.wrappingLongLines()))
        );

        result.put(
            new SettingsGroup(WRAPPING_COMMENTS),
            List.of(new CodeStyleSettingPresentation("WRAP_COMMENTS", CodeStyleLocalize.wrappingCommentsWrapAtRightMargin()))
        );

        result.put(
            new SettingsGroup(WRAPPING_BRACES),
            List.of(
                new CodeStyleSelectSettingPresentation(
                    "CLASS_BRACE_STYLE",
                    CodeStyleLocalize.wrappingBracePlacementClassDeclaration(),
                    BRACE_PLACEMENT_VALUES,
                    BRACE_PLACEMENT_OPTIONS
                ),
                new CodeStyleSelectSettingPresentation(
                    "METHOD_BRACE_STYLE",
                    CodeStyleLocalize.wrappingBracePlacementMethodDeclaration(),
                    BRACE_PLACEMENT_VALUES,
                    BRACE_PLACEMENT_OPTIONS
                ),
                new CodeStyleSelectSettingPresentation(
                    "LAMBDA_BRACE_STYLE",
                    CodeStyleLocalize.wrappingBracePlacementLambda(),
                    BRACE_PLACEMENT_VALUES,
                    BRACE_PLACEMENT_OPTIONS
                ),
                new CodeStyleSelectSettingPresentation(
                    "BRACE_STYLE",
                    CodeStyleLocalize.wrappingBracePlacementOther(),
                    BRACE_PLACEMENT_VALUES,
                    BRACE_PLACEMENT_OPTIONS
                )
            )
        );

        putGroupTop(result, "EXTENDS_LIST_WRAP", WRAPPING_EXTENDS_LIST, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_EXTENDS_LIST),
            List.of(new CodeStyleSettingPresentation("ALIGN_MULTILINE_EXTENDS_LIST", CodeStyleLocalize.wrappingAlignWhenMultiline()))
        );

        putGroupTop(result, "EXTENDS_KEYWORD_WRAP", WRAPPING_EXTENDS_KEYWORD, WRAP_VALUES_FOR_SINGLETON, WRAP_OPTIONS_FOR_SINGLETON);

        putGroupTop(result, "THROWS_LIST_WRAP", WRAPPING_THROWS_LIST, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_THROWS_LIST),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_THROWS_LIST", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation("ALIGN_THROWS_KEYWORD", CodeStyleLocalize.wrappingAlignThrowsKeyword())
            )
        );

        putGroupTop(result, "THROWS_KEYWORD_WRAP", WRAPPING_THROWS_KEYWORD, WRAP_VALUES_FOR_SINGLETON, WRAP_OPTIONS_FOR_SINGLETON);

        putGroupTop(result, "METHOD_PARAMETERS_WRAP", WRAPPING_METHOD_PARAMETERS, WRAP_VALUES, WRAP_OPTIONS);
        result.put(new SettingsGroup(WRAPPING_METHOD_PARAMETERS), List.of(
            new CodeStyleSettingPresentation("ALIGN_MULTILINE_PARAMETERS", CodeStyleLocalize.wrappingAlignWhenMultiline()),
            new CodeStyleSettingPresentation("METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingNewLineAfterLpar()),
            new CodeStyleSettingPresentation("METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingRparOnNewLine())
        ));

        putGroupTop(result, "CALL_PARAMETERS_WRAP", WRAPPING_METHOD_ARGUMENTS_WRAPPING, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_METHOD_ARGUMENTS_WRAPPING),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_PARAMETERS_IN_CALLS", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation("PREFER_PARAMETERS_WRAP", CodeStyleLocalize.wrappingTakePriorityOverCallChainWrapping()),
                new CodeStyleSettingPresentation("CALL_PARAMETERS_LPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingNewLineAfterLpar()),
                new CodeStyleSettingPresentation("CALL_PARAMETERS_RPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingRparOnNewLine())
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_METHOD_PARENTHESES),
            List.of(new CodeStyleSettingPresentation("ALIGN_MULTILINE_METHOD_BRACKETS", CodeStyleLocalize.wrappingAlignWhenMultiline()))
        );

        putGroupTop(result, "METHOD_CALL_CHAIN_WRAP", WRAPPING_CALL_CHAIN, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_CALL_CHAIN),
            List.of(
                new CodeStyleSettingPresentation(
                    "WRAP_FIRST_METHOD_IN_CALL_CHAIN",
                    CodeStyleLocalize.wrappingChainedMethodCallFirstOnNewLine()
                ),
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_CHAINED_METHODS", CodeStyleLocalize.wrappingAlignWhenMultiline())
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_IF_STATEMENT),
            List.of(
                new CodeStyleSelectSettingPresentation(
                    "IF_BRACE_FORCE",
                    CodeStyleLocalize.wrappingForceBraces(),
                    BRACE_VALUES,
                    BRACE_OPTIONS
                ),
                new CodeStyleSettingPresentation("ELSE_ON_NEW_LINE", CodeStyleLocalize.wrappingElseOnNewLine()),
                new CodeStyleSettingPresentation("SPECIAL_ELSE_IF_TREATMENT", CodeStyleLocalize.wrappingSpecialElseIfBracesTreatment())
            )
        );

        putGroupTop(result, "FOR_STATEMENT_WRAP", WRAPPING_FOR_STATEMENT, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_FOR_STATEMENT),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_FOR", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation("FOR_STATEMENT_LPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingNewLineAfterLpar()),
                new CodeStyleSettingPresentation("FOR_STATEMENT_RPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingRparOnNewLine()),
                new CodeStyleSelectSettingPresentation(
                    "FOR_BRACE_FORCE",
                    CodeStyleLocalize.wrappingForceBraces(),
                    BRACE_VALUES,
                    BRACE_OPTIONS
                )
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_WHILE_STATEMENT),
            List.of(new CodeStyleSelectSettingPresentation(
                "WHILE_BRACE_FORCE",
                CodeStyleLocalize.wrappingForceBraces(),
                BRACE_VALUES,
                BRACE_OPTIONS
            ))
        );

        result.put(
            new SettingsGroup(WRAPPING_DOWHILE_STATEMENT),
            List.of(
                new CodeStyleSelectSettingPresentation(
                    "DOWHILE_BRACE_FORCE",
                    CodeStyleLocalize.wrappingForceBraces(),
                    BRACE_VALUES,
                    BRACE_OPTIONS
                ),
                new CodeStyleSettingPresentation("WHILE_ON_NEW_LINE", CodeStyleLocalize.wrappingWhileOnNewLine())
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_SWITCH_STATEMENT),
            List.of(
                new CodeStyleSettingPresentation("INDENT_CASE_FROM_SWITCH", CodeStyleLocalize.wrappingIndentCaseFromSwitch()),
                new CodeStyleSettingPresentation("INDENT_BREAK_FROM_CASE", CodeStyleLocalize.wrappingIndentBreakFromCase()),
                new CodeStyleSettingPresentation("CASE_STATEMENT_ON_NEW_LINE", CodeStyleLocalize.wrappingCaseStatementsOnOneLine())
            )
        );

        putGroupTop(result, "RESOURCE_LIST_WRAP", WRAPPING_TRY_RESOURCE_LIST, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_TRY_RESOURCE_LIST),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_RESOURCES", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation("RESOURCE_LIST_LPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingNewLineAfterLpar()),
                new CodeStyleSettingPresentation("RESOURCE_LIST_RPAREN_ON_NEXT_LINE", CodeStyleLocalize.wrappingRparOnNewLine())
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_TRY_STATEMENT),
            List.of(
                new CodeStyleSettingPresentation("CATCH_ON_NEW_LINE", CodeStyleLocalize.wrappingCatchOnNewLine()),
                new CodeStyleSettingPresentation("FINALLY_ON_NEW_LINE", CodeStyleLocalize.wrappingFinallyOnNewLine())
            )
        );

        putGroupTop(result, "BINARY_OPERATION_WRAP", WRAPPING_BINARY_OPERATION, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_BINARY_OPERATION),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_BINARY_OPERATION", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation("BINARY_OPERATION_SIGN_ON_NEXT_LINE", CodeStyleLocalize.wrappingOperationSignOnNextLine()),
                new CodeStyleSettingPresentation(
                    "ALIGN_MULTILINE_PARENTHESIZED_EXPRESSION",
                    CodeStyleLocalize.wrappingAlignParenthesisedWhenMultiline()
                ),
                new CodeStyleSettingPresentation("PARENTHESES_EXPRESSION_LPAREN_WRAP", CodeStyleLocalize.wrappingNewLineAfterLpar()),
                new CodeStyleSettingPresentation("PARENTHESES_EXPRESSION_RPAREN_WRAP", CodeStyleLocalize.wrappingRparOnNewLine())
            )
        );

        putGroupTop(result, "ASSIGNMENT_WRAP", WRAPPING_ASSIGNMENT, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_ASSIGNMENT),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_ASSIGNMENT", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation(
                    "PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE",
                    CodeStyleLocalize.wrappingAssignmentSignOnNextLine()
                )
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_FIELDS_VARIABLES_GROUPS),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_GROUP_FIELD_DECLARATIONS", CodeStyleLocalize.wrappingAlignFieldsInColumns()),
                new CodeStyleSettingPresentation(
                    "ALIGN_CONSECUTIVE_VARIABLE_DECLARATIONS",
                    CodeStyleLocalize.wrappingAlignVariablesInColumns()
                ),
                new CodeStyleSettingPresentation("ALIGN_SUBSEQUENT_SIMPLE_METHODS", CodeStyleLocalize.wrappingAlignSimpleMethodsInColumns())
            )
        );

        putGroupTop(result, "TERNARY_OPERATION_WRAP", WRAPPING_TERNARY_OPERATION, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_TERNARY_OPERATION),
            List.of(
                new CodeStyleSettingPresentation("ALIGN_MULTILINE_TERNARY_OPERATION", CodeStyleLocalize.wrappingAlignWhenMultiline()),
                new CodeStyleSettingPresentation(
                    "TERNARY_OPERATION_SIGNS_ON_NEXT_LINE",
                    CodeStyleLocalize.wrappingQuestAndColonSignsOnNextLine()
                )
            )
        );

        putGroupTop(result, "ARRAY_INITIALIZER_WRAP", WRAPPING_ARRAY_INITIALIZER, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_ARRAY_INITIALIZER),
            List.of(
                new CodeStyleSettingPresentation(
                    "ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION",
                    CodeStyleLocalize.wrappingAlignWhenMultiline()
                ),
                new CodeStyleSettingPresentation("ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE", CodeStyleLocalize.wrappingNewLineAfterLbrace()),
                new CodeStyleSettingPresentation("ARRAY_INITIALIZER_RBRACE_ON_NEXT_LINE", CodeStyleLocalize.wrappingRbraceOnNewLine())
            )
        );

        result.put(
            new SettingsGroup(WRAPPING_MODIFIER_LIST),
            List.of(new CodeStyleSettingPresentation("MODIFIER_LIST_WRAP", CodeStyleLocalize.wrappingAfterModifierList()))
        );

        putGroupTop(result, "ASSERT_STATEMENT_WRAP", WRAPPING_ASSERT_STATEMENT, WRAP_VALUES, WRAP_OPTIONS);
        result.put(
            new SettingsGroup(WRAPPING_ASSERT_STATEMENT),
            List.of(new CodeStyleSettingPresentation(
                "ASSERT_STATEMENT_COLON_ON_NEXT_LINE",
                CodeStyleLocalize.wrappingColonSignsOnNextLine()
            ))
        );

        putGroupTop(result, "ENUM_CONSTANTS_WRAP", CodeStyleLocalize.wrappingEnumConstants(), WRAP_VALUES, WRAP_OPTIONS);
        putGroupTop(result, "CLASS_ANNOTATION_WRAP", CodeStyleLocalize.wrappingClassesAnnotation(), WRAP_VALUES, WRAP_OPTIONS);
        putGroupTop(result, "METHOD_ANNOTATION_WRAP", CodeStyleLocalize.wrappingMethodsAnnotation(), WRAP_VALUES, WRAP_OPTIONS);
        putGroupTop(result, "FIELD_ANNOTATION_WRAP", CodeStyleLocalize.wrappingFieldsAnnotation(), WRAP_VALUES, WRAP_OPTIONS);
        putGroupTop(
            result,
            "PARAMETER_ANNOTATION_WRAP",
            CodeStyleLocalize.wrappingParametersAnnotation(),
            WRAP_VALUES,
            WRAP_OPTIONS
        );
        putGroupTop(
            result,
            "VARIABLE_ANNOTATION_WRAP",
            CodeStyleLocalize.wrappingLocalVariablesAnnotation(),
            WRAP_VALUES,
            WRAP_OPTIONS
        );
        WRAPPING_AND_BRACES_STANDARD_SETTINGS = Collections.unmodifiableMap(result);

        //-----------------------------------INDENT_SETTINGS-----------------------------------------------------

        result = new LinkedHashMap<>();
        result.put(
            new SettingsGroup(LocalizeValue.empty()),
            List.of(new CodeStyleSettingPresentation("INDENT_SIZE", CodeStyleLocalize.editboxIndentIndent()))
        );
        result.put(
            new SettingsGroup(LocalizeValue.empty()),
            List.of(new CodeStyleSettingPresentation("CONTINUATION_INDENT_SIZE", CodeStyleLocalize.editboxIndentContinuationIndent()))
        );
        result.put(
            new SettingsGroup(LocalizeValue.empty()),
            List.of(new CodeStyleSettingPresentation("TAB_SIZE", CodeStyleLocalize.editboxIndentTabSize()))
        );
        INDENT_STANDARD_SETTINGS = Collections.unmodifiableMap(result);
    }

    /**
     * Returns an immutable map containing all standard settings in a mapping of type (group -> settings contained in the group).
     * Notice that lists containing settings for a specific group are also immutable. Use copies to make modifications.
     *
     * @param settingsType type to get standard settings for
     * @return mapping setting groups to contained setting presentations
     */
    public static Map<SettingsGroup, List<CodeStyleSettingPresentation>> getStandardSettings(LanguageCodeStyleSettingsProvider.SettingsType settingsType) {
        return switch (settingsType) {
            case BLANK_LINES_SETTINGS -> BLANK_LINES_STANDARD_SETTINGS;
            case SPACING_SETTINGS -> SPACING_STANDARD_SETTINGS;
            case WRAPPING_AND_BRACES_SETTINGS -> WRAPPING_AND_BRACES_STANDARD_SETTINGS;
            case INDENT_SETTINGS -> INDENT_STANDARD_SETTINGS;
            default -> new LinkedHashMap<SettingsGroup, List<CodeStyleSettingPresentation>>();
        };
    }
}
