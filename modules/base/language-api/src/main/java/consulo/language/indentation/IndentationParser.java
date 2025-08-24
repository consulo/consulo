package consulo.language.indentation;

import consulo.annotation.UsedInPlugin;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;
import consulo.util.collection.Stack;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author oleg
 */
@UsedInPlugin
public abstract class IndentationParser implements PsiParser {
    @Nonnull
    private final IElementType myEolTokenType;
    @Nonnull
    private final IElementType myIndentTokenType;
    @Nonnull
    private final IElementType myBlockElementType;
    @Nullable
    private final IElementType myDocumentType;

    public IndentationParser(@Nullable IElementType documentType,
                             @Nonnull IElementType blockElementType,
                             @Nonnull IElementType eolTokenType,
                             @Nonnull IElementType indentTokenType) {
        myDocumentType = documentType;
        myBlockElementType = blockElementType;
        myEolTokenType = eolTokenType;
        myIndentTokenType = indentTokenType;
    }

    @Override
    @Nonnull
    public final ASTNode parse(@Nonnull IElementType root, @Nonnull PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
        PsiBuilder.Marker fileMarker = builder.mark();
        PsiBuilder.Marker documentMarker = myDocumentType == null ? null : builder.mark();

        Stack<BlockInfo> stack = new Stack<>();
        stack.push(new BlockInfo(0, builder.mark(), builder.getTokenType()));

        PsiBuilder.Marker startLineMarker = null;
        int currentIndent = 0;
        boolean eolSeen = false;

        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            // EOL
            if (type == myEolTokenType) {
                // Handle variant with several EOLs
                if (startLineMarker == null) {
                    startLineMarker = builder.mark();
                }
                eolSeen = true;
            }
            else {
                if (type == myIndentTokenType) {
                    //noinspection ConstantConditions
                    currentIndent = builder.getTokenText().length();
                }
                else {
                    if (!eolSeen && !stack.isEmpty() && currentIndent > 0 && currentIndent < stack.peek().getIndent()) {
                        // sometimes we do not have EOL between indents
                        eolSeen = true;
                    }
                    if (eolSeen) {
                        if (startLineMarker != null) {
                            startLineMarker.rollbackTo();
                            startLineMarker = null;
                        }
                        // Close indentation blocks
                        while (!stack.isEmpty() && currentIndent < stack.peek().getIndent()) {
                            BlockInfo blockInfo = stack.pop();
                            closeBlock(builder, blockInfo.getMarker(), blockInfo.getStartTokenType());
                        }

                        if (!stack.isEmpty()) {
                            BlockInfo blockInfo = stack.peek();
                            if (currentIndent >= blockInfo.getIndent()) {
                                if (currentIndent == blockInfo.getIndent()) {
                                    BlockInfo info = stack.pop();
                                    closeBlock(builder, info.getMarker(), info.getStartTokenType());
                                }
                                passEOLsAndIndents(builder);
                                stack.push(new BlockInfo(currentIndent, builder.mark(), type));
                            }
                        }
                        eolSeen = false;
                        currentIndent = 0;
                    }
                }
            }
            advanceLexer(builder);
        }

        // Close all left opened markers
        if (startLineMarker != null) {
            startLineMarker.drop();
        }
        while (!stack.isEmpty()) {
            BlockInfo blockInfo = stack.pop();
            closeBlock(builder, blockInfo.getMarker(), blockInfo.getStartTokenType());
        }

        if (documentMarker != null) {
            documentMarker.done(myDocumentType);
        }
        fileMarker.done(root);
        return builder.getTreeBuilt();
    }

    protected void closeBlock(@Nonnull PsiBuilder builder, @Nonnull PsiBuilder.Marker marker, @Nullable IElementType startTokenType) {
        marker.done(myBlockElementType);
    }

    protected void advanceLexer(@Nonnull PsiBuilder builder) {
        builder.advanceLexer();
    }

    private void passEOLsAndIndents(@Nonnull PsiBuilder builder) {
        IElementType tokenType = builder.getTokenType();
        while (tokenType == myEolTokenType || tokenType == myIndentTokenType) {
            builder.advanceLexer();
            tokenType = builder.getTokenType();
        }
    }

    private static final class BlockInfo {
        private final int myIndent;
        @Nonnull
        private final PsiBuilder.Marker myMarker;
        @Nullable
        private final IElementType myStartTokenType;

        private BlockInfo(int indent, @Nonnull PsiBuilder.Marker marker, @Nullable IElementType type) {
            myIndent = indent;
            myMarker = marker;
            myStartTokenType = type;
        }

        public int getIndent() {
            return myIndent;
        }

        @Nonnull
        public PsiBuilder.Marker getMarker() {
            return myMarker;
        }

        @Nullable
        public IElementType getStartTokenType() {
            return myStartTokenType;
        }
    }
}
