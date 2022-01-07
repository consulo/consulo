// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import com.intellij.application.options.codeStyle.cache.CodeStyleCachingService;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.*;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for miscellaneous code style settings retrieving methods.
 */
public class CodeStyle {

  private CodeStyle() {
  }

  /**
   * @return Default application-wide root code style settings.
   */
  @Nonnull
  public static CodeStyleSettings getDefaultSettings() {
    //noinspection deprecation
    return CodeStyleSettingsManager.getInstance().getCurrentSettings();
  }

  /**
   * Returns root code style settings for the given project. For configurable language settings use {@link #getLanguageSettings(PsiFile)} or
   * {@link #getLanguageSettings(PsiFile, Language)}.
   *
   * @param project The project to get code style settings for.
   * @return The current root code style settings associated with the project.
   */
  @Nonnull
  public static CodeStyleSettings getSettings(@Nonnull Project project) {
    //noinspection deprecation
    return CodeStyleSettingsManager.getInstance(project).getCurrentSettings();
  }

  /**
   * Returns either project settings if the project is not null or default application-wide settings otherwise.
   *
   * @param project The project to return the settings for or {@code null} for default settings.
   * @return Project or default code style settings.
   */
  @Nonnull
  public static CodeStyleSettings getProjectOrDefaultSettings(@Nullable Project project) {
    return project != null ? getSettings(project) : getDefaultSettings();
  }

  /**
   * Returns root code style settings for the given PSI file. For configurable language settings use {@link #getLanguageSettings(PsiFile)} or
   * {@link #getLanguageSettings(PsiFile, Language)}.
   *
   * @param file The file to get code style settings for.
   * @return The current root code style settings associated with the file or default settings if the file is invalid.
   */
  @Nonnull
  public static CodeStyleSettings getSettings(@Nonnull PsiFile file) {
    final Project project = file.getProject();
    CodeStyleSettings tempSettings = CodeStyleSettingsManager.getInstance(project).getTemporarySettings();
    if (tempSettings != null) {
      return tempSettings;
    }

    //CodeStyleSettings result = FileCodeStyleProvider.EP_NAME.computeSafeIfAny(provider -> provider.getSettings(file));
    //if (result != null) {
    //  return result;
    //}

    if (!file.isPhysical()) {
      PsiFile originalFile = file.getUserData(PsiFileFactory.ORIGINAL_FILE);
      if (originalFile != null && originalFile.isPhysical()) {
        return getSettings(originalFile);
      }
      return getSettings(project);
    }
    CodeStyleSettings cachedSettings = CodeStyleCachingService.getInstance(project).tryGetSettings(file);
    return cachedSettings != null ? cachedSettings : getSettings(project);
  }

  public static CodeStyleSettings getSettings(@Nonnull Editor editor) {
    Project project = editor.getProject();
    if (project != null) {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null) {
        return getSettings(file);
      }
      return getSettings(project);
    }
    return getDefaultSettings();
  }

  /**
   * Returns language code style settings for the specified editor. If the editor has an associated document and PSI file, returns
   * settings for the PSI file or {@code null} otherwise.
   *
   * @param editor The editor to get settings for.
   * @return The language code style settings for the editor or {@code null}.
   */
  @Nullable
  public static CommonCodeStyleSettings getLanguageSettings(@Nonnull Editor editor) {
    Project project = editor.getProject();
    if (project != null) {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null) {
        return getLanguageSettings(file);
      }
    }
    return null;
  }

  /**
   * Returns custom settings for the given PSI file.
   *
   * @param file                The file.
   * @param customSettingsClass The class of a settings object to be returned.
   * @param <T>                 Settings class type.
   * @return The current custom settings associated with the PSI file.
   */
  @Nonnull
  public static <T extends CustomCodeStyleSettings> T getCustomSettings(@Nonnull PsiFile file, Class<T> customSettingsClass) {
    CodeStyleSettings rootSettings = getSettings(file);
    return rootSettings.getCustomSettings(customSettingsClass);
  }

  /**
   * Returns language settings for the given PSI file. The language is taken from the file itself.
   *
   * @param file The file to retrieve language settings for.
   * @return The associated language settings.
   */
  @Nonnull
  public static CommonCodeStyleSettings getLanguageSettings(@Nonnull PsiFile file) {
    CodeStyleSettings rootSettings = getSettings(file);
    return rootSettings.getCommonSettings(file.getLanguage());
  }

  /**
   * Returns language settings for the given PSI file and language. This method may be useful when PSI file contains elements for multiple
   * languages and language settings should be taken from a specific language context.
   *
   * @param file The file to retrieve language settings for.
   * @return The associated language settings.
   */
  @Nonnull
  public static CommonCodeStyleSettings getLanguageSettings(@Nonnull PsiFile file, @Nonnull Language language) {
    CodeStyleSettings rootSettings = getSettings(file);
    return rootSettings.getCommonSettings(language);
  }

  /**
   * Returns indent options for the given PSI file. The method attempts to use {@link FileIndentOptionsProvider}
   * if applicable to the file. If there are no suitable indent options providers, it takes configurable language indent options or
   * retrieves indent options by file type.
   *
   * @param file The file to get indent options for.
   * @return The file indent options.
   * @see FileIndentOptionsProvider
   */
  @Nonnull
  public static CommonCodeStyleSettings.IndentOptions getIndentOptions(@Nonnull PsiFile file) {
    CodeStyleSettings rootSettings = getSettings(file);
    return rootSettings.getIndentOptionsByFile(file);
  }

  /**
   * Explicitly retrieves indent options by file type.
   *
   * @param file The file to get indent options for.
   * @return The indent options associated with the file type.
   */
  @Nonnull
  public static CommonCodeStyleSettings.IndentOptions getIndentOptionsByFileType(@Nonnull PsiFile file) {
    return getSettings(file).getIndentOptions(file.getFileType());
  }

  /**
   * Returns indent options for the given project and document.
   *
   * @param project  The current project.
   * @param document The document to get indent options for.
   * @return The indent options associated with document's PSI file if the file is available or other indent options otherwise.
   */
  @Nonnull
  public static CommonCodeStyleSettings.IndentOptions getIndentOptions(@Nonnull Project project, @Nonnull Document document) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file != null) {
      return getIndentOptions(file);
    }
    return getSettings(project).getIndentOptions(null);
  }

  /**
   * Returns indent size for the given PSI file.
   *
   * @param file The file to get indent size for.
   * @return The indent size to be used with the PSI file.
   */
  public static int getIndentSize(@Nonnull PsiFile file) {
    return getIndentOptions(file).INDENT_SIZE;
  }

  /**
   * Set temporary settings for the project. Temporary settings will override any user settings until {@link #dropTemporarySettings(Project)}
   * is called.
   * <p>
   * <b>Note</b>
   * The method is supposed to be used in test's {@code setUp()} method. In production code use
   * {@link #doWithTemporarySettings(Project, CodeStyleSettings, Runnable)}.
   *
   * @param project  The project.
   * @param settings The settings to use temporarily with the project.
   */
  @TestOnly
  public static void setTemporarySettings(@Nonnull Project project, @Nonnull CodeStyleSettings settings) {
    //noinspection deprecation
    CodeStyleSettingsManager.getInstance(project).setTemporarySettings(settings);
  }


  /**
   * Drop temporary settings.
   * <p>
   * <b>Note</b>
   * The method is supposed to be used in test's {@code tearDown()} method. In production code use
   * {@link #doWithTemporarySettings(Project, CodeStyleSettings, Runnable)}.
   *
   * @param project The project to drop temporary settings for.
   * @see #setTemporarySettings(Project, CodeStyleSettings)
   */
  @TestOnly
  public static void dropTemporarySettings(@Nonnull Project project) {
    //noinspection deprecation
    CodeStyleSettingsManager.getInstance(project).dropTemporarySettings();
  }

  /**
   * Execute the specified runnable with the given temporary code style settings and restore the old settings even if the runnable fails
   * with an exception.
   *
   * @param project      The current project.
   * @param tempSettings The temporary code style settings.
   * @param runnable     The runnable to execute with the temporary settings.
   */
  @SuppressWarnings("TestOnlyProblems")
  public static void doWithTemporarySettings(@Nonnull Project project, @Nonnull CodeStyleSettings tempSettings, @Nonnull Runnable runnable) {
    CodeStyleSettings tempSettingsBefore = CodeStyleSettingsManager.getInstance(project).getTemporarySettings();
    try {
      setTemporarySettings(project, tempSettings);
      runnable.run();
    }
    finally {
      if (tempSettingsBefore != null) {
        setTemporarySettings(project, tempSettingsBefore);
      }
      else {
        dropTemporarySettings(project);
      }
    }
  }

  /**
   * @param project The project to check.
   * @return {@code true} if the project uses its own project code style, {@code false} if global (application-level) code style settings
   * are used.
   */
  public static boolean usesOwnSettings(@Nonnull Project project) {
    //noinspection deprecation
    return CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS;
  }

  /**
   * Updates document's indent options from indent options providers.
   * <p><b>Note:</b> Calling this method directly when there is an editor associated with the document may cause the editor work
   * incorrectly. To keep consistency with the editor call {@code EditorEx.reinitSettings()} instead.
   *
   * @param project  The project of the document.
   * @param document The document to update indent options for.
   */
  public static void updateDocumentIndentOptions(@Nonnull Project project, @Nonnull Document document) {
    if (!project.isDisposed()) {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
      if (documentManager != null) {
        PsiFile file = documentManager.getPsiFile(document);
        if (file != null) {
          CommonCodeStyleSettings.IndentOptions indentOptions = getSettings(file).getIndentOptionsByFile(file, null, true, null);
          indentOptions.associateWithDocument(document);
        }
      }
    }
  }

  /**
   * Assign main project-wide code style settings and force the project to use its own code style instead of a global (application) one.
   *
   * @param project  The project to assign the settings to.
   * @param settings The settings to use with the project.
   */
  //public static void setMainProjectSettings(@NotNull Project project, @NotNull CodeStyleSettings settings) {
  //  @SuppressWarnings("deprecation") CodeStyleSettingsManager codeStyleSettingsManager = CodeStyleSettingsManager.getInstance(project);
  //  codeStyleSettingsManager.setMainProjectCodeStyle(settings);
  //  codeStyleSettingsManager.USE_PER_PROJECT_SETTINGS = true;
  //}

  @Nonnull
  public static CodeStyleBean getBean(@Nonnull Project project, @Nonnull Language language) {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(language);
    CodeStyleBean codeStyleBean = null;
    if (provider != null) {
      codeStyleBean = provider.createBean();
    }
    if (codeStyleBean == null) {
      codeStyleBean = new CodeStyleBean() {
        @Nonnull
        @Override
        protected Language getLanguage() {
          return language;
        }
      };
    }
    codeStyleBean.setRootSettings(getSettings(project));
    return codeStyleBean;
  }
}
