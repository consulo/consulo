/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.ide.scratch;

import com.intellij.ide.IdeView;
import consulo.dataContext.DataContext;
import consulo.language.editor.WriteCommandAction;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.text.StringUtil;
import consulo.language.codeStyle.CodeStyleManager;
import com.intellij.util.PathUtil;
import consulo.application.util.function.Computable;
import consulo.container.plugin.PluginIds;
import consulo.language.Language;
import consulo.language.LanguageExtension;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author gregsh
 */
public abstract class ScratchFileCreationHelper {
  public static final LanguageExtension<ScratchFileCreationHelper> EXTENSION = new LanguageExtension<>(PluginIds.CONSULO_BASE + ".scratch.creationHelper", new ScratchFileCreationHelper() {
  });

  /**
   * Override to change the default initial text for a scratch file stored in {@link Context#text} field.
   * Return true if the text is set up as needed and no further considerations are necessary.
   */
  public boolean prepareText(@Nonnull Project project, @Nonnull Context context, @Nonnull DataContext dataContext) {
    return false;
  }

  public void beforeCreate(@Nonnull Project project, @Nonnull Context context) {
  }

  public static class Context {
    @Nonnull
    public String text = "";
    public Language language;
    public int caretOffset;

    public String filePrefix;
    public Factory<Integer> fileCounter;
    public String fileExtension;

    public ScratchFileService.Option createOption = ScratchFileService.Option.create_new_always;
    public IdeView ideView;
  }

  @Nullable
  public static PsiFile parseHeader(@Nonnull Project project, @Nonnull Language language, @Nonnull String text) {
    LanguageFileType fileType = language.getAssociatedFileType();
    CharSequence fileSnippet = StringUtil.first(text, 10 * 1024, false);
    PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
    return fileFactory.createFileFromText(PathUtil.makeFileName("a", fileType == null ? "" : fileType.getDefaultExtension()), language, fileSnippet);
  }

  @Nonnull
  public static String reformat(@Nonnull Project project, @Nonnull Language language, @Nonnull String text) {
    return WriteCommandAction.runWriteCommandAction(project, (Computable<String>)() -> {
      PsiFile psi = parseHeader(project, language, text);
      if (psi != null) CodeStyleManager.getInstance(project).reformat(psi);
      return psi == null ? text : psi.getText();
    });
  }
}
