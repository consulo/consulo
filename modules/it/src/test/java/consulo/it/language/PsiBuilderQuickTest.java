// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.language;

import consulo.it.HeadlessApplicationExtension;
import consulo.it.LoggedError;
import consulo.it.internal.HeadlessLoggerFactory;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.ast.ILightLazyParseableElementType;
import consulo.language.ast.LighterASTNode;
import consulo.language.ast.LighterLazyParseableNode;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.LazyParseableElement;
import consulo.language.impl.internal.ast.ASTStructure;
import consulo.language.impl.internal.parser.PsiBuilderImpl;
import consulo.language.impl.internal.psi.diff.DiffTree;
import consulo.language.impl.internal.psi.diff.DiffTreeChangeBuilder;
import consulo.language.impl.internal.psi.diff.ShallowNodeComparator;
import consulo.language.impl.plain.PlainTextParserDefinition;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.LexerBase;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.parser.PsiBuilderUtil;
import consulo.language.parser.WhitespacesAndCommentsBinder;
import consulo.language.parser.WhitespacesBinders;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.language.version.LanguageVersion;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ThreeState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(HeadlessApplicationExtension.class)
public class PsiBuilderQuickTest {
    private static final IFileElementType ROOT = new IFileElementType("ROOT", Language.ANY);

    private static final IElementType LETTER = new IElementType("LETTER", Language.ANY);
    private static final IElementType DIGIT = new IElementType("DIGIT", Language.ANY);
    private static final IElementType OTHER = new IElementType("OTHER", Language.ANY);
    private static final IElementType COLLAPSED = new IElementType("COLLAPSED", Language.ANY);
    private static final IElementType LEFT_BOUND = new IElementType("LEFT_BOUND", Language.ANY) {
        @Override
        public boolean isLeftBound() {
            return true;
        }
    };
    private static final IElementType COMMENT = new IElementType("COMMENT", Language.ANY);

    private static final TokenSet WHITESPACE_SET = TokenSet.WHITE_SPACE;
    private static final TokenSet COMMENT_SET = TokenSet.create(COMMENT);

    private static final LanguageVersion LANGUAGE_VERSION = new LanguageVersion("DEFAULT", "DEFAULT", Language.ANY);

    @Test
    public void testPlain() {
        doTest("a<<b",
            """
                Element(ROOT)
                  PsiElement(LETTER)('a')
                  PsiElement(OTHER)('<')
                  PsiElement(OTHER)('<')
                  PsiElement(LETTER)('b')
                """, builder -> {
                while (builder.getTokenType() != null) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testComposites() {
        doTest("1(a(b)c)2(d)3",
            """
                Element(ROOT)
                  PsiElement(DIGIT)('1')
                  Element(OTHER)
                    PsiElement(OTHER)('(')
                    PsiElement(LETTER)('a')
                    Element(OTHER)
                      PsiElement(OTHER)('(')
                      PsiElement(LETTER)('b')
                      PsiElement(OTHER)(')')
                    PsiElement(LETTER)('c')
                    PsiElement(OTHER)(')')
                  PsiElement(DIGIT)('2')
                  Element(OTHER)
                    PsiElement(OTHER)('(')
                    Element(OTHER)
                      <empty list>
                    PsiElement(LETTER)('d')
                    PsiElement(OTHER)(')')
                  PsiElement(DIGIT)('3')
                """, builder -> {
                PsiBuilderUtil.advance(builder, 1);
                PsiBuilder.Marker marker1 = builder.mark();
                PsiBuilderUtil.advance(builder, 2);
                PsiBuilder.Marker marker2 = builder.mark();
                PsiBuilderUtil.advance(builder, 3);
                marker2.done(OTHER);
                PsiBuilderUtil.advance(builder, 2);
                marker1.done(OTHER);
                PsiBuilderUtil.advance(builder, 1);
                PsiBuilder.Marker marker3 = builder.mark();
                PsiBuilderUtil.advance(builder, 1);
                builder.mark().done(OTHER);
                PsiBuilderUtil.advance(builder, 2);
                marker3.done(OTHER);
                PsiBuilderUtil.advance(builder, 1);
                assertEquals("(a(b)c)", getText(builder, marker1));
                assertEquals("(b)", getText(builder, marker2));
                assertEquals("(d)", getText(builder, marker3));
            }
        );
    }

    @Test
    public void testCollapse() {
        doTest("a<<>>b",
            """
                Element(ROOT)
                  PsiElement(LETTER)('a')
                  PsiElement(COLLAPSED)('<<')
                  PsiElement(COLLAPSED)('>>')
                  PsiElement(LETTER)('b')
                """, builder -> {
                PsiBuilderUtil.advance(builder, 1);
                PsiBuilder.Marker marker1 = builder.mark();
                PsiBuilderUtil.advance(builder, 2);
                marker1.collapse(COLLAPSED);
                PsiBuilder.Marker marker2 = builder.mark();
                PsiBuilderUtil.advance(builder, 2);
                marker2.collapse(COLLAPSED);
                PsiBuilderUtil.advance(builder, 1);
            }
        );
    }

    @Test
    public void testDoneAndError() {
        doTest("a2b",
            """
                Element(ROOT)
                  Element(LETTER)
                    PsiElement(LETTER)('a')
                  PsiErrorElement:no digits allowed
                    PsiElement(DIGIT)('2')
                  Element(LETTER)
                    PsiElement(LETTER)('b')
                """, builder -> {
                IElementType tokenType;
                while ((tokenType = builder.getTokenType()) != null) {
                    PsiBuilder.Marker marker = builder.mark();
                    builder.advanceLexer();
                    if (tokenType == DIGIT) marker.error(LocalizeValue.of("no digits allowed"));
                    else marker.done(tokenType);
                }
            }
        );
    }

    @Test
    public void testPrecedeAndDoneBefore() {
        doTest("ab",
            """
                Element(ROOT)
                  Element(COLLAPSED)
                    PsiElement(LETTER)('a')
                    Element(COLLAPSED)
                      <empty list>
                    PsiErrorElement:with error
                      <empty list>
                  Element(OTHER)
                    PsiElement(LETTER)('b')
                """, builder -> {
                PsiBuilder.Marker marker1 = builder.mark();
                builder.advanceLexer();
                PsiBuilder.Marker marker2 = builder.mark();
                builder.advanceLexer();
                marker2.done(OTHER);
                marker2.precede().doneBefore(COLLAPSED, marker2);
                marker1.doneBefore(COLLAPSED, marker2, LocalizeValue.of("with error"));
            }
        );
    }

    @Test
    public void testErrorBefore() {
        doTest("a1",
            """
                Element(ROOT)
                  Element(LETTER)
                    PsiElement(LETTER)('a')
                  PsiErrorElement:something lost
                    <empty list>
                  Element(DIGIT)
                    PsiElement(DIGIT)('1')
                """, builder -> {
                PsiBuilder.Marker letter = builder.mark();
                builder.advanceLexer();
                letter.done(LETTER);
                PsiBuilder.Marker digit = builder.mark();
                builder.advanceLexer();
                digit.done(DIGIT);
                digit.precede().errorBefore(LocalizeValue.of("something lost"), digit);
            }
        );
    }

    @Test
    public void testValidityChecksOnDone() throws Exception {
        doFailTest("a",
            "Another not done marker added after this one. Must be done before this.", builder -> {
                PsiBuilder.Marker first = builder.mark();
                builder.advanceLexer();
                builder.mark();
                first.done(LETTER);
            }
        );
    }

    @Test
    public void testValidityChecksOnDoneBefore1() throws Exception {
        doFailTest("a",
            "Another not done marker added after this one. Must be done before this.", builder -> {
                PsiBuilder.Marker first = builder.mark();
                builder.advanceLexer();
                PsiBuilder.Marker second = builder.mark();
                second.precede();
                first.doneBefore(LETTER, second);
            }
        );
    }

    @Test
    public void testValidityChecksOnDoneBefore2() throws Exception {
        doFailTest("a",
            "'Before' marker precedes this one.", builder -> {
                PsiBuilder.Marker first = builder.mark();
                builder.advanceLexer();
                PsiBuilder.Marker second = builder.mark();
                second.doneBefore(LETTER, first);
            }
        );
    }

    @Test
    public void testValidityChecksOnTreeBuild1() throws Exception {
        doFailTest("aa",
            "Parser produced no markers. Text:\naa", builder -> {
                while (!builder.eof()) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testValidityChecksOnTreeBuild2() throws Exception {
        doFailTest("aa",
            "Tokens [LETTER] were not inserted into the tree. Text:\naa", builder -> {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(LETTER);
            }
        );
    }

    @Test
    public void testValidityChecksOnTreeBuild3() throws Exception {
        doFailTest("a ",
            "Tokens [WHITE_SPACE] are outside of root element \"LETTER\". Text:\na ", builder -> {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(LETTER);
                while (!builder.eof()) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testWhitespaceTrimming() {
        doTest(" a b ",
            """
                Element(ROOT)
                  PsiWhiteSpace(' ')
                  Element(OTHER)
                    PsiElement(LETTER)('a')
                  PsiWhiteSpace(' ')
                  Element(OTHER)
                    PsiElement(LETTER)('b')
                  PsiWhiteSpace(' ')
                """, builder -> {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(OTHER);
                marker = builder.mark();
                builder.advanceLexer();
                marker.done(OTHER);
                builder.advanceLexer();
            }
        );
    }

    @Test
    public void testWhitespaceBalancingByErrors() {
        doTest("a b c",
            """
                Element(ROOT)
                  Element(OTHER)
                    PsiElement(LETTER)('a')
                    PsiErrorElement:error 1
                      <empty list>
                  PsiWhiteSpace(' ')
                  Element(OTHER)
                    PsiElement(LETTER)('b')
                    PsiErrorElement:error 2
                      <empty list>
                  PsiWhiteSpace(' ')
                  PsiErrorElement:error 3
                    PsiElement(LETTER)('c')
                """, builder -> {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                builder.error(LocalizeValue.of("error 1"));
                marker.done(OTHER);
                marker = builder.mark();
                builder.advanceLexer();
                builder.mark().error(LocalizeValue.of("error 2"));
                marker.done(OTHER);
                marker = builder.mark();
                builder.advanceLexer();
                marker.error(LocalizeValue.of("error 3"));
            }
        );
    }

    @Test
    public void testWhitespaceBalancingByEmptyComposites() {
        doTest("a b c",
            """
                Element(ROOT)
                  Element(OTHER)
                    PsiElement(LETTER)('a')
                    PsiWhiteSpace(' ')
                    Element(OTHER)
                      <empty list>
                  Element(OTHER)
                    PsiElement(LETTER)('b')
                    Element(LEFT_BOUND)
                      <empty list>
                  PsiWhiteSpace(' ')
                  PsiElement(LETTER)('c')
                """, builder -> {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                builder.mark().done(OTHER);
                marker.done(OTHER);
                marker = builder.mark();
                builder.advanceLexer();
                builder.mark().done(LEFT_BOUND);
                marker.done(OTHER);
                builder.advanceLexer();
            }
        );
    }

    @Test
    public void testCustomEdgeProcessors() {
        WhitespacesAndCommentsBinder leftEdgeProcessor = new WhitespacesAndCommentsBinder() {
            @Override
            public int getEdgePosition(List<IElementType> tokens, boolean atStreamEdge, TokenTextGetter getter) {
                int pos = tokens.size() - 1;
                while (tokens.get(pos) != COMMENT && pos > 0) pos--;
                return pos;
            }
        };
        WhitespacesAndCommentsBinder rightEdgeProcessor = new WhitespacesAndCommentsBinder() {
            @Override
            public int getEdgePosition(List<IElementType> tokens, boolean atStreamEdge, TokenTextGetter getter) {
                int pos = 0;
                while (tokens.get(pos) != COMMENT && pos < tokens.size() - 1) pos++;
                return pos + 1;
            }
        };

        doTest("{ # i # }",
            """
                Element(ROOT)
                  PsiElement(OTHER)('{')
                  PsiWhiteSpace(' ')
                  Element(OTHER)
                    PsiElement(COMMENT)('#')
                    PsiWhiteSpace(' ')
                    PsiElement(LETTER)('i')
                    PsiWhiteSpace(' ')
                    PsiElement(COMMENT)('#')
                  PsiWhiteSpace(' ')
                  PsiElement(OTHER)('}')
                """, builder -> {
                while (builder.getTokenType() != LETTER) builder.advanceLexer();
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(OTHER);
                marker.setCustomEdgeTokenBinders(leftEdgeProcessor, rightEdgeProcessor);
                while (builder.getTokenType() != null) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testCustomEdgeProcessorsOnOpenMarker() {
        doTest("{ # i # }",
            CUSTOM_EDGE_PROCESSORS_TREE, builder -> {
                while (builder.getTokenType() != LETTER) builder.advanceLexer();
                PsiBuilder.Marker marker = builder.mark();
                marker.setCustomEdgeTokenBinders(commentLeftEdgeProcessor(), commentRightEdgeProcessor());
                builder.advanceLexer();
                marker.done(OTHER);
                while (builder.getTokenType() != null) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testCustomEdgeProcessorsOnOpenMarkerDoneBefore() {
        doTest("{ # i # }",
            CUSTOM_EDGE_PROCESSORS_TREE, builder -> {
                while (builder.getTokenType() != LETTER) builder.advanceLexer();
                PsiBuilder.Marker marker = builder.mark();
                marker.setCustomEdgeTokenBinders(commentLeftEdgeProcessor(), commentRightEdgeProcessor());
                builder.advanceLexer();
                PsiBuilder.Marker before = builder.mark();
                marker.doneBefore(OTHER, before);
                before.drop();
                while (builder.getTokenType() != null) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testCustomEdgeProcessorsOnOpenMarkerError() {
        doTest("{ # i # }",
            """
                Element(ROOT)
                  PsiElement(OTHER)('{')
                  PsiWhiteSpace(' ')
                  PsiErrorElement:letter
                    PsiElement(COMMENT)('#')
                    PsiWhiteSpace(' ')
                    PsiElement(LETTER)('i')
                    PsiWhiteSpace(' ')
                    PsiElement(COMMENT)('#')
                  PsiWhiteSpace(' ')
                  PsiElement(OTHER)('}')
                """, builder -> {
                while (builder.getTokenType() != LETTER) builder.advanceLexer();
                PsiBuilder.Marker marker = builder.mark();
                marker.setCustomEdgeTokenBinders(commentLeftEdgeProcessor(), commentRightEdgeProcessor());
                builder.advanceLexer();
                marker.error(LocalizeValue.of("letter"));
                while (builder.getTokenType() != null) builder.advanceLexer();
            }
        );
    }

    @Test
    public void testRightEdgeBinderOfRolledBackMarkerIsForgotten() {
        doTest("a ",
            """
                Element(ROOT)
                  Element(OTHER)
                    PsiElement(LETTER)('a')
                  PsiWhiteSpace(' ')
                """, builder -> {
                PsiBuilder.Marker rolledBack = builder.mark();
                rolledBack.setCustomEdgeTokenBinders(null, WhitespacesBinders.GREEDY_RIGHT_BINDER);
                rolledBack.rollbackTo();
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(OTHER);
                assertTrue(builder.eof());
            }
        );
    }

    @Test
    public void testLightChameleon() {
        IElementType CHAMELEON_2 = new MyChameleon2Type();
        IElementType CHAMELEON_1 = new MyChameleon1Type(CHAMELEON_2);

        doTest("ab{12[.?]}cd{x}",
            """
                Element(ROOT)
                  PsiElement(LETTER)('a')
                  PsiElement(LETTER)('b')
                  Element(CHAMELEON_1)
                    PsiElement(OTHER)('{')
                    PsiElement(DIGIT)('1')
                    PsiElement(DIGIT)('2')
                    Element(OTHER)
                      Element(CHAMELEON_2)
                        PsiElement(OTHER)('[')
                        PsiElement(OTHER)('.')
                        PsiErrorElement:test error 2
                          PsiElement(OTHER)('?')
                        PsiElement(OTHER)(']')
                    PsiErrorElement:test error 1
                      <empty list>
                    PsiElement(OTHER)('}')
                  PsiElement(LETTER)('c')
                  PsiElement(LETTER)('d')
                  Element(CHAMELEON_1)
                    PsiElement(OTHER)('{')
                    PsiElement(LETTER)('x')
                    PsiElement(OTHER)('}')
                """, builder -> {
                PsiBuilderUtil.advance(builder, 2);
                PsiBuilder.Marker chameleon = builder.mark();
                PsiBuilderUtil.advance(builder, 8);
                chameleon.collapse(CHAMELEON_1);
                PsiBuilderUtil.advance(builder, 2);
                chameleon = builder.mark();
                PsiBuilderUtil.advance(builder, 3);
                chameleon.collapse(CHAMELEON_1);
            }
        );
    }

    @Test
    public void testLightChameleonIsParsedOnce() {
        AtomicInteger parserInvocations = new AtomicInteger();

        PsiBuilder builder = createBuilder("{x}");
        PsiBuilder.Marker rootMarker = builder.mark();
        PsiBuilder.Marker chameleon = builder.mark();
        PsiBuilderUtil.advance(builder, 3);
        chameleon.collapse(new MyChameleon2Type() {
            @Override
            public void parse(PsiBuilder builder1) {
                parserInvocations.incrementAndGet();
                super.parse(builder1);
            }
        });
        rootMarker.done(ROOT);

        String treeString = """
            Element(ROOT)
              Element(CHAMELEON_2)
                PsiElement(OTHER)('{')
                PsiElement(LETTER)('x')
                PsiElement(OTHER)('}')
            """;
        FlyweightCapableTreeStructure<LighterASTNode> tree = builder.getLightTree();
        assertEquals(treeString, DebugUtil.lightTreeToString(tree, false));
        assertEquals(1, parserInvocations.get());

        assertEquals(treeString, DebugUtil.lightTreeToString(tree, false));
        assertEquals(1, parserInvocations.get());

        // new tree
        assertEquals(treeString, DebugUtil.lightTreeToString(builder.getLightTree(), false));
        assertEquals(1, parserInvocations.get());
    }

    @Test
    public void testEndMarkersOverlapping() {
        doTest("a ",
            END_MARKERS_OVERLAPPING_TREE, builder -> {
                PsiBuilder.Marker e1 = builder.mark();
                PsiBuilder.Marker e2 = builder.mark();
                builder.advanceLexer();
                e2.done(OTHER);
                e2.setCustomEdgeTokenBinders(null, WhitespacesBinders.GREEDY_RIGHT_BINDER);
                e1.done(OTHER);
                e1.setCustomEdgeTokenBinders(null, WhitespacesBinders.DEFAULT_RIGHT_BINDER);
                assertTrue(builder.eof());
            }
        );
    }

    @Test
    public void testEndMarkersOverlappingOnOpenMarkers() {
        doTest("a ",
            END_MARKERS_OVERLAPPING_TREE, builder -> {
                PsiBuilder.Marker e1 = builder.mark();
                PsiBuilder.Marker e2 = builder.mark();
                e2.setCustomEdgeTokenBinders(null, WhitespacesBinders.GREEDY_RIGHT_BINDER);
                e1.setCustomEdgeTokenBinders(null, WhitespacesBinders.DEFAULT_RIGHT_BINDER);
                builder.advanceLexer();
                e2.done(OTHER);
                e1.done(OTHER);
                assertTrue(builder.eof());
            }
        );
    }

    @Test
    public void testEmptyCollapsedNode() {
        doTest("a<<b",
            """
                Element(ROOT)
                  PsiElement(LETTER)('a')
                  PsiElement(COLLAPSED)('')
                  PsiElement(OTHER)('<')
                  PsiElement(OTHER)('<')
                  PsiElement(LETTER)('b')
                """, builder -> {
                builder.advanceLexer();
                builder.mark().collapse(COLLAPSED);
                while (builder.getTokenType() != null) {
                    builder.advanceLexer();
                }
            }
        );
    }

    @Test
    public void testRemapNoRestore() {
        doTest(
            "ab",
            """
                Element(ROOT)
                  Element(OTHER)
                    PsiElement(LETTER)('a')
                  Element(OTHER)
                    PsiElement(DIGIT)('b')
                """,
            builder -> {
                PsiBuilder.Marker a = builder.mark();
                builder.advanceLexer();
                a.done(OTHER);

                PsiBuilder.Marker b = builder.mark();
                builder.remapCurrentTokenAndRestoreOnRollback(DIGIT);
                builder.advanceLexer();
                b.done(OTHER);
            }
        );
    }

    @Test
    public void testRemapAndRestore() {
        doTest(
            "ab",
            """
                Element(ROOT)
                  Element(OTHER)
                    PsiElement(LETTER)('a')
                  Element(OTHER)
                    PsiElement(LETTER)('b')
                """,
            builder -> {
                PsiBuilder.Marker a = builder.mark();
                builder.advanceLexer();
                a.done(OTHER);

                PsiBuilder.Marker b = builder.mark();
                builder.remapCurrentTokenAndRestoreOnRollback(DIGIT);
                builder.remapCurrentTokenAndRestoreOnRollback(OTHER);
                builder.advanceLexer();

                PsiBuilder.Marker b1 = b.precede();
                b.rollbackTo();
                builder.advanceLexer();
                b1.done(OTHER);
            }
        );
    }

    private static final String CUSTOM_EDGE_PROCESSORS_TREE = """
        Element(ROOT)
          PsiElement(OTHER)('{')
          PsiWhiteSpace(' ')
          Element(OTHER)
            PsiElement(COMMENT)('#')
            PsiWhiteSpace(' ')
            PsiElement(LETTER)('i')
            PsiWhiteSpace(' ')
            PsiElement(COMMENT)('#')
          PsiWhiteSpace(' ')
          PsiElement(OTHER)('}')
        """;

    private static final String END_MARKERS_OVERLAPPING_TREE = """
        Element(ROOT)
          Element(OTHER)
            Element(OTHER)
              PsiElement(LETTER)('a')
              PsiWhiteSpace(' ')
        """;

    private static WhitespacesAndCommentsBinder commentLeftEdgeProcessor() {
        return (tokens, atStreamEdge, getter) -> {
            int pos = tokens.size() - 1;
            while (tokens.get(pos) != COMMENT && pos > 0) pos--;
            return pos;
        };
    }

    private static WhitespacesAndCommentsBinder commentRightEdgeProcessor() {
        return (tokens, atStreamEdge, getter) -> {
            int pos = 0;
            while (tokens.get(pos) != COMMENT && pos < tokens.size() - 1) pos++;
            return pos + 1;
        };
    }

    private static String getText(PsiBuilder builder, PsiBuilder.Marker marker) {
        LighterASTNode node = (LighterASTNode) marker;
        return builder.getOriginalText().subSequence(node.getStartOffset(), node.getEndOffset()).toString();
    }

    private static void doTest(String text, String expected, Consumer<? super PsiBuilder> parser) {
        PsiBuilder builder = createBuilder(text);
        PsiBuilder.Marker rootMarker = builder.mark();
        parser.accept(builder);
        rootMarker.done(ROOT);

        // check light tree composition
        FlyweightCapableTreeStructure<LighterASTNode> lightTree = builder.getLightTree();
        assertEquals(text, getText(builder, (PsiBuilder.Marker) lightTree.getRoot()));
        assertEquals(expected, DebugUtil.lightTreeToString(lightTree, false));
        // verify that light tree can be taken multiple times
        FlyweightCapableTreeStructure<LighterASTNode> lightTree2 = builder.getLightTree();
        assertEquals(expected, DebugUtil.lightTreeToString(lightTree2, false));

        // check heavy tree composition
        ASTNode root = builder.getTreeBuilt();
        assertEquals(expected, DebugUtil.nodeTreeToString(root, false));

        // check heavy vs. light tree merging
        PsiBuilder builder2 = createBuilder(text);
        PsiBuilder.Marker rootMarker2 = builder2.mark();
        parser.accept(builder2);
        rootMarker2.done(ROOT);
        DiffTree.diff(
            new ASTStructure(root), builder2.getLightTree(),
            new ShallowNodeComparator<>() {
                @Override
                public ThreeState deepEqual(ASTNode oldNode, LighterASTNode newNode) {
                    return ThreeState.UNSURE;
                }

                @Override
                public boolean typesEqual(ASTNode oldNode, LighterASTNode newNode) {
                    return true;
                }

                @Override
                public boolean hashCodesEqual(ASTNode oldNode, LighterASTNode newNode) {
                    return true;
                }
            },
            new DiffTreeChangeBuilder<>() {
                @Override
                public void nodeReplaced(ASTNode oldChild, LighterASTNode newChild) {
                    fail("replaced(" + oldChild + "," + newChild.getTokenType() + ")");
                }

                @Override
                public void nodeDeleted(ASTNode oldParent, ASTNode oldNode) {
                    fail("deleted(" + oldParent + "," + oldNode + ")");
                }

                @Override
                public void nodeInserted(ASTNode oldParent, LighterASTNode newNode, int pos) {
                    fail("inserted(" + oldParent + "," + newNode.getTokenType() + ")");
                }
            },
            root.getText());
    }

    private static void doFailTest(String text, String expected, Consumer<? super PsiBuilder> parser) throws Exception {
        try {
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(new PlainTextParserDefinition(), new MyTestLexer(), text);
            builder.setDebugMode(true);
            parser.accept(builder);
            builder.getLightTree();
        }
        catch (RuntimeException | AssertionError e) {
            if (HeadlessLoggerFactory.peekLoggedErrors().isEmpty()) {
                throw e;
            }
        }
        List<LoggedError> loggedErrors = HeadlessLoggerFactory.takeLoggedErrors();
        if (loggedErrors.isEmpty()) {
            fail("should fail");
        }
        LoggedError error = loggedErrors.get(0);
        assertEquals("[" + error.getCategory() + "] " + expected, error.getMessage());
    }

    private static PsiBuilderImpl createBuilder(CharSequence text) {
        ParserDefinition parserDefinition = new PlainTextParserDefinition() {
            @Override
            public Lexer createLexer(LanguageVersion languageVersion) {
                return new MyTestLexer();
            }

            @Override
            public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
                return WHITESPACE_SET;
            }

            @Override
            public TokenSet getCommentTokens(LanguageVersion languageVersion) {
                return COMMENT_SET;
            }
        };
        return new PsiBuilderImpl(null, null, parserDefinition, parserDefinition.createLexer(LANGUAGE_VERSION), LANGUAGE_VERSION, null,
            text, null, null);
    }

    private static class MyTestLexer extends LexerBase {
        private CharSequence myBuffer = "";
        private int myIndex = 0;
        private int myBufferEnd = 1;

        @Override
        public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
            myBuffer = buffer.subSequence(startOffset, endOffset);
            myIndex = 0;
            myBufferEnd = myBuffer.length();
        }

        @Override
        public int getState() {
            return 0;
        }

        @Override
        public IElementType getTokenType() {
            if (myIndex >= myBufferEnd) return null;
            else if (Character.isLetter(myBuffer.charAt(myIndex))) return LETTER;
            else if (Character.isDigit(myBuffer.charAt(myIndex))) return DIGIT;
            else if (Character.isWhitespace(myBuffer.charAt(myIndex))) return TokenType.WHITE_SPACE;
            else if (myBuffer.charAt(myIndex) == '#') return COMMENT;
            else return OTHER;
        }

        @Override
        public int getTokenStart() {
            return myIndex;
        }

        @Override
        public int getTokenEnd() {
            return myIndex + 1;
        }

        @Override
        public void advance() {
            if (myIndex < myBufferEnd) myIndex++;
        }

        @Override
        public CharSequence getBufferSequence() {
            return myBuffer;
        }

        @Override
        public int getBufferEnd() {
            return myBufferEnd;
        }
    }

    private abstract static class MyLazyElementType extends ILazyParseableElementType implements ILightLazyParseableElementType {
        protected MyLazyElementType(String debugName) {
            super(debugName, Language.ANY);
        }

        @Override
        public ASTNode createNode(CharSequence text) {
            return new LazyParseableElement(this, text);
        }
    }

    private static class MyChameleon1Type extends MyLazyElementType {
        private final IElementType myCHAMELEON_2;

        MyChameleon1Type(IElementType CHAMELEON_2) {
            super("CHAMELEON_1");
            myCHAMELEON_2 = CHAMELEON_2;
        }

        @Override
        public FlyweightCapableTreeStructure<LighterASTNode> parseContents(LighterLazyParseableNode chameleon) {
            PsiBuilder builder = createBuilder(chameleon.getText());
            parse(builder);
            return builder.getLightTree();
        }

        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            PsiBuilder builder = createBuilder(chameleon.getText());
            parse(builder);
            return builder.getTreeBuilt().getFirstChildNode();
        }

        public void parse(PsiBuilder builder) {
            PsiBuilder.Marker root = builder.mark();
            PsiBuilder.Marker nested = null;
            while (!builder.eof()) {
                String token = builder.getTokenText();
                if ("[".equals(token) && nested == null) {
                    nested = builder.mark();
                }
                builder.advanceLexer();
                if ("]".equals(token) && nested != null) {
                    nested.collapse(myCHAMELEON_2);
                    nested.precede().done(OTHER);
                    nested = null;
                    builder.error(LocalizeValue.of("test error 1"));
                }
            }
            if (nested != null) nested.drop();
            root.done(this);
        }
    }

    private static class MyChameleon2Type extends MyLazyElementType {
        MyChameleon2Type() {
            super("CHAMELEON_2");
        }

        @Override
        public FlyweightCapableTreeStructure<LighterASTNode> parseContents(LighterLazyParseableNode chameleon) {
            PsiBuilder builder = createBuilder(chameleon.getText());
            parse(builder);
            return builder.getLightTree();
        }

        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            PsiBuilder builder = createBuilder(chameleon.getText());
            parse(builder);
            return builder.getTreeBuilt().getFirstChildNode();
        }

        public void parse(PsiBuilder builder) {
            PsiBuilder.Marker root = builder.mark();
            PsiBuilder.Marker error = null;
            while (!builder.eof()) {
                String token = builder.getTokenText();
                if ("?".equals(token)) error = builder.mark();
                builder.advanceLexer();
                if (error != null) {
                    error.error(LocalizeValue.of("test error 2"));
                    error = null;
                }
            }
            root.done(this);
        }
    }
}
