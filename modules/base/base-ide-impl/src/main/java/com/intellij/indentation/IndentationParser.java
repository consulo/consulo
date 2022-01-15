package com.intellij.indentation;

import com.intellij.lang.ASTNode;
import consulo.annotation.UsedInPlugin;
import consulo.lang.LanguageVersion;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public final ASTNode parse(@Nonnull final IElementType root, @Nonnull final PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
    final PsiBuilder.Marker fileMarker = builder.mark();
    final PsiBuilder.Marker documentMarker = myDocumentType == null ? null : builder.mark();

    final Stack<BlockInfo> stack = new Stack<>();
    stack.push(new BlockInfo(0, builder.mark(), builder.getTokenType()));

    PsiBuilder.Marker startLineMarker = null;
    int currentIndent = 0;
    boolean eolSeen = false;

    while (!builder.eof()) {
      final IElementType type = builder.getTokenType();
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
              final BlockInfo blockInfo = stack.pop();
              closeBlock(builder, blockInfo.getMarker(), blockInfo.getStartTokenType());
            }

            if (!stack.isEmpty()) {
              final BlockInfo blockInfo = stack.peek();
              if (currentIndent >= blockInfo.getIndent()) {
                if (currentIndent == blockInfo.getIndent()) {
                  final BlockInfo info = stack.pop();
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
      final BlockInfo blockInfo = stack.pop();
      closeBlock(builder, blockInfo.getMarker(), blockInfo.getStartTokenType());
    }

    if (documentMarker != null) {
      documentMarker.done(myDocumentType);
    }
    fileMarker.done(root);
    return builder.getTreeBuilt();
  }

  protected void closeBlock(final @Nonnull PsiBuilder builder, final @Nonnull PsiBuilder.Marker marker, final @Nullable IElementType startTokenType) {
    marker.done(myBlockElementType);
  }

  protected void advanceLexer(@Nonnull PsiBuilder builder) {
    builder.advanceLexer();
  }

  private void passEOLsAndIndents(@Nonnull final PsiBuilder builder) {
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

    private BlockInfo(final int indent, final @Nonnull PsiBuilder.Marker marker, final @Nullable IElementType type) {
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
