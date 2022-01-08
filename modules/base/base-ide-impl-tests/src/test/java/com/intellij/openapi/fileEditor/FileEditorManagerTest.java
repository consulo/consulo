/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.fileEditor;

import com.intellij.ide.ui.UISettings;
import com.intellij.mock.Mock;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.components.PathMacroUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestExtensionUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Dmitry Avdeev
 *         Date: 4/16/13
 */
public abstract class FileEditorManagerTest extends FileEditorManagerTestCase {
  public void testTabOrder() throws Exception {

    openFiles(STRING);
    assertOpenFiles("1.txt", "foo.xml", "2.txt", "3.txt");
  }

  public void testTabLimit() throws Exception {

    int limit = UISettings.getInstance().EDITOR_TAB_LIMIT;
    try {
      UISettings.getInstance().EDITOR_TAB_LIMIT = 2;
      openFiles(STRING);
      // note that foo.xml is pinned
      assertOpenFiles("foo.xml", "3.txt");
    }
    finally {
      UISettings.getInstance().EDITOR_TAB_LIMIT = limit;
    }
  }

  public void testOpenRecentEditorTab() throws Exception {
    PsiTestExtensionUtil.registerExtension(FileEditorProvider.EP_FILE_EDITOR_PROVIDER, new MyFileEditorProvider(), getTestRootDisposable());

    openFiles("  <component name=\"FileEditorManager\">\n" +
        "    <leaf>\n" +
        "      <file leaf-file-name=\"foo.xsd\" pinned=\"false\" current=\"true\" current-in-tab=\"true\">\n" +
        "        <entry selected=\"true\" file=\"file://$PROJECT_DIR$/src/1.txt\">\n" +
        "          <provider editor-type-id=\"mock\" selected=\"true\">\n" +
        "            <state />\n" +
        "          </provider>\n" +
        "          <provider editor-type-id=\"text-editor\">\n" +
        "            <state/>\n" +
        "          </provider>\n" +
        "        </entry>\n" +
        "      </file>\n" +
        "    </leaf>\n" +
        "  </component>\n");
    FileEditor[] selectedEditors = myManager.getSelectedEditors();
    assertEquals(1, selectedEditors.length);
    assertEquals("mockEditor", selectedEditors[0].getName());
  }

  public void testTrackSelectedEditor() throws Exception {
    PsiTestExtensionUtil.registerExtension(FileEditorProvider.EP_FILE_EDITOR_PROVIDER, new MyFileEditorProvider(), getTestRootDisposable());
    VirtualFile file = getFile("/src/1.txt");
    assertNotNull(file);
    FileEditor[] editors = myManager.openFile(file, true);
    assertEquals(2, editors.length);
    assertEquals("Text", myManager.getSelectedEditor(file).getName());
    myManager.setSelectedEditor(file, "mock");
    assertEquals("mockEditor", myManager.getSelectedEditor(file).getName());

    VirtualFile file1 = getFile("/src/2.txt");
    myManager.openFile(file1, true);
    assertEquals("mockEditor", myManager.getSelectedEditor(file).getName());
  }

  private static final String STRING = "<component name=\"FileEditorManager\">\n" +
      "    <leaf>\n" +
      "      <file leaf-file-name=\"1.txt\" pinned=\"false\" current=\"false\" current-in-tab=\"false\">\n" +
      "        <entry file=\"file://$PROJECT_DIR$/src/1.txt\">\n" +
      "          <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
      "            <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
      "            </state>\n" +
      "          </provider>\n" +
      "        </entry>\n" +
      "      </file>\n" +
      "      <file leaf-file-name=\"foo.xml\" pinned=\"true\" current=\"false\" current-in-tab=\"false\">\n" +
      "        <entry file=\"file://$PROJECT_DIR$/src/foo.xml\">\n" +
      "          <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
      "            <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
      "            </state>\n" +
      "          </provider>\n" +
      "        </entry>\n" +
      "      </file>\n" +
      "      <file leaf-file-name=\"2.txt\" pinned=\"false\" current=\"true\" current-in-tab=\"true\">\n" +
      "        <entry file=\"file://$PROJECT_DIR$/src/2.txt\">\n" +
      "          <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
      "            <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
      "            </state>\n" +
      "          </provider>\n" +
      "        </entry>\n" +
      "      </file>\n" +
      "      <file leaf-file-name=\"3.txt\" pinned=\"false\" current=\"false\" current-in-tab=\"false\">\n" +
      "        <entry file=\"file://$PROJECT_DIR$/src/3.txt\">\n" +
      "          <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
      "            <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
      "            </state>\n" +
      "          </provider>\n" +
      "        </entry>\n" +
      "      </file>\n" +
      "    </leaf>\n" +
      "  </component>\n";

  private void assertOpenFiles(String... fileNames) {
    EditorWithProviderComposite[] files = myManager.getSplitters().getEditorsComposites();
    List<String> names = ContainerUtil.map(files, new Function<EditorWithProviderComposite, String>() {
      @Override
      public String fun(EditorWithProviderComposite composite) {
        return composite.getFile().getName();
      }
    });
    assertEquals(Arrays.asList(fileNames), names);
  }

  private void openFiles(String s) throws IOException, JDOMException, InterruptedException, ExecutionException {
    Document document = JDOMUtil.loadDocument(s);
    Element rootElement = document.getRootElement();
    ExpandMacroToPathMap map = new ExpandMacroToPathMap();
    map.addMacroExpand(PathMacroUtil.PROJECT_DIR_MACRO_NAME, getTestDataPath());
    map.substitute(rootElement, true, true);

    myManager.loadState(rootElement);

    UIAccess uiAccess = UIAccess.get();
    Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        myManager.getMainSplitters().openFiles(uiAccess);
      }
    });
    future.get();
  }

  @Override
  protected String getTestDataPath() {
    return PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/') + "/platform/platform-tests/testData/fileEditorManager";
  }

  static class MyFileEditorProvider implements FileEditorProvider {
    @Nonnull
    @Override
    public String getEditorTypeId() {
      return "mock";
    }

    @Nonnull
    @Override
    public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
      return FileEditorState.INSTANCE;
    }

    @Override
    public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
    }

    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
      return true;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
      return new Mock.MyFileEditor() {
        @Override
        public boolean isValid() {
          return true;
        }

        @Nonnull
        @Override
        public JComponent getComponent() {
          return new JLabel();
        }

        @Nonnull
        @Override
        public String getName() {
          return "mockEditor";
        }
      };
    }

    @Override
    public void disposeEditor(@Nonnull FileEditor editor) {
    }

    @Nonnull
    @Override
    public FileEditorPolicy getPolicy() {
      return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
  }
}
