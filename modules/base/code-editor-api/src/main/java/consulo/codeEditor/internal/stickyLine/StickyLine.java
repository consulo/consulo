// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.internal.stickyLine;

import consulo.document.util.TextRange;

/**
 * Represents scope of class or function.
 * <p>
 * For example,
 * <pre>{@code
 * @Override
 * public void run() { // <- primary line
 *   //       ^ navigation offset
 *   // ...
 * }                   // <- scope line
 * }</pre>
 */
public interface StickyLine extends Comparable<StickyLine> {

    /**
     * The first logical line of scope used to pin corresponding editor's line on sticky panel.
     * <p>
     * Usually it is {@code lineOf(psiElement.textOffset)}
     */
    int primaryLine();

    /**
     * The last logical line of scope used to unpin corresponding editor's line from sticky panel.
     * <p>
     * Usually it is {@code lineOf(psiElement.endOffset)}
     */
    int scopeLine();

    /**
     * Offset where editor's caret put on mouse click.
     * <p>
     * Usually it is {@code psiElement.textOffset}
     */
    int navigateOffset();

    /**
     * Range between primary line and scope line.
     * <p>
     * Usually it is {@code new TextRange(psiElement.textOffset, psiElement.endOffset)}
     */
    TextRange textRange();

    /**
     * Short text of psi element representing the sticky line.
     * <p>
     * Usually it is {@code psiElement.toString()}. May be null if debug mode is disabled
     */
    String debugText();

    /**
     * Compares lines according to scope order.
     */
    @Override
    int compareTo(StickyLine other);
}
