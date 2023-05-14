// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.template.context;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.EditorFactory;
import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.lazy.ClearableLazyValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Implement this class to describe some particular context that the user may associate with a live template, e.g., "Java String Start".
 * Contexts are available for the user in the Live Template management UI.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TemplateContextType {
  public static final ExtensionPointName<TemplateContextType> EP_NAME = ExtensionPointName.create(TemplateContextType.class);

  @Nonnull
  private final String myContextId;
  @Nonnull
  private final String myPresentableName;
  private final ClearableLazyValue<? extends TemplateContextType> myBaseContextType;

  protected TemplateContextType(@Nonnull String id, @Nonnull String presentableName) {
    this(id, presentableName, EverywhereContextType.class);
  }

  protected TemplateContextType(@Nonnull String id, @Nonnull String presentableName, @Nullable Class<? extends TemplateContextType> baseContextType) {
    myContextId = id;
    myPresentableName = presentableName;
    myBaseContextType = ClearableLazyValue.nullable((Supplier<? extends TemplateContextType>)() -> baseContextType == null ? null : EP_NAME.findExtensionOrFail(baseContextType));
  }

  /**
   * @return context presentable name for templates editor
   */
  @Nonnull
  public String getPresentableName() {
    return myPresentableName;
  }

  /**
   * @return unique ID to be used on configuration files to flag if this context is enabled for particular template
   */
  @Nonnull
  public String getContextId() {
    return myContextId;
  }

  /**
   * @deprecated use {@link #isInContext(TemplateActionContext)}
   */
  @Deprecated(forRemoval = true)
  public boolean isInContext(@Nonnull PsiFile file, int offset) {
    throw new RuntimeException("Please, implement isInContext(TemplateActionContext) method and don't invoke this method directly");
  }

  /**
   * @return true iff this context type permits using template associated with it according to {@code templateActionContext}
   */
  public boolean isInContext(@Nonnull TemplateActionContext templateActionContext) {
    return isInContext(templateActionContext.getFile(), templateActionContext.getStartOffset());
  }

  /**
   * @return whether an abbreviation of this context's template can be entered in editor
   * and expanded from there by Insert Live Template action
   */
  public boolean isExpandableFromEditor() {
    return true;
  }

  /**
   * @return syntax highlighter that going to be used in live template editor for template with context type enabled. If several context
   * types are enabled - first registered wins.
   */
  @Nullable
  public SyntaxHighlighter createHighlighter() {
    return null;
  }

  /**
   * @return parent context type. Parent context serves two purposes:
   * <ol>
   * <li>Context types hierarchy shown as a tree in template editor</li>
   * <li>When template applicability is computed, IDE finds all deepest applicable context types for the current {@link TemplateActionContext}
   * and excludes checking of all of their parent contexts. Then, IDE checks that at least one of these deepest applicable contexts is
   * enabled for the template.</li>
   * </ol>
   */
  @Nullable
  public TemplateContextType getBaseContextType() {
    return myBaseContextType.get();
  }

  public void clearCachedBaseContextType() {
    myBaseContextType.clear();
  }

  /**
   * @return document for live template editor. Used for live templates with this context type enabled. If several context types are enabled -
   * first registered wins.
   */
  public Document createDocument(CharSequence text, Project project) {
    return EditorFactory.getInstance().createDocument(text);
  }
}
