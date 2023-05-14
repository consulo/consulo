/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.postfixTemplate;


import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PostfixTemplateProvider implements LanguageExtension {
  private static final Logger LOG = Logger.getInstance(PostfixTemplate.class);

  private static final ExtensionPointCacheKey<PostfixTemplateProvider, ByLanguageValue<List<PostfixTemplateProvider>>> KEY =
          ExtensionPointCacheKey.create("PostfixTemplateProvider", LanguageOneToMany.build(false));

  @Nonnull
  public static List<PostfixTemplateProvider> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(PostfixTemplateProvider.class).getOrBuildCache(KEY).requiredGet(language);
  }

  private final LazyValue<Set<PostfixTemplate>> myTemplatesValue = LazyValue.notNull(() -> {
    Set<PostfixTemplate> postfixTemplates = buildTemplates();
    Set<String> keys = new HashSet<>();
    for (PostfixTemplate postfixTemplate : postfixTemplates) {
      if (!keys.add(postfixTemplate.getKey())) {
        LOG.error("Duplicate postfixTemplate with key: " + postfixTemplate.getKey());
      }
    }
    return postfixTemplates;
  });

  /**
   * Return all templates registered in the provider
   */
  @Nonnull
  public final Set<PostfixTemplate> getTemplates() {
    return myTemplatesValue.get();
  }

  protected abstract Set<PostfixTemplate> buildTemplates();

  /**
   * Check symbol can separate template keys
   */
  public abstract boolean isTerminalSymbol(char currentChar);

  /**
   * Prepare file for template expanding. Running on EDT.
   * E.g. java postfix templates adds semicolon after caret in order to simplify context checking.
   * <p>
   * File content doesn't contain template's key, it is deleted just before this method invocation.
   * <p>
   * Note that while postfix template is checking its availability the file parameter is a _COPY_ of the real file,
   * so you can do with it anything that you want, but in the same time it doesn't recommended to modify editor state because it's real.
   */
  public abstract void preExpand(@Nonnull PsiFile file, @Nonnull Editor editor);

  /**
   * Invoked after template finished (doesn't matter if it finished successfully or not).
   * E.g. java postfix template use this method for deleting inserted semicolon.
   */
  public abstract void afterExpand(@Nonnull PsiFile file, @Nonnull Editor editor);

  /**
   * Prepare file for checking availability of templates.
   * Almost the same as {@link this#preExpand(PsiFile, Editor)} with several differences:
   * 1. Processes copy of file. So implementations can modify it without corrupting the real file.
   * 2. Could be invoked from anywhere (EDT, write-action, read-action, completion-thread etc.). So implementations should make
   * additional effort to make changes in file.
   * <p>
   * Content of file copy doesn't contain template's key, it is deleted just before this method invocation.
   * <p>
   * NOTE: editor is real (not copy) and it doesn't represents the copyFile.
   * So it's safer to use currentOffset parameter instead of offset from editor. Do not modify text via editor.
   */
  @Nonnull
  public abstract PsiFile preCheck(@Nonnull PsiFile copyFile, @Nonnull Editor realEditor, int currentOffset);
}
