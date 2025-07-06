// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.annotation;

import consulo.annotation.DeprecationInfo;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.jetbrains.annotations.Contract;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows a custom language plugin to define annotations for files in that language.
 *
 * @author max
 * @see Annotator#annotate(PsiElement, AnnotationHolder)
 */
public interface AnnotationHolder {
    /**
     * Creates an error annotation with the specified message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the error message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createErrorAnnotation(@Nonnull PsiElement elt, @Nullable String message);

    /**
     * Creates an error annotation with the specified message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the error message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createErrorAnnotation(@Nonnull ASTNode node, @Nullable String message);

    /**
     * Creates an error annotation with the specified message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the error message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createErrorAnnotation(@Nonnull TextRange range, @Nullable String message);

    /**
     * Creates a warning annotation with the specified message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the warning message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createWarningAnnotation(@Nonnull PsiElement elt, @Nullable String message);

    /**
     * Creates a warning annotation with the specified message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the warning message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createWarningAnnotation(@Nonnull ASTNode node, @Nullable String message);

    /**
     * Creates a warning annotation with the specified message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the warning message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createWarningAnnotation(@Nonnull TextRange range, @Nullable String message);

    /**
     * Creates an annotation with severity {@link HighlightSeverity#WEAK_WARNING} ('weak warning') with the specified
     * message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the info message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createWeakWarningAnnotation(@Nonnull PsiElement elt, @Nullable String message);

    /**
     * Creates an annotation with severity {@link HighlightSeverity#WEAK_WARNING} ('weak warning') with the specified
     * message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the info message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createWeakWarningAnnotation(@Nonnull ASTNode node, @Nullable String message);

    /**
     * Creates an annotation with severity {@link HighlightSeverity#WEAK_WARNING} ('weak warning') with the specified
     * message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the info message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createWeakWarningAnnotation(@Nonnull TextRange range, @Nullable String message);

    /**
     * Creates an information annotation (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createInfoAnnotation(@Nonnull PsiElement elt, @Nullable String message);

    /**
     * Creates an information annotation (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createInfoAnnotation(@Nonnull ASTNode node, @Nullable String message);

    /**
     * Creates an information annotation (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createInfoAnnotation(@Nonnull TextRange range, @Nullable String message);

    /**
     * Creates an annotation with the given severity (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified text range.
     *
     * @param severity the severity.
     * @param range    the text range over which the annotation is created.
     * @param message  the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createAnnotation(@Nonnull HighlightSeverity severity, @Nonnull TextRange range, @Nullable String message);

    /**
     * Creates an annotation with the given severity (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message and tooltip markup over the specified text range.
     *
     * @param severity    the severity.
     * @param range       the text range over which the annotation is created.
     * @param message     the information message.
     * @param htmlTooltip the tooltip to show (usually the message, but escaped as HTML and surrounded by a {@code <html>} tag
     * @return the annotation (which can be modified to set additional annotation parameters)
     * @deprecated Use {@link #newAnnotation(HighlightSeverity, String)} instead
     */
    @Deprecated
    Annotation createAnnotation(
        @Nonnull HighlightSeverity severity,
        @Nonnull TextRange range,
        @Nullable String message,
        @Nullable String htmlTooltip
    );

    @Nonnull
    AnnotationSession getCurrentAnnotationSession();

    boolean isBatchMode();

    /**
     * Begin constructing a new annotation.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newAnnotation(HighlightSeverity.WARNING, "My warning message").create();}</p>
     *
     * @param severity The severity of the annotation.
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element, i.e. the psiElement currently visited by inspection
     * visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)} or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newAnnotation(@Nonnull HighlightSeverity severity, @Nonnull LocalizeValue message) {
        return newOfSeverity(severity, message);
    }

    /**
     * Begin constructing a new annotation.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newOfSeverity(HighlightSeverity.WARNING, "My warning message").create();}</p>
     *
     * @param severity The severity of the annotation.
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element,
     * i.e. the psiElement currently visited by inspection visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)}
     * or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newOfSeverity(@Nonnull HighlightSeverity severity, @Nonnull LocalizeValue message) {
        throw new IllegalStateException("Please do not override AnnotationHolder, use the standard provided one instead");
    }

    /**
     * Begin constructing a new annotation with severity {@link HighlightSeverity#ERROR}.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newError("My error message").create();}</p>
     *
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element,
     * i.e. the psiElement currently visited by inspection visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)}
     * or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newError(@Nonnull LocalizeValue message) {
        return newOfSeverity(HighlightSeverity.ERROR, message);
    }

    /**
     * Begin constructing a new annotation with severity {@link HighlightSeverity#WARNING}.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newWarning("My warning message").create();}</p>
     *
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element,
     * i.e. the psiElement currently visited by inspection visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)}
     * or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newWarn(@Nonnull LocalizeValue message) {
        return newOfSeverity(HighlightSeverity.WARNING, message);
    }

    /**
     * Begin constructing a new annotation with severity {@link HighlightSeverity#WEAK_WARNING}.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newWeakWarn("My warning message").create();}</p>
     *
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element,
     * i.e. the psiElement currently visited by inspection visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)}
     * or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newWeakWarn(@Nonnull LocalizeValue message) {
        return newOfSeverity(HighlightSeverity.WEAK_WARNING, message);
    }

    /**
     * Begin constructing a new annotation with severity {@link HighlightSeverity#INFORMATION}.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newInfo("My warning message").create();}</p>
     *
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element,
     * i.e. the psiElement currently visited by inspection visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)}
     * or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newInfo(@Nonnull LocalizeValue message) {
        return newOfSeverity(HighlightSeverity.INFORMATION, message);
    }

    /**
     * Begin constructing a new annotation.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newAnnotation(HighlightSeverity.WARNING, "My warning message").create();}</p>
     *
     * @param severity The severity of the annotation.
     * @param message  The message this annotation will show in the status bar and the tooltip.
     * @apiNote The builder created by this method is already initialized by the current element, i.e. the psiElement currently visited by inspection
     * visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)} or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    @Deprecated
    @DeprecationInfo("Use newOfSeverity(HighlightSeverity, LocalizeValue)")
    default AnnotationBuilder newAnnotation(@Nonnull HighlightSeverity severity, @Nonnull String message) {
        return newOfSeverity(severity, LocalizeValue.of(message));
    }

    /**
     * Begin constructing a new annotation with no message and no tooltip.
     * To finish construction and show the annotation on screen {@link AnnotationBuilder#create()} must be called.
     * For example: <p>{@code holder.newSilentAnnotation(HighlightSeverity.WARNING).textAttributes(MY_ATTRIBUTES_KEY).create();}</p>
     *
     * @param severity The severity of the annotation.
     * @apiNote The builder created by this method is already initialized by the current element, i.e. the psiElement currently visited by inspection
     * visitor. You'll need to call {@link AnnotationBuilder#range(TextRange)} or similar method explicitly only if target element differs from current element.
     * Please note, that the range in {@link AnnotationBuilder#range(TextRange)} must be inside the range of the current element.
     */
    @Contract(pure = true)
    @Nonnull
    default AnnotationBuilder newSilentAnnotation(@Nonnull HighlightSeverity severity) {
        throw new IllegalStateException("Please do not override AnnotationHolder, use the standard provided one instead");
    }
}