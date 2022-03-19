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

package com.intellij.application.options.colors;

import consulo.application.AllIcons;
import com.intellij.openapi.application.ApplicationNamesInfo;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.util.EventDispatcher;
import consulo.ui.ex.awt.JBUI;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.LogicalPosition;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.document.Document;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.awt.*;

public class FontEditorPreview implements PreviewPanel{
  private final EditorEx myEditor;

  private final ColorAndFontOptions myOptions;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  FontEditorPreview(final ColorAndFontOptions options, boolean editable) {
    myOptions = options;

    @Nls String text = getIDEDemoText();

    myEditor = (EditorEx)createPreviewEditor(text, 10, 3, -1, myOptions, editable);

    installTrafficLights(myEditor);
  }

  public static String getIDEDemoText() {
    return
            ApplicationNamesInfo.getInstance().getFullProductName() +
            " is a full-featured IDE\n" +
            "with a high level of usability and outstanding\n" +
            "advanced code editing and refactoring support.\n" +
            "\n" +
            "abcdefghijklmnopqrstuvwxyz 0123456789 (){}[]\n" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ +-*/= .,;:!? #&$%@|^\n" +
            // Create empty lines in order to make the gutter wide enough to display two-digits line numbers (other previews use long text
            // and we don't want different gutter widths on color pages switching).
            "\n" +
            "\n" +
            "\n";
  }

  static void installTrafficLights(@Nonnull EditorEx editor) {
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeRenderer(new DumbTrafficLightRenderer());
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeVisible(true);
  }

  private static class DumbTrafficLightRenderer implements ErrorStripeRenderer {
    @Override
    public void paint(@Nonnull Component c, Graphics g, @Nonnull Rectangle r) {
      Image icon = AllIcons.General.InspectionsOK;
      TargetAWT.to(icon).paintIcon(c, g, r.x, r.y);
    }

    @Override
    public int getSquareSize() {
      return AllIcons.General.InspectionsOK.getHeight();
    }
  }

  static Editor createPreviewEditor(String text, int column, int line, int selectedLine, ColorAndFontOptions options, boolean editable) {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document editorDocument = editorFactory.createDocument(text);
    EditorEx editor = (EditorEx) (editable ? editorFactory.createEditor(editorDocument) : editorFactory.createViewer(editorDocument));
    editor.setColorsScheme(options.getSelectedScheme());
    EditorSettings settings = editor.getSettings();
    settings.setLineNumbersShown(true);
    settings.setWhitespacesShown(true);
    settings.setLineMarkerAreaShown(false);
    settings.setIndentGuidesShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setAdditionalColumnsCount(0);
    settings.setAdditionalLinesCount(0);
    settings.setRightMarginShown(true);
    settings.setRightMargin(60);

    LogicalPosition pos = new LogicalPosition(line, column);
    editor.getCaretModel().moveToLogicalPosition(pos);
    if (selectedLine >= 0) {
      editor.getSelectionModel().setSelection(editorDocument.getLineStartOffset(selectedLine),
                                              editorDocument.getLineEndOffset(selectedLine));
    }
    editor.setBorder(JBUI.Borders.empty());

    return editor;
  }

  @Override
  public Component getPanel() {
    return myEditor.getComponent();
  }

  @Override
  public void updateView() {
    EditorColorsScheme scheme = updateOptionsScheme(myOptions.getSelectedScheme());

    myEditor.setColorsScheme(scheme);
    myEditor.reinitSettings();

  }

  protected EditorColorsScheme updateOptionsScheme(EditorColorsScheme selectedScheme) {
    return selectedScheme;
  }

  @Override
  public void blinkSelectedHighlightType(Object description) {
  }

  @Override
  public void addListener(@Nonnull final ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void disposeUIResources() {
    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myEditor);
  }
}
