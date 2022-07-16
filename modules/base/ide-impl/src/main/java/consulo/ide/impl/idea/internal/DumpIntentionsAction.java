/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 28-Jun-2007
 */
package consulo.ide.impl.idea.internal;

import consulo.application.dumb.DumbAware;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ide.impl.idea.codeInsight.intention.impl.config.IntentionManagerSettings;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DumpIntentionsAction extends AnAction implements DumbAware {
  public DumpIntentionsAction() {
    super("Dump Intentions");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final VirtualFile file =
      IdeaFileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), e.getData(CommonDataKeys.PROJECT), null);
    if (file != null) {
      final List<IntentionActionMetaData> list = IntentionManagerSettings.getInstance().getMetaData();
      final File root = VfsUtil.virtualToIoFile(file);
      Element el = new Element("root");
      Map<String, Element> categoryMap = new HashMap<String, Element>();
      for (IntentionActionMetaData metaData : list) {

        try {
          Element metadataElement = new Element("intention");
          metadataElement.setAttribute("text", metaData.getActionText());
          metadataElement.setAttribute("description", metaData.getDescription().getText());

          String key = StringUtil.join(metaData.myCategory, ".");
          Element element = getCategoryElement(categoryMap, el, metaData, key, metaData.myCategory.length - 1);
          element.addContent(metadataElement);
        }
        catch (IOException e1) {
          e1.printStackTrace();
        }
      }

      try {
        JDOMUtil.writeDocument(new Document(el), new File(root, "intentions.xml"), "\n");
      }
      catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }

  private static Element getCategoryElement(Map<String, Element> categoryMap, Element rootElement, IntentionActionMetaData metaData, String key, int idx) {
    Element element = categoryMap.get(key);
    if (element == null) {

      element = new Element("category");
      element.setAttribute("name", metaData.myCategory[idx]);
      categoryMap.put(key, element);
      if (idx == 0) {
        rootElement.addContent(element);
      } else {
        getCategoryElement(categoryMap, rootElement, metaData, StringUtil.join(metaData.myCategory, 0, metaData.myCategory.length - 1, "."), idx - 1).addContent(element);
      }
    }
    return element;
  }

  @Override
  public void update(final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}