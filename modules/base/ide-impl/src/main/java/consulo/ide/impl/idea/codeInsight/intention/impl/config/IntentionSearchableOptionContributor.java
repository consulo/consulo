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
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionContributor;
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionProcessor;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.intention.IntentionActionBean;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Set;

@ExtensionImpl
public class IntentionSearchableOptionContributor extends SearchableOptionContributor {
  private static final Logger LOG = Logger.getInstance(IntentionSearchableOptionContributor.class);

  @Override
  public void processOptions(@Nonnull SearchableOptionProcessor processor) {
    for (IntentionActionBean bean : IntentionManager.EP_INTENTION_ACTIONS.getExtensionList()) {
      String[] categories = bean.getCategories();

      if (categories == null) {
        continue;
      }
      String descriptionDirectoryName = bean.getDescriptionDirectoryName();

      IntentionActionWrapper intentionAction = new IntentionActionWrapper(bean);
      if (descriptionDirectoryName == null) {
        descriptionDirectoryName = IntentionManagerImpl.getDescriptionDirectoryName(intentionAction);
      }

      IntentionActionMetaData data = new IntentionActionMetaData(intentionAction, IntentionManagerSettings.getClassLoader(intentionAction), categories, descriptionDirectoryName);

      final TextDescriptor description = data.getDescription();

      try {
        String descriptionText = description.getText().toLowerCase();
        descriptionText = IntentionManagerSettings.HTML_PATTERN.matcher(descriptionText).replaceAll(" ");
        final Set<String> words = processor.getProcessedWordsWithoutStemming(descriptionText);
        words.addAll(processor.getProcessedWords(data.getFamily()));
        for (String word : words) {
          processor.addOption(word, data.getFamily(), data.getFamily(), "editor.code.intentions", CodeInsightBundle.message("intention.settings"));
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }
}
