// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.speedSearch.SpeedSearch;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.FontUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.fileTypes.impl.VfsIconUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

import static com.intellij.ide.actions.RecentLocationsAction.EMPTY_FILE_TEXT;

class RecentLocationsRenderer extends ColoredListCellRenderer<RecentLocationItem> {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final SpeedSearch mySpeedSearch;
  @Nonnull
  private final RecentLocationsDataModel myData;
  @Nonnull
  private final JBCheckBox myCheckBox;

  RecentLocationsRenderer(@Nonnull Project project, @Nonnull SpeedSearch speedSearch, @Nonnull RecentLocationsDataModel data, @Nonnull JBCheckBox checkBox) {
    myProject = project;
    mySpeedSearch = speedSearch;
    myData = data;
    myCheckBox = checkBox;
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends RecentLocationItem> list, RecentLocationItem value, int index, boolean selected, boolean hasFocus) {
    EditorEx editor = value.getEditor();
    if (myProject.isDisposed() || editor.isDisposed()) {
      return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
    }

    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    String breadcrumbs = myData.getBreadcrumbsMap(myCheckBox.isSelected()).get(value.getInfo());
    JPanel panel = new JPanel(new VerticalFlowLayout(0, 0));
    if (index != 0) {
      panel.add(createSeparatorLine(colorsScheme));
    }
    panel.add(createTitleComponent(myProject, list, mySpeedSearch, breadcrumbs, value.getInfo(), colorsScheme, selected));
    panel.add(setupEditorComponent(editor, editor.getDocument().getText(), mySpeedSearch, colorsScheme, selected));

    return panel;
  }

  @Nonnull
  private static ColorValue getBackgroundColor(@Nonnull EditorColorsScheme colorsScheme, boolean selected) {
    return selected ? HintUtil.getRecentLocationsSelectionColor(colorsScheme) : colorsScheme.getDefaultBackground();
  }

  @Nonnull
  private static JComponent createTitleComponent(@Nonnull Project project,
                                                 @Nonnull JList<? extends RecentLocationItem> list,
                                                 @Nonnull SpeedSearch speedSearch,
                                                 @Nullable String breadcrumb,
                                                 @Nonnull IdeDocumentHistoryImpl.PlaceInfo placeInfo,
                                                 @Nonnull EditorColorsScheme colorsScheme,
                                                 boolean selected) {
    JComponent title = JBUI.Panels.simplePanel().withBorder(JBUI.Borders.empty()).addToLeft(createTitleTextComponent(project, list, speedSearch, placeInfo, colorsScheme, breadcrumb, selected));

    title.setBorder(JBUI.Borders.empty(8, 6, 5, 0));
    title.setBackground(TargetAWT.to(getBackgroundColor(colorsScheme, selected)));

    return title;
  }

  @Nonnull
  private static JPanel createSeparatorLine(@Nonnull EditorColorsScheme colorsScheme) {
    Color color = TargetAWT.to(colorsScheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR));
    if (color == null) {
      color = JBColor.namedColor("Group.separatorColor", new JBColor(Gray.xCD, Gray.x51));
    }

    return JBUI.Panels.simplePanel().withBorder(JBUI.Borders.customLine(color, 1, 0, 0, 0));
  }

  @Nonnull
  private static JComponent setupEditorComponent(@Nonnull EditorEx editor, @Nonnull String text, @Nonnull SpeedSearch speedSearch, @Nonnull EditorColorsScheme colorsScheme, boolean selected) {
    Iterable<TextRange> ranges = speedSearch.matchingFragments(text);
    if (ranges != null) {
      selectSearchResultsInEditor(editor, ranges.iterator());
    }
    else {
      RecentLocationsAction.clearSelectionInEditor(editor);
    }

    editor.setBackgroundColor(getBackgroundColor(colorsScheme, selected));
    editor.setBorder(JBUI.Borders.empty(0, 4, 6, 0));

    if (EMPTY_FILE_TEXT.equals(editor.getDocument().getText())) {
      editor.getMarkupModel().addRangeHighlighter(0, EMPTY_FILE_TEXT.length(), HighlighterLayer.SYNTAX, createEmptyTextForegroundTextAttributes(colorsScheme), HighlighterTargetArea.EXACT_RANGE);
    }

    return editor.getComponent();
  }

  @Nonnull
  private static SimpleColoredComponent createTitleTextComponent(@Nonnull Project project,
                                                                 @Nonnull JList<? extends RecentLocationItem> list,
                                                                 @Nonnull SpeedSearch speedSearch,
                                                                 @Nonnull IdeDocumentHistoryImpl.PlaceInfo placeInfo,
                                                                 @Nonnull EditorColorsScheme colorsScheme,
                                                                 @Nullable String breadcrumbText,
                                                                 boolean selected) {
    SimpleColoredComponent titleTextComponent = new SimpleColoredComponent();

    String fileName = placeInfo.getFile().getName();
    String text = fileName;
    titleTextComponent.append(fileName, createFileNameTextAttributes(colorsScheme, selected));

    if (StringUtil.isNotEmpty(breadcrumbText) && !StringUtil.equals(breadcrumbText, fileName)) {
      text += " " + breadcrumbText;
      titleTextComponent.append("  ");
      titleTextComponent.append(breadcrumbText, createBreadcrumbsTextAttributes(colorsScheme, selected));
    }

    Image icon = fetchIcon(project, placeInfo);

    if (icon != null) {
      titleTextComponent.setIcon(icon);
      titleTextComponent.setIconTextGap(4);
    }

    titleTextComponent.setBorder(JBUI.Borders.empty());

    if (!SystemInfo.isWindows) {
      titleTextComponent.setFont(FontUtil.minusOne(UIUtil.getLabelFont()));
    }

    if (speedSearch.matchingFragments(text) != null) {
      SpeedSearchUtil.applySpeedSearchHighlighting(list, titleTextComponent, false, selected);
    }

    long timeStamp = placeInfo.getTimeStamp();
    if (UISettings.getInstance().getShowInplaceComments() && Registry.is("show.last.visited.timestamps", true) && timeStamp != -1) {
      titleTextComponent.append(" " + DateFormatUtil.formatPrettyDateTime(timeStamp), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
    }

    return titleTextComponent;
  }

  @Nullable
  private static Image fetchIcon(@Nonnull Project project, @Nonnull IdeDocumentHistoryImpl.PlaceInfo placeInfo) {
    return VfsIconUtil.getIcon(placeInfo.getFile(), Iconable.ICON_FLAG_READ_STATUS, project);
  }

  @Nonnull
  private static SimpleTextAttributes createFileNameTextAttributes(@Nonnull EditorColorsScheme colorsScheme, boolean selected) {
    TextAttributes textAttributes = createDefaultTextAttributesWithBackground(colorsScheme, getBackgroundColor(colorsScheme, selected));
    textAttributes.setFontType(Font.BOLD);

    return SimpleTextAttributes.fromTextAttributes(textAttributes);
  }

  @Nonnull
  private static SimpleTextAttributes createBreadcrumbsTextAttributes(@Nonnull EditorColorsScheme colorsScheme, boolean selected) {
    ColorValue backgroundColor = getBackgroundColor(colorsScheme, selected);
    TextAttributes attributes = colorsScheme.getAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
    if (attributes != null) {
      ColorValue unusedForeground = attributes.getForegroundColor();
      if (unusedForeground != null) {
        return SimpleTextAttributes.fromTextAttributes(new TextAttributes(unusedForeground, backgroundColor, null, null, Font.PLAIN));
      }
    }

    return SimpleTextAttributes.fromTextAttributes(createDefaultTextAttributesWithBackground(colorsScheme, backgroundColor));
  }

  @Nonnull
  private static TextAttributes createDefaultTextAttributesWithBackground(@Nonnull EditorColorsScheme colorsScheme, @Nonnull ColorValue backgroundColor) {
    TextAttributes defaultTextAttributes = new TextAttributes();
    TextAttributes textAttributes = colorsScheme.getAttributes(HighlighterColors.TEXT);
    if (textAttributes != null) {
      defaultTextAttributes = textAttributes.clone();
      defaultTextAttributes.setBackgroundColor(backgroundColor);
    }

    return defaultTextAttributes;
  }

  @Nonnull
  private static TextAttributes createEmptyTextForegroundTextAttributes(@Nonnull EditorColorsScheme colorsScheme) {
    TextAttributes unusedAttributes = colorsScheme.getAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
    return unusedAttributes != null ? unusedAttributes : SimpleTextAttributes.GRAYED_ATTRIBUTES.toTextAttributes();
  }

  @Override
  protected void customizeCellRenderer(@Nonnull JList<? extends RecentLocationItem> list, RecentLocationItem value, int index, boolean selected, boolean hasFocus) {
  }

  private static void selectSearchResultsInEditor(@Nonnull Editor editor, @Nonnull Iterator<? extends TextRange> resultIterator) {
    if (!editor.getCaretModel().supportsMultipleCarets()) {
      return;
    }
    ArrayList<CaretState> caretStates = new ArrayList<>();
    while (resultIterator.hasNext()) {
      TextRange findResult = resultIterator.next();

      int caretOffset = findResult.getEndOffset();

      int selectionStartOffset = findResult.getStartOffset();
      int selectionEndOffset = findResult.getEndOffset();
      EditorActionUtil.makePositionVisible(editor, caretOffset);
      EditorActionUtil.makePositionVisible(editor, selectionStartOffset);
      EditorActionUtil.makePositionVisible(editor, selectionEndOffset);
      caretStates.add(new CaretState(editor.offsetToLogicalPosition(caretOffset), editor.offsetToLogicalPosition(selectionStartOffset), editor.offsetToLogicalPosition(selectionEndOffset)));
    }
    if (caretStates.isEmpty()) {
      return;
    }
    editor.getCaretModel().setCaretsAndSelections(caretStates);
  }
}
