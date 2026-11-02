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
package consulo.language.editor.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
@Singleton
public class CodeSemanticsToolset implements McpToolset {
    private static final int MAX_LINES = 200;
    private static final int MAX_TREE_NODES = 1000;
    private static final int MAX_TOKEN_TEXT = 40;

    @Override
    public McpToolGroup getGroup() {
        return McpToolGroup.CODE_ANALYSIS;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_code_semantics")
            .description("Explains what the code in a line range means. Prints each source line followed by its meaningful tokens: "
                + "'keyword', 'literal', 'decl name : kind' for declarations, and 'ref text -> target' for references with the "
                + "resolved declaration (qualified name, kind and path:line, marked 'library' outside the project). References "
                + "that resolve to nothing are reported as UNRESOLVED. At most " + MAX_LINES + " lines per call.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file."))
            .param(McpParameter.integer("startLine", "First 1-based line of the range."))
            .param(McpParameter.integer("endLine", "Last 1-based line of the range, inclusive.").optional())
            .handler(CodeSemanticsToolset::getCodeSemantics);

        registrar.tool("get_psi_tree")
            .description("Prints the parser's syntax tree (PSI) for a line range: one node per line, indented by depth, as "
                + "'ElementType [line:column-line:column]', leaves with their text. Whitespace is skipped. Useful to see how the "
                + "parser understood the code. At most " + MAX_TREE_NODES + " nodes per call.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file."))
            .param(McpParameter.integer("startLine", "First 1-based line of the range."))
            .param(McpParameter.integer("endLine", "Last 1-based line of the range, inclusive.").optional())
            .param(McpParameter.integer("depth", "Maximum depth below the file node.").defaultValue(30))
            .handler(CodeSemanticsToolset::getPsiTree);
    }

    private static CompletableFuture<McpToolCallResult> getCodeSemantics(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        int startLine = context.getInt("startLine");
        String endLineArgument = context.findString("endLine");

        return McpCodeElements.committedReadAction(project, () -> {
            PsiFile psiFile = findPsiFile(project, path);
            Document document = psiFile == null ? null : PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (psiFile == null || document == null) {
                return McpToolCallResult.error("File is not backed by a PSI tree: " + path);
            }

            int endLine = endLineOf(endLineArgument, startLine, document);
            int startOffset = McpCodeElements.lineStartOffset(document, startLine);
            if (startOffset < 0 || endLine < startLine) {
                return McpToolCallResult.error("Line range is outside the file: " + startLine + "-" + endLine);
            }

            SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(psiFile.getLanguage(), project, psiFile.getVirtualFile());
            Set<TextRange> seenReferences = new HashSet<>();

            StringBuilder builder = new StringBuilder();
            for (int line = startLine; line <= endLine; line++) {
                int lineStart = document.getLineStartOffset(line - 1);
                int lineEnd = document.getLineEndOffset(line - 1);
                builder.append(line).append(": ").append(document.getText(new TextRange(lineStart, lineEnd))).append('\n');

                PsiElement leaf = psiFile.findElementAt(lineStart);
                while (leaf != null && leaf.getTextRange().getStartOffset() < lineEnd) {
                    if (leaf.getTextRange().getEndOffset() > lineStart) {
                        describeLeaf(project, highlighter, leaf, seenReferences, builder);
                    }
                    leaf = PsiTreeUtil.nextLeaf(leaf);
                }
            }
            if (requestedEndLine(endLineArgument, startLine) > endLine && endLine < document.getLineCount()) {
                builder.append("... truncated at ").append(MAX_LINES).append(" lines, continue from line ").append(endLine + 1).append('\n');
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static void describeLeaf(Project project,
                                     @Nullable SyntaxHighlighter highlighter,
                                     PsiElement leaf,
                                     Set<TextRange> seenReferences,
                                     StringBuilder builder) {
        if (leaf instanceof PsiWhiteSpace || leaf instanceof PsiComment || leaf.getTextLength() == 0) {
            return;
        }

        PsiNamedElement declaration = McpCodeElements.findDeclaration(leaf);
        if (declaration != null) {
            builder.append("    decl    ").append(declaration.getName()).append(" : ").append(McpCodeElements.kind(declaration)).append('\n');
            return;
        }

        boolean referenced = false;
        TextRange leafRange = leaf.getTextRange();
        PsiElement owner = leaf;
        for (int level = 0; owner != null && level < 3 && !(owner instanceof PsiFile); level++) {
            int ownerStart = owner.getTextRange().getStartOffset();
            for (PsiReference reference : owner.getReferences()) {
                TextRange range = reference.getRangeInElement().shiftRight(ownerStart);
                if (!range.contains(leafRange) || !seenReferences.add(range)) {
                    continue;
                }
                referenced = true;
                builder.append("    ref     ").append(StringUtil.first(reference.getCanonicalText(), MAX_TOKEN_TEXT, true))
                    .append(" -> ").append(describeTarget(project, reference)).append('\n');
            }
            owner = owner.getParent();
        }
        if (referenced || highlighter == null) {
            return;
        }

        ASTNode node = leaf.getNode();
        String category = node == null ? null : category(highlighter.getTokenHighlights(node.getElementType()));
        if (category != null) {
            builder.append("    ").append(category).append(' ').append(StringUtil.first(leaf.getText(), MAX_TOKEN_TEXT, true)).append('\n');
        }
    }

    private static String describeTarget(Project project, PsiReference reference) {
        PsiElement target = reference.resolve();
        if (target == null && reference instanceof PsiPolyVariantReference polyVariant) {
            ResolveResult[] results = polyVariant.multiResolve(false);
            if (results.length > 0) {
                List<String> candidates = new ArrayList<>();
                for (ResolveResult result : results) {
                    PsiElement element = result.getElement();
                    if (element != null) {
                        candidates.add(McpCodeElements.name(element) + " (" + McpCodeElements.location(project, element) + ")");
                    }
                }
                if (!candidates.isEmpty()) {
                    return "AMBIGUOUS " + String.join(" | ", candidates);
                }
            }
        }
        if (target == null) {
            return reference.isSoft() ? "(not resolved, soft)" : "UNRESOLVED";
        }
        return McpCodeElements.name(target) + " : " + McpCodeElements.kind(target) + " (" + McpCodeElements.location(project, target) + ")";
    }

    private static @Nullable String category(TextAttributesKey[] keys) {
        for (TextAttributesKey key : keys) {
            for (TextAttributesKey current = key; current != null; current = current.getFallbackAttributeKey()) {
                if (current == DefaultLanguageHighlighterColors.KEYWORD || current == DefaultLanguageHighlighterColors.MACRO_KEYWORD) {
                    return "keyword";
                }
                if (current == DefaultLanguageHighlighterColors.STRING || current == DefaultLanguageHighlighterColors.NUMBER) {
                    return "literal";
                }
            }
        }
        return null;
    }

    private static CompletableFuture<McpToolCallResult> getPsiTree(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        int startLine = context.getInt("startLine");
        String endLineArgument = context.findString("endLine");
        int depth = context.getInt("depth");

        return McpCodeElements.committedReadAction(project, () -> {
            PsiFile psiFile = findPsiFile(project, path);
            Document document = psiFile == null ? null : PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (psiFile == null || document == null) {
                return McpToolCallResult.error("File is not backed by a PSI tree: " + path);
            }

            int endLine = endLineOf(endLineArgument, startLine, document);
            int startOffset = McpCodeElements.lineStartOffset(document, startLine);
            if (startOffset < 0 || endLine < startLine) {
                return McpToolCallResult.error("Line range is outside the file: " + startLine + "-" + endLine);
            }
            TextRange range = new TextRange(startOffset, document.getLineEndOffset(endLine - 1));

            StringBuilder builder = new StringBuilder();
            int[] count = {0};
            printNode(psiFile, document, range, 0, depth, count, builder);
            if (count[0] >= MAX_TREE_NODES) {
                builder.append("... truncated at ").append(MAX_TREE_NODES).append(" nodes\n");
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static void printNode(PsiElement element, Document document, TextRange range, int level, int maxDepth, int[] count, StringBuilder builder) {
        if (count[0] >= MAX_TREE_NODES || element instanceof PsiWhiteSpace || !element.getTextRange().intersects(range)) {
            return;
        }
        count[0]++;

        ASTNode node = element.getNode();
        builder.append("  ".repeat(level))
            .append(node == null ? element.getClass().getSimpleName() : node.getElementType().toString())
            .append(" [").append(position(document, element.getTextRange().getStartOffset()))
            .append('-').append(position(document, element.getTextRange().getEndOffset())).append(']');

        PsiElement firstChild = element.getFirstChild();
        if (firstChild == null) {
            builder.append(" '").append(StringUtil.first(element.getText().replace("\n", "\\n"), MAX_TOKEN_TEXT, true)).append('\'');
        }
        builder.append('\n');

        if (level >= maxDepth) {
            return;
        }
        for (PsiElement child = firstChild; child != null; child = child.getNextSibling()) {
            printNode(child, document, range, level + 1, maxDepth, count, builder);
        }
    }

    private static String position(Document document, int offset) {
        int clamped = Math.min(offset, document.getTextLength());
        int line = document.getLineNumber(clamped);
        return (line + 1) + ":" + (clamped - document.getLineStartOffset(line) + 1);
    }

    private static int endLineOf(@Nullable String endLineArgument, int startLine, Document document) {
        return Math.min(Math.min(requestedEndLine(endLineArgument, startLine), startLine + MAX_LINES - 1), document.getLineCount());
    }

    private static int requestedEndLine(@Nullable String endLineArgument, int startLine) {
        return endLineArgument == null ? startLine + MAX_LINES - 1 : parseLine(endLineArgument, startLine);
    }

    private static int parseLine(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static @Nullable PsiFile findPsiFile(Project project, String path) {
        VirtualFile file = McpProjectPaths.resolve(project, path);
        return PsiManager.getInstance(project).findFile(file);
    }
}
