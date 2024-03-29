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
package consulo.language.editor.impl.internal.postfixTemplate;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.impl.internal.intention.ActionUsagePanel;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.language.editor.internal.postfixTemplate.PostfixTemplateMetaData;
import consulo.language.file.FileTypeManager;
import consulo.language.plain.PlainTextFileType;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class PostfixDescriptionPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(PostfixDescriptionPanel.class);
  private JPanel myPanel;

  private JPanel myAfterPanel;
  private JPanel myBeforePanel;
  private JEditorPane myDescriptionBrowser;

  public PostfixDescriptionPanel() {
    initializeExamplePanel(myAfterPanel);
    initializeExamplePanel(myBeforePanel);
  }

  public void reset(@Nonnull PostfixTemplateMetaData actionMetaData) {

    final TextDescriptor url = actionMetaData.getDescription();
    final String description = getDescription(url);
    myDescriptionBrowser.setText(description);

    showUsages(myBeforePanel, ArrayUtil.getFirstElement(actionMetaData.getExampleUsagesBefore()));
    showUsages(myAfterPanel, ArrayUtil.getFirstElement(actionMetaData.getExampleUsagesAfter()));
  }

  @Nonnull
  private static String getDescription(TextDescriptor url) {
    try {
      return url.getText();
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return "";
  }

  private static void showUsages(@Nonnull JPanel panel, @Nullable TextDescriptor exampleUsage) {
    String text = "";
    FileType fileType = PlainTextFileType.INSTANCE;
    if (exampleUsage != null) {
      try {
        text = exampleUsage.getText();
        String name = exampleUsage.getFileName();
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        String extension = FileUtil.getExtension(name);
        fileType = fileTypeManager.getFileTypeByExtension(extension);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    ((ActionUsagePanel)panel.getComponent(0)).reset(text, fileType);
    panel.repaint();
  }

  private void initializeExamplePanel(@Nonnull JPanel panel) {
    panel.setLayout(new BorderLayout());
    ActionUsagePanel actionUsagePanel = new ActionUsagePanel();
    panel.add(actionUsagePanel);
    Disposer.register(this, actionUsagePanel);
  }

  public JPanel getComponent() {
    return myPanel;
  }

  @Override
  public void dispose() {
  }
}
