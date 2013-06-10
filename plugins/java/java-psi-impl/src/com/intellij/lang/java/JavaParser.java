package com.intellij.lang.java;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageVersion;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 19:50/30.05.13
 */
public class JavaParser implements PsiParser {
  @NotNull
  @Override
  public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder, @NotNull LanguageVersion languageVersion) {
    builder.setDebugMode(true);
    final PsiBuilder.Marker marker = builder.mark();
    com.intellij.lang.java.parser.JavaParser.INSTANCE.getFileParser().parse(builder);
    marker.done(root);
    return builder.getTreeBuilt();
  }
}
