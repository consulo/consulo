// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.ui.UISettings;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.component.util.Iconable;
import consulo.ide.impl.VfsIconUtil;
import consulo.ide.impl.idea.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.platform.Platform;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearch;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

class RecentLocationsRenderer extends ColoredListCellRenderer<RecentLocationItem> {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final SpeedSearch mySpeedSearch;
  @Nonnull
  private final RecentLocationsDataModel myData;
  @Nonnull
  private final JBCheckBox myCheckBox;

  RecentLocationsRenderer(
    @Nonnull Project project,
    @Nonnull SpeedSearch speedSearch,
    @Nonnull RecentLocationsDataModel data,
    @Nonnull JBCheckBox checkBox
  ) {
    myProject = project;
    mySpeedSearch = speedSearch;
    myData = data;
    myCheckBox = checkBox;
  }

  @Override
  public Component getListCellRendererComponent(
    JList<? extends RecentLocationItem> list,
    RecentLocationItem value,
    int index,
    boolean selected,
    boolean hasFocus
  ) {
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
  private static JComponent createTitleComponent(
    @Nonnull Project project,
    @Nonnull JList<? extends RecentLocationItem> list,
    @Nonnull SpeedSearch speedSearch,
    @Nullable String breadcrumb,
    @Nonnull IdeDocumentHistoryImpl.PlaceInfo placeInfo,
    @Nonnull EditorColorsScheme colorsScheme,
    boolean selected
  ) {
    JComponent title = JBUI.Panels.simplePanel()
      .withBorder(JBUI.Borders.empty())
      .addToLeft(createTitleTextComponent(project, list, speedSearch, placeInfo, colorsScheme, breadcrumb, selected));

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
  private static JComponent setupEditorComponent(
    @Nonnull EditorEx editor,
    @Nonnull String text,
    @Nonnull SpeedSearch speedSearch,
    @Nonnull EditorColorsScheme colorsScheme,
    boolean selected
  ) {
    Iterable<MatcherTextRange> ranges = speedSearch.matchingFragments(text);
    if (ranges != null) {
      selectSearchResultsInEditor(editor, ranges.iterator());
    }
    else {
      RecentLocationsAction.clearSelectionInEditor(editor);
    }

    editor.setBackgroundColor(getBackgroundColor(colorsScheme, selected));
    editor.setBorder(JBUI.Borders.empty(0, 4, 6, 0));

    String emptyFileText = IdeLocalize.recentLocationsPopupEmptyFileText().get();
    if (emptyFileText.equals(editor.getDocument().getText())) {
      editor.getMarkupModel().addRangeHighlighter(
        0,
        emptyFileText.length(),
        HighlighterLayer.SYNTAX,
        createEmptyTextForegroundTextAttributes(colorsScheme),
        HighlighterTargetArea.EXACT_RANGE
      );
    }

    return editor.getComponent();
  }

  @Nonnull
  private static SimpleColoredComponent createTitleTextComponent(
    @Nonnull Project project,
    @Nonnull JList<? extends RecentLocationItem> list,
    @Nonnull SpeedSearch speedSearch,
    @Nonnull IdeDocumentHistoryImpl.PlaceInfo placeInfo,
    @Nonnull EditorColorsScheme colorsScheme,
    @Nullable String breadcrumbText,
    boolean selected
  ) {
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

    if (!Platform.current().os().isWindows()) {
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

    return TextAttributesUtil.fromTextAttributes(textAttributes);
  }

  @Nonnull
  private static SimpleTextAttributes createBreadcrumbsTextAttributes(@Nonnull EditorColorsScheme colorsScheme, boolean selected) {
    ColorValue backgroundColor = getBackgroundColor(colorsScheme, selected);
    TextAttributes attributes = colorsScheme.getAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
    if (attributes != null) {
      ColorValue unusedForeground = attributes.getForegroundColor();
      if (unusedForeground != null) {
        return TextAttributesUtil.fromTextAttributes(
          new TextAttributes(unusedForeground, backgroundColor, null, null, Font.PLAIN)
        );
      }
    }

    return TextAttributesUtil.fromTextAttributes(createDefaultTextAttributesWithBackground(colorsScheme, backgroundColor));
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
    return unusedAttributes != null ? unusedAttributes : TextAttributesUtil.toTextAttributes(SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  @Override
  protected void customizeCellRenderer(
    @Nonnull JList<? extends RecentLocationItem> list,
    RecentLocationItem value,
    int index,
    boolean selected,
    boolean hasFocus
  ) {
  }

  private static void selectSearchResultsInEditor(@Nonnull Editor editor, @Nonnull Iterator<? extends MatcherTextRange> resultIterator) {
    if (!editor.getCaretModel().supportsMultipleCarets()) {
      return;
    }
    ArrayList<CaretState> caretStates = new ArrayList<>();
    while (resultIterator.hasNext()) {
      MatcherTextRange findResult = resultIterator.next();

      int caretOffset = findResult.getEndOffset();

      int selectionStartOffset = findResult.getStartOffset();
      int selectionEndOffset = findResult.getEndOffset();
      EditorActionUtil.makePositionVisible(editor, caretOffset);
      EditorActionUtil.makePositionVisible(editor, selectionStartOffset);
      EditorActionUtil.makePositionVisible(editor, selectionEndOffset);
      caretStates.add(new CaretState(
        editor.offsetToLogicalPosition(caretOffset),
        editor.offsetToLogicalPosition(selectionStartOffset), editor.offsetToLogicalPosition(selectionEndOffset)
      ));
    }
    if (caretStates.isEmpty()) {
      return;
    }
    editor.getCaretModel().setCaretsAndSelections(caretStates);
  }
}
