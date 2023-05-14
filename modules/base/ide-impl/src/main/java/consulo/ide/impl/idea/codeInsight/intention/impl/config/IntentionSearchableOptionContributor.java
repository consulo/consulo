/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionContributor;
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionProcessor;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.impl.internal.intention.IntentionManagerSettings;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.logging.Logger;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Set;

@ExtensionImpl
public class IntentionSearchableOptionContributor extends SearchableOptionContributor {
  private static final Logger LOG = Logger.getInstance(IntentionSearchableOptionContributor.class);

  private final Application myApplication;

  @Inject
  public IntentionSearchableOptionContributor(Application application) {
    myApplication = application;
  }

  @Override
  public void processOptions(@Nonnull SearchableOptionProcessor processor) {
    for (IntentionAction action : myApplication.getExtensionList(IntentionAction.class)) {
      IntentionMetaData intentionMetaData = action.getClass().getAnnotation(IntentionMetaData.class);
      if (intentionMetaData == null) {
        continue;
      }

      String[] categories = intentionMetaData.categories();

      String descriptionDirectoryName = IntentionManagerSettings.getDescriptionDirectoryName(action);

      IntentionActionMetaData data = new IntentionActionMetaData(action, categories, descriptionDirectoryName);

      final TextDescriptor description = data.getDescription();

      try {
        String descriptionText = description.getText().toLowerCase();
        descriptionText = IntentionManagerSettings.HTML_PATTERN.matcher(descriptionText).replaceAll(" ");
        final Set<String> words = processor.getProcessedWordsWithoutStemming(descriptionText);
        words.addAll(processor.getProcessedWords(data.getActionText()));
        for (String word : words) {
          processor.addOption(word, data.getActionText(), data.getActionText(), "editor.code.intentions", CodeInsightBundle.message("intention.settings"));
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }
}
