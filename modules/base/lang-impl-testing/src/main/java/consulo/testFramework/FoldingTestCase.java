/*
 * Copyright 2013-2016 consulo.io
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
package consulo.testFramework;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import consulo.annotation.UsedInPlugin;
import junit.framework.Assert;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 08.04.2016
 */
public class FoldingTestCase extends OneFileAtProjectTestCase {
  private class Border implements Comparable<Border> {
    public static final boolean LEFT = true;
    public static final boolean RIGHT = false;
    public boolean mySide;
    public int myOffset;
    public String myText;
    public boolean myIsExpanded;

    private Border(boolean side, int offset, String text, boolean isExpanded) {
      mySide = side;
      myOffset = offset;
      myText = text;
      myIsExpanded = isExpanded;
    }

    public boolean isExpanded() {
      return myIsExpanded;
    }

    public boolean isSide() {
      return mySide;
    }

    public int getOffset() {
      return myOffset;
    }

    public String getText() {
      return myText;
    }

    @Override
    public int compareTo(Border o) {
      return getOffset() < o.getOffset() ? 1 : -1;
    }
  }

  private static final String START_FOLD = "<fold\\stext=\'[^\']*\'(\\sexpand=\'[^\']*\')*>";
  private static final String END_FOLD = "</fold>";

  private boolean myDoCheckCollapseStatus;

  public FoldingTestCase(@NonNls @Nonnull String dataPath, @Nonnull String ext) {
    super(dataPath, ext);
  }

  @UsedInPlugin
  protected void withCheckCollapseState() {
    myDoCheckCollapseStatus = true;
  }

  @Override
  protected void runTestInternal() throws Throwable {
    String filePath = myFullDataPath + "/" + getTestName(false) + "." + myExtension;
    File file = new File(filePath);

    String expectedContent;
    try {
      expectedContent = FileUtil.loadFile(file);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    Assert.assertNotNull(expectedContent);

    expectedContent = StringUtil.replace(expectedContent, "\r", "");
    final String cleanContent = expectedContent.replaceAll(START_FOLD, "").replaceAll(END_FOLD, "");
    final String actual = getFoldingDescription(cleanContent, file.getName(), myDoCheckCollapseStatus);

    Assert.assertEquals(expectedContent, actual);
  }

  @Nonnull
  private String getFoldingDescription(@Nonnull String content, @Nonnull String fileName, boolean doCheckCollapseStatus) {
    FileType fileTypeByFileName = FileTypeManager.getInstance().getFileTypeByFileName(fileName);

    PsiFile file = createFile(fileName, fileTypeByFileName, content);

    final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);

    Editor editor = fileEditorManager.openTextEditor(new OpenFileDescriptor(myProject, file.getVirtualFile()), false);

    CodeFoldingManager.getInstance(myProject).buildInitialFoldings(editor);

    final FoldingModel model = editor.getFoldingModel();
    final FoldRegion[] foldingRegions = model.getAllFoldRegions();
    final List<Border> borders = new LinkedList<Border>();

    for (FoldRegion region : foldingRegions) {
      borders.add(new Border(Border.LEFT, region.getStartOffset(), region.getPlaceholderText(), region.isExpanded()));
      borders.add(new Border(Border.RIGHT, region.getEndOffset(), "", region.isExpanded()));
    }
    Collections.sort(borders);

    StringBuilder result = new StringBuilder(editor.getDocument().getText());
    for (Border border : borders) {
      result.insert(border.getOffset(), border.isSide() == Border.LEFT ? "<fold text=\'" + border.getText() + "\'" +
                                                                         (doCheckCollapseStatus ? " expand=\'" +
                                                                                                  border.isExpanded() +
                                                                                                  "\'" : "") +
                                                                         ">" : END_FOLD);
    }

    return result.toString();
  }
}
