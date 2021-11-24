/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.diff.util;

import com.intellij.codeStyle.CodeStyleFacade;
import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffTool;
import com.intellij.diff.SuppressiveDiffTool;
import com.intellij.diff.comparison.ByWord;
import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.contents.EmptyContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.diff.fragments.DiffFragment;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.diff.fragments.MergeLineFragment;
import com.intellij.diff.impl.DiffSettingsHolder;
import com.intellij.diff.impl.DiffSettingsHolder.DiffSettings;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.simple.MergeInnerDifferences;
import com.intellij.diff.tools.util.base.HighlightPolicy;
import com.intellij.diff.tools.util.base.IgnorePolicy;
import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.command.undo.DocumentReferenceManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diff.DiffBundle;
import com.intellij.openapi.diff.impl.GenericDataProvider;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapperDialog;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.WindowWrapper;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.DocumentUtil;
import com.intellij.util.LineSeparator;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBValue;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.function.BiPredicate;

public class DiffUtil {
  private static final Logger LOG = Logger.getInstance(DiffUtil.class);

  @Nonnull
  public static final String DIFF_CONFIG = StoragePathMacros.APP_CONFIG + "/diff.xml";
  public static final JBValue TITLE_GAP = new JBValue.Float(2);

  //
  // Editor
  //

  public static boolean isDiffEditor(@Nonnull Editor editor) {
    return editor.getEditorKind() == EditorKind.DIFF;
  }

  @Nullable
  public static EditorHighlighter initEditorHighlighter(@Nullable Project project,
                                                        @Nonnull DocumentContent content,
                                                        @Nonnull CharSequence text) {
    EditorHighlighter highlighter = createEditorHighlighter(project, content);
    if (highlighter == null) return null;
    highlighter.setText(text);
    return highlighter;
  }

  @Nonnull
  public static EditorHighlighter initEmptyEditorHighlighter(@Nonnull CharSequence text) {
    EditorHighlighter highlighter = createEmptyEditorHighlighter();
    highlighter.setText(text);
    return highlighter;
  }

  @Nullable
  private static EditorHighlighter createEditorHighlighter(@Nullable Project project, @Nonnull DocumentContent content) {
    FileType type = content.getContentType();
    VirtualFile file = content.getHighlightFile();
    Language language = content.getUserData(DiffUserDataKeys.LANGUAGE);

    EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
    if (language != null) {
      SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
      return highlighterFactory.createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().getGlobalScheme());
    }
    if (file != null) {
      if ((type == null || type == PlainTextFileType.INSTANCE) || file.getFileType() == type || file instanceof LightVirtualFile) {
        return highlighterFactory.createEditorHighlighter(project, file);
      }
    }
    if (type != null) {
      return highlighterFactory.createEditorHighlighter(project, type);
    }
    return null;
  }

  @Nonnull
  private static EditorHighlighter createEmptyEditorHighlighter() {
    return new EmptyEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT));
  }

  public static void setEditorHighlighter(@Nullable Project project, @Nonnull EditorEx editor, @Nonnull DocumentContent content) {
    EditorHighlighter highlighter = createEditorHighlighter(project, content);
    if (highlighter != null) editor.setHighlighter(highlighter);
  }

  public static void setEditorCodeStyle(@Nullable Project project, @Nonnull EditorEx editor, @Nullable FileType fileType) {
    if (project != null && fileType != null) {
      CodeStyleFacade codeStyleFacade = CodeStyleFacade.getInstance(project);
      editor.getSettings().setTabSize(codeStyleFacade.getTabSize(fileType));
      editor.getSettings().setUseTabCharacter(codeStyleFacade.useTabCharacter(fileType));
    }
    editor.getSettings().setCaretRowShown(false);
    editor.reinitSettings();
  }

  public static void setFoldingModelSupport(@Nonnull EditorEx editor) {
    editor.getSettings().setFoldingOutlineShown(true);
    editor.getSettings().setAutoCodeFoldingEnabled(false);
    editor.getColorsScheme().setAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES, null);
  }

  @Nonnull
  public static EditorEx createEditor(@Nonnull Document document, @Nullable Project project, boolean isViewer) {
    return createEditor(document, project, isViewer, false);
  }

  @Nonnull
  public static EditorEx createEditor(@Nonnull Document document, @Nullable Project project, boolean isViewer, boolean enableFolding) {
    EditorFactory factory = EditorFactory.getInstance();
    EditorEx editor = (EditorEx)(isViewer ? factory.createViewer(document, project, EditorKind.DIFF) : factory.createEditor(document, project, EditorKind.DIFF));

    editor.getSettings().setShowIntentionBulb(false);
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeVisible(true);
    editor.getGutterComponentEx().setShowDefaultGutterPopup(false);

    if (enableFolding) {
      setFoldingModelSupport(editor);
    } else {
      editor.getSettings().setFoldingOutlineShown(false);
      editor.getFoldingModel().setFoldingEnabled(false);
    }

    UIUtil.removeScrollBorder(editor.getComponent());

    return editor;
  }

  public static void configureEditor(@Nonnull EditorEx editor, @Nonnull DocumentContent content, @Nullable Project project) {
    setEditorHighlighter(project, editor, content);
    setEditorCodeStyle(project, editor, content.getContentType());
    editor.reinitSettings();
  }

  public static boolean isMirrored(@Nonnull Editor editor) {
    if (editor instanceof EditorEx) {
      return ((EditorEx)editor).getVerticalScrollbarOrientation() == EditorEx.VERTICAL_SCROLLBAR_LEFT;
    }
    return false;
  }

  //
  // Scrolling
  //

  public static void disableBlitting(@Nonnull EditorEx editor) {
    if (Registry.is("diff.divider.repainting.disable.blitting")) {
      editor.getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    }
  }

  public static void moveCaret(@Nullable final Editor editor, int line) {
    if (editor == null) return;
    editor.getCaretModel().removeSecondaryCarets();
    editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, 0));
  }

  public static void scrollEditor(@Nullable final Editor editor, int line, boolean animated) {
    scrollEditor(editor, line, 0, animated);
  }

  public static void scrollEditor(@Nullable final Editor editor, int line, int column, boolean animated) {
    if (editor == null) return;
    editor.getCaretModel().removeSecondaryCarets();
    editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, column));
    scrollToCaret(editor, animated);
  }

  public static void scrollToPoint(@Nullable Editor editor, @Nonnull Point point, boolean animated) {
    if (editor == null) return;
    if (!animated) editor.getScrollingModel().disableAnimation();
    editor.getScrollingModel().scrollHorizontally(point.x);
    editor.getScrollingModel().scrollVertically(point.y);
    if (!animated) editor.getScrollingModel().enableAnimation();
  }

  public static void scrollToCaret(@Nullable Editor editor, boolean animated) {
    if (editor == null) return;
    if (!animated) editor.getScrollingModel().disableAnimation();
    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    if (!animated) editor.getScrollingModel().enableAnimation();
  }

  @Nonnull
  public static Point getScrollingPosition(@Nullable Editor editor) {
    if (editor == null) return new Point(0, 0);
    ScrollingModel model = editor.getScrollingModel();
    return new Point(model.getHorizontalScrollOffset(), model.getVerticalScrollOffset());
  }

  @Nonnull
  public static LogicalPosition getCaretPosition(@Nullable Editor editor) {
    return editor != null ? editor.getCaretModel().getLogicalPosition() : new LogicalPosition(0, 0);
  }

  //
  // Icons
  //

  @Nonnull
  public static Image getArrowIcon(@Nonnull Side sourceSide) {
    return sourceSide.select(AllIcons.Diff.ArrowRight, AllIcons.Diff.Arrow);
  }

  @Nonnull
  public static Image getArrowDownIcon(@Nonnull Side sourceSide) {
    return sourceSide.select(AllIcons.Diff.ArrowRightDown, AllIcons.Diff.ArrowLeftDown);
  }

  //
  // UI
  //

  public static void registerAction(@Nonnull AnAction action, @Nonnull JComponent component) {
    action.registerCustomShortcutSet(action.getShortcutSet(), component);
  }

  @Nonnull
  public static JPanel createMessagePanel(@Nonnull String message) {
    String text = StringUtil.replace(message, "\n", "<br>");
    JLabel label = new JBLabel(text) {
      @Override
      public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.width = Math.min(size.width, 200);
        size.height = Math.min(size.height, 100);
        return size;
      }
    }.setCopyable(true);
    label.setForeground(UIUtil.getInactiveTextColor());

    return new CenteredPanel(label, JBUI.Borders.empty(5));
  }

  public static void addActionBlock(@Nonnull DefaultActionGroup group, AnAction... actions) {
    addActionBlock(group, Arrays.asList(actions));
  }

  public static void addActionBlock(@Nonnull DefaultActionGroup group, @Nullable List<? extends AnAction> actions) {
    if (actions == null || actions.isEmpty()) return;
    group.addSeparator();

    AnAction[] children = group.getChildren(null);
    for (AnAction action : actions) {
      if (!ArrayUtil.contains(action, children)) {
        group.add(action);
      }
    }
  }

  @Nonnull
  public static String getSettingsConfigurablePath() {
    return "Settings | Tools | Diff";
  }

  @Nonnull
  public static String createTooltipText(@Nonnull String text, @Nullable String appendix) {
    StringBuilder result = new StringBuilder();
    result.append("<html><body>");
    result.append(text);
    if (appendix != null) {
      result.append("<br><div style='margin-top: 5px'><font size='2'>");
      result.append(appendix);
      result.append("</font></div>");
    }
    result.append("</body></html>");
    return result.toString();
  }

  @Nonnull
  public static String createNotificationText(@Nonnull String text, @Nullable String appendix) {
    StringBuilder result = new StringBuilder();
    result.append("<html><body>");
    result.append(text);
    if (appendix != null) {
      result.append("<br><span style='color:#").append(ColorUtil.toHex(JBColor.gray)).append("'><small>");
      result.append(appendix);
      result.append("</small></span>");
    }
    result.append("</body></html>");
    return result.toString();
  }

  public static void showSuccessPopup(@Nonnull String message,
                                      @Nonnull RelativePoint point,
                                      @Nonnull Disposable disposable,
                                      @Nullable Runnable hyperlinkHandler) {
    HyperlinkListener listener = null;
    if (hyperlinkHandler != null) {
      listener = new HyperlinkAdapter() {
        @Override
        protected void hyperlinkActivated(HyperlinkEvent e) {
          hyperlinkHandler.run();
        }
      };
    }

    Color bgColor = MessageType.INFO.getPopupBackground();

    Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, bgColor, listener)
            .setAnimationCycle(200)
            .createBalloon();
    balloon.show(point, Balloon.Position.below);
    Disposer.register(disposable, balloon);
  }

  //
  // Titles
  //

  @Nonnull
  public static List<JComponent> createSimpleTitles(@Nonnull ContentDiffRequest request) {
    List<DiffContent> contents = request.getContents();
    List<String> titles = request.getContentTitles();

    if (!ContainerUtil.exists(titles, Condition.NOT_NULL)) {
      return Collections.nCopies(titles.size(), null);
    }

    List<JComponent> components = new ArrayList<>(titles.size());
    for (int i = 0; i < contents.size(); i++) {
      JComponent title = createTitle(StringUtil.notNullize(titles.get(i)));
      title = createTitleWithNotifications(title, contents.get(i));
      components.add(title);
    }

    return components;
  }

  @Nonnull
  public static List<JComponent> createTextTitles(@Nonnull ContentDiffRequest request, @Nonnull List<? extends Editor> editors) {
    List<DiffContent> contents = request.getContents();
    List<String> titles = request.getContentTitles();

    boolean equalCharsets = TextDiffViewerUtil.areEqualCharsets(contents);
    boolean equalSeparators = TextDiffViewerUtil.areEqualLineSeparators(contents);

    List<JComponent> result = new ArrayList<>(contents.size());

    if (equalCharsets && equalSeparators && !ContainerUtil.exists(titles, Condition.NOT_NULL)) {
      return Collections.nCopies(titles.size(), null);
    }

    for (int i = 0; i < contents.size(); i++) {
      JComponent title = createTitle(StringUtil.notNullize(titles.get(i)), contents.get(i), equalCharsets, equalSeparators, editors.get(i));
      title = createTitleWithNotifications(title, contents.get(i));
      result.add(title);
    }

    return result;
  }

  @Nullable
  private static JComponent createTitleWithNotifications(@Nullable JComponent title,
                                                         @Nonnull DiffContent content) {
    List<JComponent> notifications = getCustomNotifications(content);
    if (notifications.isEmpty()) return title;

    List<JComponent> components = new ArrayList<>();
    if (title != null) components.add(title);
    components.addAll(notifications);
    return createStackedComponents(components, TITLE_GAP.get());
  }

  @Nullable
  private static JComponent createTitle(@Nonnull String title,
                                        @Nonnull DiffContent content,
                                        boolean equalCharsets,
                                        boolean equalSeparators,
                                        @Nullable Editor editor) {
    if (content instanceof EmptyContent) return null;

    Charset charset = equalCharsets ? null : ((DocumentContent)content).getCharset();
    LineSeparator separator = equalSeparators ? null : ((DocumentContent)content).getLineSeparator();
    boolean isReadOnly = editor == null || editor.isViewer() || !canMakeWritable(editor.getDocument());

    return createTitle(title, charset, separator, isReadOnly);
  }

  @Nonnull
  public static JComponent createTitle(@Nonnull String title) {
    return createTitle(title, null, null, false);
  }

  @Nonnull
  public static JComponent createTitle(@Nonnull String title,
                                       @Nullable Charset charset,
                                       @Nullable LineSeparator separator,
                                       boolean readOnly) {
    if (readOnly) title += " " + DiffBundle.message("diff.content.read.only.content.title.suffix");

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(JBUI.Borders.empty(2, 4));
    panel.add(new JBLabel(title).setCopyable(true), BorderLayout.CENTER);
    if (charset != null && separator != null) {
      JPanel panel2 = new JPanel();
      panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
      panel2.add(createCharsetPanel(charset));
      panel2.add(Box.createRigidArea(JBUI.size(4, 0)));
      panel2.add(createSeparatorPanel(separator));
      panel.add(panel2, BorderLayout.EAST);
    }
    else if (charset != null) {
      panel.add(createCharsetPanel(charset), BorderLayout.EAST);
    }
    else if (separator != null) {
      panel.add(createSeparatorPanel(separator), BorderLayout.EAST);
    }
    return panel;
  }

  @Nonnull
  private static JComponent createCharsetPanel(@Nonnull Charset charset) {
    JLabel label = new JLabel(charset.displayName());
    // TODO: specific colors for other charsets
    if (charset.equals(Charset.forName("UTF-8"))) {
      label.setForeground(JBColor.BLUE);
    }
    else if (charset.equals(Charset.forName("ISO-8859-1"))) {
      label.setForeground(JBColor.RED);
    }
    else {
      label.setForeground(JBColor.BLACK);
    }
    return label;
  }

  @Nonnull
  private static JComponent createSeparatorPanel(@Nonnull LineSeparator separator) {
    JLabel label = new JLabel(separator.name());
    Color color;
    if (separator == LineSeparator.CRLF) {
      color = JBColor.RED;
    }
    else if (separator == LineSeparator.LF) {
      color = JBColor.BLUE;
    }
    else if (separator == LineSeparator.CR) {
      color = JBColor.MAGENTA;
    }
    else {
      color = JBColor.BLACK;
    }
    label.setForeground(color);
    return label;
  }

  @Nonnull
  public static List<JComponent> createSyncHeightComponents(@Nonnull final List<JComponent> components) {
    if (!ContainerUtil.exists(components, Condition.NOT_NULL)) return components;
    List<JComponent> result = new ArrayList<>();
    for (int i = 0; i < components.size(); i++) {
      result.add(new SyncHeightComponent(components, i));
    }
    return result;
  }

  @Nonnull
  public static JComponent createStackedComponents(@Nonnull List<JComponent> components, int gap) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    for (int i = 0; i < components.size(); i++) {
      if (i != 0) panel.add(Box.createVerticalStrut(JBUI.scale(gap)));
      panel.add(components.get(i));
    }

    return panel;
  }

  //
  // Focus
  //

  public static boolean isFocusedComponent(@Nullable Component component) {
    return isFocusedComponent(null, component);
  }

  public static boolean isFocusedComponent(@Nullable Project project, @Nullable Component component) {
    if (component == null) return false;
    return IdeFocusManager.getInstance(project).getFocusedDescendantFor(component) != null;
  }

  public static void requestFocus(@Nullable Project project, @Nullable Component component) {
    if (component == null) return;
    IdeFocusManager.getInstance(project).requestFocus(component, true);
  }

  //
  // Compare
  //

  @Nonnull
  public static List<LineFragment> compare(@Nonnull DiffRequest request,
                                           @Nonnull CharSequence text1,
                                           @Nonnull CharSequence text2,
                                           @Nonnull DiffConfig config,
                                           @Nonnull ProgressIndicator indicator) {
    indicator.checkCanceled();

    DiffUserDataKeysEx.DiffComputer diffComputer = request.getUserData(DiffUserDataKeysEx.CUSTOM_DIFF_COMPUTER);

    List<LineFragment> fragments;
    if (diffComputer != null) {
      fragments = diffComputer.compute(text1, text2, config.policy, config.innerFragments, indicator);
    }
    else {
      if (config.innerFragments) {
        fragments = ComparisonManager.getInstance().compareLinesInner(text1, text2, config.policy, indicator);
      }
      else {
        fragments = ComparisonManager.getInstance().compareLines(text1, text2, config.policy, indicator);
      }
    }

    indicator.checkCanceled();
    return ComparisonManager.getInstance().processBlocks(fragments, text1, text2,
                                                         config.policy, config.squashFragments, config.trimFragments);
  }

  @Nullable
  public static MergeInnerDifferences compareThreesideInner(@Nonnull List<CharSequence> chunks,
                                                            @Nonnull ComparisonPolicy comparisonPolicy,
                                                            @Nonnull ProgressIndicator indicator) {
    if (chunks.get(0) == null && chunks.get(1) == null && chunks.get(2) == null) return null; // ---

    if (comparisonPolicy == ComparisonPolicy.IGNORE_WHITESPACES) {
      if (isChunksEquals(chunks.get(0), chunks.get(1), comparisonPolicy) &&
          isChunksEquals(chunks.get(0), chunks.get(2), comparisonPolicy)) {
        // whitespace-only changes, ex: empty lines added/removed
        return new MergeInnerDifferences(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
      }
    }

    if (chunks.get(0) == null && chunks.get(1) == null ||
        chunks.get(0) == null && chunks.get(2) == null ||
        chunks.get(1) == null && chunks.get(2) == null) { // =--, -=-, --=
      return null;
    }

    if (chunks.get(0) != null && chunks.get(1) != null && chunks.get(2) != null) { // ===
      List<DiffFragment> fragments1 = ByWord.compare(chunks.get(1), chunks.get(0), comparisonPolicy, indicator);
      List<DiffFragment> fragments2 = ByWord.compare(chunks.get(1), chunks.get(2), comparisonPolicy, indicator);

      List<TextRange> left = new ArrayList<>();
      List<TextRange> base = new ArrayList<>();
      List<TextRange> right = new ArrayList<>();

      for (DiffFragment wordFragment : fragments1) {
        base.add(new TextRange(wordFragment.getStartOffset1(), wordFragment.getEndOffset1()));
        left.add(new TextRange(wordFragment.getStartOffset2(), wordFragment.getEndOffset2()));
      }

      for (DiffFragment wordFragment : fragments2) {
        base.add(new TextRange(wordFragment.getStartOffset1(), wordFragment.getEndOffset1()));
        right.add(new TextRange(wordFragment.getStartOffset2(), wordFragment.getEndOffset2()));
      }

      return new MergeInnerDifferences(left, base, right);
    }

    // ==-, =-=, -==
    final ThreeSide side1 = chunks.get(0) != null ? ThreeSide.LEFT : ThreeSide.BASE;
    final ThreeSide side2 = chunks.get(2) != null ? ThreeSide.RIGHT : ThreeSide.BASE;
    CharSequence chunk1 = side1.select(chunks);
    CharSequence chunk2 = side2.select(chunks);

    List<DiffFragment> wordConflicts = ByWord.compare(chunk1, chunk2, comparisonPolicy, indicator);

    List<List<TextRange>> textRanges = ThreeSide.map(side -> {
      if (side == side1) {
        return ContainerUtil.map(wordConflicts, fragment -> new TextRange(fragment.getStartOffset1(), fragment.getEndOffset1()));
      }
      if (side == side2) {
        return ContainerUtil.map(wordConflicts, fragment -> new TextRange(fragment.getStartOffset2(), fragment.getEndOffset2()));
      }
      return null;
    });

    return new MergeInnerDifferences(textRanges.get(0), textRanges.get(1), textRanges.get(2));
  }

  private static boolean isChunksEquals(@Nullable CharSequence chunk1,
                                        @Nullable CharSequence chunk2,
                                        @Nonnull ComparisonPolicy comparisonPolicy) {
    if (chunk1 == null) chunk1 = "";
    if (chunk2 == null) chunk2 = "";
    return ComparisonManager.getInstance().isEquals(chunk1, chunk2, comparisonPolicy);
  }

  @Nonnull
  public static <T> int[] getSortedIndexes(@Nonnull List<T> values, @Nonnull Comparator<T> comparator) {
    final List<Integer> indexes = new ArrayList<>(values.size());
    for (int i = 0; i < values.size(); i++) {
      indexes.add(i);
    }

    ContainerUtil.sort(indexes, (i1, i2) -> {
      T val1 = values.get(indexes.get(i1));
      T val2 = values.get(indexes.get(i2));
      return comparator.compare(val1, val2);
    });

    return ArrayUtil.toIntArray(indexes);
  }

  @Nonnull
  public static int[] invertIndexes(@Nonnull int[] indexes) {
    int[] inverted = new int[indexes.length];
    for (int i = 0; i < indexes.length; i++) {
      inverted[indexes[i]] = i;
    }
    return inverted;
  }

  //
  // Document modification
  //

  @Nonnull
  public static BitSet getSelectedLines(@Nonnull Editor editor) {
    Document document = editor.getDocument();
    int totalLines = getLineCount(document);
    BitSet lines = new BitSet(totalLines + 1);

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (caret.hasSelection()) {
        int line1 = editor.offsetToLogicalPosition(caret.getSelectionStart()).line;
        int line2 = editor.offsetToLogicalPosition(caret.getSelectionEnd()).line;
        lines.set(line1, line2 + 1);
        if (caret.getSelectionEnd() == document.getTextLength()) lines.set(totalLines);
      }
      else {
        lines.set(caret.getLogicalPosition().line);
        if (caret.getOffset() == document.getTextLength()) lines.set(totalLines);
      }
    }

    return lines;
  }

  public static boolean isSelectedByLine(int line, int line1, int line2) {
    if (line1 == line2 && line == line1) {
      return true;
    }
    if (line >= line1 && line < line2) {
      return true;
    }
    return false;
  }

  public static boolean isSelectedByLine(@Nonnull BitSet selected, int line1, int line2) {
    if (line1 == line2) {
      return selected.get(line1);
    }
    else {
      int next = selected.nextSetBit(line1);
      return next != -1 && next < line2;
    }
  }

  private static void deleteLines(@Nonnull Document document, int line1, int line2) {
    TextRange range = getLinesRange(document, line1, line2);
    int offset1 = range.getStartOffset();
    int offset2 = range.getEndOffset();

    if (offset1 > 0) {
      offset1--;
    }
    else if (offset2 < document.getTextLength()) {
      offset2++;
    }
    document.deleteString(offset1, offset2);
  }

  private static void insertLines(@Nonnull Document document, int line, @Nonnull CharSequence text) {
    if (line == getLineCount(document)) {
      document.insertString(document.getTextLength(), "\n" + text);
    }
    else {
      document.insertString(document.getLineStartOffset(line), text + "\n");
    }
  }

  private static void replaceLines(@Nonnull Document document, int line1, int line2, @Nonnull CharSequence text) {
    TextRange currentTextRange = getLinesRange(document, line1, line2);
    int offset1 = currentTextRange.getStartOffset();
    int offset2 = currentTextRange.getEndOffset();

    document.replaceString(offset1, offset2, text);
  }

  public static void applyModification(@Nonnull Document document,
                                       int line1,
                                       int line2,
                                       @Nonnull List<? extends CharSequence> newLines) {
    if (line1 == line2 && newLines.isEmpty()) return;
    if (line1 == line2) {
      insertLines(document, line1, StringUtil.join(newLines, "\n"));
    }
    else if (newLines.isEmpty()) {
      deleteLines(document, line1, line2);
    }
    else {
      replaceLines(document, line1, line2, StringUtil.join(newLines, "\n"));
    }
  }

  public static void applyModification(@Nonnull Document document1,
                                       int line1,
                                       int line2,
                                       @Nonnull Document document2,
                                       int oLine1,
                                       int oLine2) {
    if (line1 == line2 && oLine1 == oLine2) return;
    if (line1 == line2) {
      insertLines(document1, line1, getLinesContent(document2, oLine1, oLine2));
    }
    else if (oLine1 == oLine2) {
      deleteLines(document1, line1, line2);
    }
    else {
      replaceLines(document1, line1, line2, getLinesContent(document2, oLine1, oLine2));
    }
  }

  @Nonnull
  public static CharSequence getLinesContent(@Nonnull Document document, int line1, int line2) {
    TextRange otherRange = getLinesRange(document, line1, line2);
    return document.getImmutableCharSequence().subSequence(otherRange.getStartOffset(), otherRange.getEndOffset());
  }

  /**
   * Return affected range, without non-internal newlines
   * <p/>
   * we consider '\n' not as a part of line, but a separator between lines
   * ex: if last line is not empty, the last symbol will not be '\n'
   */
  public static TextRange getLinesRange(@Nonnull Document document, int line1, int line2) {
    return getLinesRange(document, line1, line2, false);
  }

  @Nonnull
  public static TextRange getLinesRange(@Nonnull Document document, int line1, int line2, boolean includeNewline) {
    if (line1 == line2) {
      int lineStartOffset = line1 < getLineCount(document) ? document.getLineStartOffset(line1) : document.getTextLength();
      return new TextRange(lineStartOffset, lineStartOffset);
    }
    else {
      int startOffset = document.getLineStartOffset(line1);
      int endOffset = document.getLineEndOffset(line2 - 1);
      if (includeNewline && endOffset < document.getTextLength()) endOffset++;
      return new TextRange(startOffset, endOffset);
    }
  }

  public static int getOffset(@Nonnull Document document, int line, int column) {
    if (line < 0) return 0;
    if (line >= getLineCount(document)) return document.getTextLength();

    int start = document.getLineStartOffset(line);
    int end = document.getLineEndOffset(line);
    return Math.min(start + column, end);
  }

  public static int getLineCount(@Nonnull Document document) {
    return Math.max(document.getLineCount(), 1);
  }

  @Nonnull
  public static List<String> getLines(@Nonnull Document document) {
    return getLines(document, 0, getLineCount(document));
  }

  @Nonnull
  public static List<String> getLines(@Nonnull Document document, int startLine, int endLine) {
    if (startLine < 0 || startLine > endLine || endLine > getLineCount(document)) {
      throw new IndexOutOfBoundsException(String.format("Wrong line range: [%d, %d); lineCount: '%d'",
                                                        startLine, endLine, document.getLineCount()));
    }

    List<String> result = new ArrayList<>();
    for (int i = startLine; i < endLine; i++) {
      int start = document.getLineStartOffset(i);
      int end = document.getLineEndOffset(i);
      result.add(document.getText(new TextRange(start, end)));
    }
    return result;
  }

  //
  // Updating ranges on change
  //

  @Nonnull
  public static LineRange getAffectedLineRange(@Nonnull DocumentEvent e) {
    int line1 = e.getDocument().getLineNumber(e.getOffset());
    int line2 = e.getDocument().getLineNumber(e.getOffset() + e.getOldLength()) + 1;
    return new LineRange(line1, line2);
  }

  public static int countLinesShift(@Nonnull DocumentEvent e) {
    return StringUtil.countNewLines(e.getNewFragment()) - StringUtil.countNewLines(e.getOldFragment());
  }

  @Nonnull
  public static UpdatedLineRange updateRangeOnModification(int start, int end, int changeStart, int changeEnd, int shift) {
    return updateRangeOnModification(start, end, changeStart, changeEnd, shift, false);
  }

  @Nonnull
  public static UpdatedLineRange updateRangeOnModification(int start, int end, int changeStart, int changeEnd, int shift, boolean greedy) {
    if (end <= changeStart) { // change before
      return new UpdatedLineRange(start, end, false);
    }
    if (start >= changeEnd) { // change after
      return new UpdatedLineRange(start + shift, end + shift, false);
    }

    if (start <= changeStart && end >= changeEnd) { // change inside
      return new UpdatedLineRange(start, end + shift, false);
    }

    // range is damaged. We don't know new boundaries.
    // But we can try to return approximate new position
    int newChangeEnd = changeEnd + shift;

    if (start >= changeStart && end <= changeEnd) { // fully inside change
      return greedy ? new UpdatedLineRange(changeStart, newChangeEnd, true) :
             new UpdatedLineRange(newChangeEnd, newChangeEnd, true);
    }

    if (start < changeStart) { // bottom boundary damaged
      return greedy ? new UpdatedLineRange(start, newChangeEnd, true) :
             new UpdatedLineRange(start, changeStart, true);
    } else { // top boundary damaged
      return greedy ? new UpdatedLineRange(changeStart, end + shift, true) :
             new UpdatedLineRange(newChangeEnd, end + shift, true);
    }
  }

  public static class UpdatedLineRange {
    public final int startLine;
    public final int endLine;
    public final boolean damaged;

    public UpdatedLineRange(int startLine, int endLine, boolean damaged) {
      this.startLine = startLine;
      this.endLine = endLine;
      this.damaged = damaged;
    }
  }

  //
  // Types
  //

  @Nonnull
  public static TextDiffType getLineDiffType(@Nonnull LineFragment fragment) {
    boolean left = fragment.getStartLine1() != fragment.getEndLine1();
    boolean right = fragment.getStartLine2() != fragment.getEndLine2();
    return getDiffType(left, right);
  }

  @Nonnull
  public static TextDiffType getDiffType(@Nonnull DiffFragment fragment) {
    boolean left = fragment.getEndOffset1() != fragment.getStartOffset1();
    boolean right = fragment.getEndOffset2() != fragment.getStartOffset2();
    return getDiffType(left, right);
  }

  @Nonnull
  public static TextDiffType getDiffType(boolean hasDeleted, boolean hasInserted) {
    if (hasDeleted && hasInserted) {
      return TextDiffType.MODIFIED;
    }
    else if (hasDeleted) {
      return TextDiffType.DELETED;
    }
    else if (hasInserted) {
      return TextDiffType.INSERTED;
    }
    else {
      LOG.error("Diff fragment should not be empty");
      return TextDiffType.MODIFIED;
    }
  }

  @Nonnull
  public static MergeConflictType getMergeType(@Nonnull Condition<ThreeSide> emptiness,
                                               @Nonnull BiPredicate<ThreeSide, ThreeSide> equality) {
    boolean isLeftEmpty = emptiness.value(ThreeSide.LEFT);
    boolean isBaseEmpty = emptiness.value(ThreeSide.BASE);
    boolean isRightEmpty = emptiness.value(ThreeSide.RIGHT);
    assert !isLeftEmpty || !isBaseEmpty || !isRightEmpty;

    if (isBaseEmpty) {
      if (isLeftEmpty) { // --=
        return new MergeConflictType(TextDiffType.INSERTED, false, true);
      }
      else if (isRightEmpty) { // =--
        return new MergeConflictType(TextDiffType.INSERTED, true, false);
      }
      else { // =-=
        boolean equalModifications = equality.test(ThreeSide.LEFT, ThreeSide.RIGHT);
        return new MergeConflictType(equalModifications ? TextDiffType.INSERTED : TextDiffType.CONFLICT);
      }
    }
    else {
      if (isLeftEmpty && isRightEmpty) { // -=-
        return new MergeConflictType(TextDiffType.DELETED);
      }
      else { // -==, ==-, ===
        boolean unchangedLeft = equality.test(ThreeSide.BASE, ThreeSide.LEFT);
        boolean unchangedRight = equality.test(ThreeSide.BASE, ThreeSide.RIGHT);
        assert !unchangedLeft || !unchangedRight;

        if (unchangedLeft) return new MergeConflictType(isRightEmpty ? TextDiffType.DELETED : TextDiffType.MODIFIED, false, true);
        if (unchangedRight) return new MergeConflictType(isLeftEmpty ? TextDiffType.DELETED : TextDiffType.MODIFIED, true, false);

        boolean equalModifications = equality.test(ThreeSide.LEFT, ThreeSide.RIGHT);
        return new MergeConflictType(equalModifications ? TextDiffType.MODIFIED : TextDiffType.CONFLICT);
      }
    }
  }

  @Nonnull
  public static MergeConflictType getLineMergeType(@Nonnull MergeLineFragment fragment,
                                                   @Nonnull List<? extends Document> documents,
                                                   @Nonnull ComparisonPolicy policy) {
    return getMergeType((side) -> isLineMergeIntervalEmpty(fragment, side),
                        (side1, side2) -> compareLineMergeContents(fragment, documents, policy, side1, side2));
  }

  private static boolean compareLineMergeContents(@Nonnull MergeLineFragment fragment,
                                                  @Nonnull List<? extends Document> documents,
                                                  @Nonnull ComparisonPolicy policy,
                                                  @Nonnull ThreeSide side1,
                                                  @Nonnull ThreeSide side2) {
    int start1 = fragment.getStartLine(side1);
    int end1 = fragment.getEndLine(side1);
    int start2 = fragment.getStartLine(side2);
    int end2 = fragment.getEndLine(side2);

    if (end2 - start2 != end1 - start1) return false;

    Document document1 = side1.select(documents);
    Document document2 = side2.select(documents);

    for (int i = 0; i < end1 - start1; i++) {
      int line1 = start1 + i;
      int line2 = start2 + i;

      CharSequence content1 = getLinesContent(document1, line1, line1 + 1);
      CharSequence content2 = getLinesContent(document2, line2, line2 + 1);
      if (!ComparisonManager.getInstance().isEquals(content1, content2, policy)) return false;
    }

    return true;
  }

  private static boolean isLineMergeIntervalEmpty(@Nonnull MergeLineFragment fragment, @Nonnull ThreeSide side) {
    return fragment.getStartLine(side) == fragment.getEndLine(side);
  }

  //
  // Writable
  //

  @RequiredUIAccess
  public static void executeWriteCommand(@Nullable Project project,
                                         @Nonnull Document document,
                                         @Nullable String commandName,
                                         @Nullable String commandGroupId,
                                         @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                         boolean underBulkUpdate,
                                         @Nonnull Runnable task) {
    if (!makeWritable(project, document)) {
      VirtualFile file = FileDocumentManager.getInstance().getFile(document);
      LOG.warn("Document is read-only" + (file != null ? ": " + file.getPresentableName() : ""));
      return;
    }

    ApplicationManager.getApplication().runWriteAction(() -> {
      CommandProcessor.getInstance().executeCommand(project, () -> {
        if (underBulkUpdate) {
          DocumentUtil.executeInBulk(document, true, task);
        }
        else {
          task.run();
        }
      }, commandName, commandGroupId, confirmationPolicy, document);
    });
  }

  @RequiredUIAccess
  public static void executeWriteCommand(@Nonnull final Document document,
                                         @Nullable final Project project,
                                         @Nullable final String commandName,
                                         @Nonnull final Runnable task) {
    executeWriteCommand(project, document, commandName, null, UndoConfirmationPolicy.DEFAULT, false, task);
  }

  public static boolean isEditable(@Nonnull Editor editor) {
    return !editor.isViewer() && canMakeWritable(editor.getDocument());
  }

  public static boolean canMakeWritable(@Nonnull Document document) {
    if (document.isWritable()) {
      return true;
    }
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null && file.isValid() && file.isInLocalFileSystem()) {
      // decompiled file can be writable, but Document with decompiled content is still read-only
      return !file.isWritable();
    }
    return false;
  }

  @RequiredUIAccess
  public static boolean makeWritable(@Nullable Project project, @Nonnull Document document) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null || !file.isValid()) return document.isWritable();
    return makeWritable(project, file) && document.isWritable();
  }

  @RequiredUIAccess
  public static boolean makeWritable(@Nullable Project project, @Nonnull VirtualFile file) {
    if (project == null) project = ProjectManager.getInstance().getDefaultProject();
    return !ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(file).hasReadonlyFiles();
  }

  public static void putNonundoableOperation(@Nullable Project project, @Nonnull Document document) {
    UndoManager undoManager = project != null ? UndoManager.getInstance(project) : UndoManager.getGlobalInstance();
    if (undoManager != null) {
      DocumentReference ref = DocumentReferenceManager.getInstance().create(document);
      undoManager.nonundoableActionPerformed(ref, false);
    }
  }

  /**
   * Difference with {@link VfsUtil#markDirtyAndRefresh} is that refresh from VfsUtil will be performed with ModalityState.NON_MODAL.
   */
  public static void markDirtyAndRefresh(boolean async, boolean recursive, boolean reloadChildren, @Nonnull VirtualFile... files) {
    ModalityState modalityState = ApplicationManager.getApplication().getDefaultModalityState();
    VfsUtil.markDirty(recursive, reloadChildren, files);
    RefreshQueue.getInstance().refresh(async, recursive, null, modalityState, files);
  }

  //
  // Windows
  //

  @Nonnull
  public static Dimension getDefaultDiffPanelSize() {
    return new Dimension(400, 200);
  }

  @Nonnull
  public static Dimension getDefaultDiffWindowSize() {
    Rectangle screenBounds = ScreenUtil.getMainScreenBounds();
    int width = (int)(screenBounds.width * 0.8);
    int height = (int)(screenBounds.height * 0.8);
    return new Dimension(width, height);
  }

  @Nonnull
  public static WindowWrapper.Mode getWindowMode(@Nonnull DiffDialogHints hints) {
    WindowWrapper.Mode mode = hints.getMode();
    if (mode == null) {
      boolean isUnderDialog = LaterInvocator.isInModalContext();
      mode = isUnderDialog ? WindowWrapper.Mode.MODAL : WindowWrapper.Mode.FRAME;
    }
    return mode;
  }

  public static void closeWindow(@Nullable Window window, boolean modalOnly, boolean recursive) {
    if (window == null) return;

    Component component = window;
    while (component != null) {
      if (component instanceof Window) closeWindow((Window)component, modalOnly);

      component = recursive ? component.getParent() : null;
    }
  }

  public static void closeWindow(@Nonnull Window window, boolean modalOnly) {
    consulo.ui.Window uiWindow = TargetAWT.from(window);
    if(uiWindow != null) {
      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if(ideFrame != null) {
        return;
      }
    }

    if (modalOnly && window instanceof Frame) return;

    if (window instanceof DialogWrapperDialog) {
      ((DialogWrapperDialog)window).getDialogWrapper().doCancelAction();
      return;
    }

    window.setVisible(false);
    window.dispose();
  }

  //
  // UserData
  //

  public static <T> UserDataHolderBase createUserDataHolder(@Nonnull Key<T> key, @Nullable T value) {
    UserDataHolderBase holder = new UserDataHolderBase();
    holder.putUserData(key, value);
    return holder;
  }

  public static boolean isUserDataFlagSet(@Nonnull Key<Boolean> key, UserDataHolder... holders) {
    for (UserDataHolder holder : holders) {
      if (holder == null) continue;
      Boolean data = holder.getUserData(key);
      if (data != null) return data;
    }
    return false;
  }

  public static <T> T getUserData(@Nullable UserDataHolder first, @Nullable UserDataHolder second, @Nonnull Key<T> key) {
    if (first != null) {
      T data = first.getUserData(key);
      if (data != null) return data;
    }
    if (second != null) {
      T data = second.getUserData(key);
      if (data != null) return data;
    }
    return null;
  }

  public static void addNotification(@Nullable JComponent component, @Nonnull UserDataHolder holder) {
    if (component == null) return;
    List<JComponent> oldComponents = ContainerUtil.notNullize(holder.getUserData(DiffUserDataKeys.NOTIFICATIONS));
    holder.putUserData(DiffUserDataKeys.NOTIFICATIONS, ContainerUtil.append(oldComponents, component));
  }

  @Nonnull
  public static List<JComponent> getCustomNotifications(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    List<JComponent> requestComponents = request.getUserData(DiffUserDataKeys.NOTIFICATIONS);
    List<JComponent> contextComponents = context.getUserData(DiffUserDataKeys.NOTIFICATIONS);
    return ContainerUtil.concat(ContainerUtil.notNullize(contextComponents), ContainerUtil.notNullize(requestComponents));
  }

  @Nonnull
  public static List<JComponent> getCustomNotifications(@Nonnull DiffContent content) {
    return ContainerUtil.notNullize(content.getUserData(DiffUserDataKeys.NOTIFICATIONS));
  }

  //
  // DataProvider
  //

  @Nullable
  public static VirtualFile getVirtualFile(@Nonnull ContentDiffRequest request, @Nonnull Side currentSide) {
    List<DiffContent> contents = request.getContents();
    DiffContent content1 = currentSide.select(contents);
    DiffContent content2 = currentSide.other().select(contents);

    if (content1 instanceof FileContent) return ((FileContent)content1).getFile();
    if (content2 instanceof FileContent) return ((FileContent)content2).getFile();
    return null;
  }

  @Nullable
  public static VirtualFile getVirtualFile(@Nonnull ContentDiffRequest request, @Nonnull ThreeSide currentSide) {
    List<DiffContent> contents = request.getContents();
    DiffContent content1 = currentSide.select(contents);
    DiffContent content2 = ThreeSide.BASE.select(contents);

    if (content1 instanceof FileContent) return ((FileContent)content1).getFile();
    if (content2 instanceof FileContent) return ((FileContent)content2).getFile();
    return null;
  }

  @Nullable
  public static Object getData(@Nullable DataProvider provider, @Nullable DataProvider fallbackProvider, @NonNls Key<?> dataId) {
    if (provider != null) {
      Object data = provider.getData(dataId);
      if (data != null) return data;
    }
    if (fallbackProvider != null) {
      Object data = fallbackProvider.getData(dataId);
      if (data != null) return data;
    }
    return null;
  }

  public static <T> void putDataKey(@Nonnull UserDataHolder holder, @Nonnull Key<T> key, @Nullable T value) {
    DataProvider dataProvider = holder.getUserData(DiffUserDataKeys.DATA_PROVIDER);
    if (!(dataProvider instanceof GenericDataProvider)) {
      dataProvider = new GenericDataProvider(dataProvider);
      holder.putUserData(DiffUserDataKeys.DATA_PROVIDER, dataProvider);
    }
    ((GenericDataProvider)dataProvider).putData(key, value);
  }

  @Nonnull
  public static DiffSettings getDiffSettings(@Nonnull DiffContext context) {
    DiffSettings settings = context.getUserData(DiffSettingsHolder.KEY);
    if (settings == null) {
      settings = DiffSettings.getSettings(context.getUserData(DiffUserDataKeys.PLACE));
      context.putUserData(DiffSettingsHolder.KEY, settings);
    }
    return settings;
  }

  //
  // Tools
  //

  @Nonnull
  public static <T extends DiffTool> List<T> filterSuppressedTools(@Nonnull List<T> tools) {
    if (tools.size() < 2) return tools;

    final List<Class<? extends DiffTool>> suppressedTools = new ArrayList<>();
    for (T tool : tools) {
      try {
        if (tool instanceof SuppressiveDiffTool) suppressedTools.addAll(((SuppressiveDiffTool)tool).getSuppressedTools());
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    if (suppressedTools.isEmpty()) return tools;

    List<T> filteredTools = ContainerUtil.filter(tools, tool -> !suppressedTools.contains(tool.getClass()));
    return filteredTools.isEmpty() ? tools : filteredTools;
  }

  //
  // Helpers
  //

  public static class DiffConfig {
    @Nonnull
    public final ComparisonPolicy policy;
    public final boolean innerFragments;
    public final boolean squashFragments;
    public final boolean trimFragments;

    public DiffConfig(@Nonnull ComparisonPolicy policy, boolean innerFragments, boolean squashFragments, boolean trimFragments) {
      this.policy = policy;
      this.innerFragments = innerFragments;
      this.squashFragments = squashFragments;
      this.trimFragments = trimFragments;
    }

    public DiffConfig(@Nonnull IgnorePolicy ignorePolicy, @Nonnull HighlightPolicy highlightPolicy) {
      this(ignorePolicy.getComparisonPolicy(), highlightPolicy.isFineFragments(), highlightPolicy.isShouldSquash(),
           ignorePolicy.isShouldTrimChunks());
    }
  }

  private static class SyncHeightComponent extends JPanel {
    @Nonnull
    private final List<JComponent> myComponents;

    public SyncHeightComponent(@Nonnull List<JComponent> components, int index) {
      super(new BorderLayout());
      myComponents = components;
      JComponent delegate = components.get(index);
      if (delegate != null) add(delegate, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      size.height = getPreferredHeight();
      return size;
    }

    private int getPreferredHeight() {
      int height = 0;
      for (JComponent component : myComponents) {
        if (component == null) continue;
        height = Math.max(height, component.getPreferredSize().height);
      }
      return height;
    }
  }

  public static class CenteredPanel extends JPanel {
    private final JComponent myComponent;

    public CenteredPanel(@Nonnull JComponent component) {
      myComponent = component;
      add(component);
    }

    public CenteredPanel(@Nonnull JComponent component, @Nonnull Border border) {
      this(component);
      setBorder(border);
    }

    @Override
    public void doLayout() {
      final Dimension size = getSize();
      final Dimension preferredSize = myComponent.getPreferredSize();

      Insets insets = getInsets();
      JBInsets.removeFrom(size, insets);

      int width = Math.min(size.width, preferredSize.width);
      int height = Math.min(size.height, preferredSize.height);
      int x = Math.max(0, (size.width - preferredSize.width) / 2);
      int y = Math.max(0, (size.height - preferredSize.height) / 2);

      myComponent.setBounds(insets.left + x, insets.top + y, width, height);
    }

    @Override
    public Dimension getPreferredSize() {
      return addInsets(myComponent.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
      return addInsets(myComponent.getMinimumSize());
    }

    @Override
    public Dimension getMaximumSize() {
      return addInsets(myComponent.getMaximumSize());
    }

    private Dimension addInsets(Dimension dimension) {
      JBInsets.addTo(dimension, getInsets());
      return dimension;
    }
  }
}
