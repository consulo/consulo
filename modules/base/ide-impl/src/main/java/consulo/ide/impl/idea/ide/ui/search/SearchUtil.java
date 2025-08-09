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

package consulo.ide.impl.idea.ide.ui.search;

import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.idea.application.options.SkipSelfSearchComponent;
import consulo.ide.impl.idea.openapi.options.ex.GlassPanel;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author anna
 * @since 2006-02-07
 */
public class SearchUtil {
  private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");
  private static final Pattern QUOTED = Pattern.compile("\\\"([^\\\"]+)\\\"");

  public static final String HIGHLIGHT_WITH_BORDER = "searchUtil.highlightWithBorder";
  public static final String STYLE_END = "</style>";

  private SearchUtil() {
  }

  public static void processProjectConfigurables(Project project, HashMap<SearchableConfigurable, TreeSet<OptionDescription>> options) {
    Configurable[] configurables = BaseShowSettingsUtil.buildConfigurables(project);
    processConfigurables(configurables, options);
  }

  private static void processConfigurables(Configurable[] configurables,
                                           HashMap<SearchableConfigurable, TreeSet<OptionDescription>> options) {
    for (Configurable configurable : configurables) {
      if (configurable instanceof SearchableConfigurable) {
        TreeSet<OptionDescription> configurableOptions = new TreeSet<OptionDescription>();

        if (configurable instanceof Configurable.Composite) {
          Configurable[] children = ((Configurable.Composite)configurable).getConfigurables();
          processConfigurables(children, options);
        }

        //ignore invisible root nodes
        if (configurable instanceof SearchableConfigurable.Parent && !((SearchableConfigurable.Parent)configurable).isVisible()) {
          continue;
        }

        options.put((SearchableConfigurable)configurable, configurableOptions);

        if (configurable instanceof MasterDetails) {
          MasterDetails md = (MasterDetails)configurable;
          md.initUi();
          _processComponent(configurable, configurableOptions, md.getMaster());
          _processComponent(configurable, configurableOptions, md.getDetails().getComponent());
        }
        else {
          _processComponent(configurable, configurableOptions, configurable.createComponent());
        }
      }
    }
  }

  private static void _processComponent(Configurable configurable, TreeSet<OptionDescription> configurableOptions,
                                        JComponent component) {

    if (component == null) return;

    processUILabel(configurable.getDisplayName().get(), configurableOptions, null);
    processComponent(component, configurableOptions, null);
  }

  public static void processComponent(JComponent component, Set<OptionDescription> configurableOptions, @NonNls String path) {
    if (component instanceof SkipSelfSearchComponent) return;
    if (Objects.equals(UIUtil.getClientProperty(component, SkipSelfSearchComponent.KEY), Boolean.TRUE)) return;

    Border border = component.getBorder();
    if (border instanceof TitledBorder) {
      TitledBorder titledBorder = (TitledBorder)border;
      String title = titledBorder.getTitle();
      if (title != null) {
        processUILabel(title, configurableOptions, path);
      }
    }
    if (component instanceof JLabel) {
      String label = ((JLabel)component).getText();
      if (label != null) {
        processUILabel(label, configurableOptions, path);
      }
    }
    else if (component instanceof JCheckBox) {
      @NonNls String checkBoxTitle = ((JCheckBox)component).getText();
      if (checkBoxTitle != null) {
        processUILabel(checkBoxTitle, configurableOptions, path);
      }
    }
    else if (component instanceof JRadioButton) {
      @NonNls String radioButtonTitle = ((JRadioButton)component).getText();
      if (radioButtonTitle != null) {
        processUILabel(radioButtonTitle, configurableOptions, path);
      }
    }
    else if (component instanceof JButton) {
      @NonNls String buttonTitle = ((JButton)component).getText();
      if (buttonTitle != null) {
        processUILabel(buttonTitle, configurableOptions, path);
      }
    }
    if (component instanceof JTabbedPane) {
      JTabbedPane tabbedPane = (JTabbedPane)component;
      int tabCount = tabbedPane.getTabCount();
      for (int i = 0; i < tabCount; i++) {
        String title = path != null ? path + '.' + tabbedPane.getTitleAt(i) : tabbedPane.getTitleAt(i);
        processUILabel(title, configurableOptions, title);
        Component tabComponent = tabbedPane.getComponentAt(i);
        if (tabComponent instanceof JComponent) {
          processComponent((JComponent)tabComponent, configurableOptions, title);
        }
      }
    }
    else {
      Component[] components = component.getComponents();
      if (components != null) {
        for (Component child : components) {
          if (child instanceof JComponent) {
            processComponent((JComponent)child, configurableOptions, path);
          }
        }
      }
    }
  }

  private static void processUILabel(@NonNls String title, Set<OptionDescription> configurableOptions, String path) {
    Set<String> words = SearchableOptionsRegistrar.getInstance().getProcessedWordsWithoutStemming(title);
    @NonNls String regex = "[\\W&&[^\\p{Punct}\\p{Blank}]]";
    for (String option : words) {
      configurableOptions.add(new OptionDescription(option, HTML_PATTERN.matcher(title).replaceAll(" ").replaceAll(regex, " "), path));
    }
  }

  public static Runnable lightOptions(final SearchableConfigurable configurable,
                                      final JComponent component,
                                      final String option,
                                      final GlassPanel glassPanel) {
    return new Runnable() {
      public void run() {
        if (!traverseComponentsTree(configurable, glassPanel, component, option, true)) {
          traverseComponentsTree(configurable, glassPanel, component, option, false);
        }
      }
    };
  }

  private static int getSelection(String tabIdx, JTabbedPane tabbedPane) {
    SearchableOptionsRegistrar searchableOptionsRegistrar = SearchableOptionsRegistrar.getInstance();
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      Set<String> pathWords = searchableOptionsRegistrar.getProcessedWords(tabIdx);
      String title = tabbedPane.getTitleAt(i);
      if (!pathWords.isEmpty()) {
        Set<String> titleWords = searchableOptionsRegistrar.getProcessedWords(title);
        pathWords.removeAll(titleWords);
        if (pathWords.isEmpty()) return i;
      } else if (tabIdx.equalsIgnoreCase(title)) { //e.g. only stop words
        return i;
      }
    }
    return -1;
  }

  public static int getSelection(String tabIdx, TabbedPaneWrapper tabbedPane) {
    SearchableOptionsRegistrar searchableOptionsRegistrar = SearchableOptionsRegistrar.getInstance();
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      Set<String> pathWords = searchableOptionsRegistrar.getProcessedWords(tabIdx);
      String title = tabbedPane.getTitleAt(i);
      Set<String> titleWords = searchableOptionsRegistrar.getProcessedWords(title);
      pathWords.removeAll(titleWords);
      if (pathWords.isEmpty()) return i;
    }
    return -1;
  }

  private static boolean traverseComponentsTree(SearchableConfigurable configurable,
                                                GlassPanel glassPanel,
                                                JComponent rootComponent,
                                                String option,
                                                boolean force) {

    rootComponent.putClientProperty(HIGHLIGHT_WITH_BORDER, null);

    if (option == null || option.trim().length() == 0) return false;
    boolean highlight = false;
    if (rootComponent instanceof JCheckBox) {
      JCheckBox checkBox = ((JCheckBox)rootComponent);
      if (isComponentHighlighted(checkBox.getText(), option, force, configurable)) {
        highlight = true;
        glassPanel.addSpotlight(checkBox);
      }
    }
    else if (rootComponent instanceof JRadioButton) {
      JRadioButton radioButton = ((JRadioButton)rootComponent);
      if (isComponentHighlighted(radioButton.getText(), option, force, configurable)) {
        highlight = true;
        glassPanel.addSpotlight(radioButton);
      }
    }
    else if (rootComponent instanceof JLabel) {
      JLabel label = ((JLabel)rootComponent);
      if (isComponentHighlighted(label.getText(), option, force, configurable)) {
        highlight = true;
        glassPanel.addSpotlight(label);
      }
    }
    else if (rootComponent instanceof JButton) {
      JButton button = ((JButton)rootComponent);
      if (isComponentHighlighted(button.getText(), option, force, configurable)) {
        highlight = true;
        glassPanel.addSpotlight(button);
      }
    }
    else if (rootComponent instanceof JTabbedPane) {
      JTabbedPane tabbedPane = (JTabbedPane)rootComponent;
      String path = SearchableOptionsRegistrar.getInstance().getInnerPath(configurable, option);
      if (path != null) {
        int index = getSelection(path, tabbedPane);
        if (index > -1 && index < tabbedPane.getTabCount()) {
          if (tabbedPane.getTabComponentAt(index) instanceof JComponent) {
            glassPanel.addSpotlight((JComponent)tabbedPane.getTabComponentAt(index));
          }
        }
      }
    }


    Component[] components = rootComponent.getComponents();
    for (Component component : components) {
      if (component instanceof JComponent) {
        boolean innerHighlight = traverseComponentsTree(configurable, glassPanel, (JComponent)component, option, force);

        if (!highlight && !innerHighlight) {
          Border border = rootComponent.getBorder();
          if (border instanceof TitledBorder) {
            String title = ((TitledBorder)border).getTitle();
            if (isComponentHighlighted(title, option, force, configurable)) {
              highlight = true;
              glassPanel.addSpotlight(rootComponent);
              rootComponent.putClientProperty(HIGHLIGHT_WITH_BORDER, Boolean.TRUE);
            }
          }
        }


        if (innerHighlight) {
          highlight = true;
        }
      }
    }
    return highlight;
  }

  public static boolean isComponentHighlighted(String text, String option, boolean force, SearchableConfigurable configurable) {
    if (text == null || option == null || option.length() == 0) return false;
    SearchableOptionsRegistrar searchableOptionsRegistrar = SearchableOptionsRegistrar.getInstance();
    Set<String> words = searchableOptionsRegistrar.getProcessedWords(option);
    Set<String> options =
      configurable != null ? searchableOptionsRegistrar.replaceSynonyms(words, configurable) : words;
    if (options == null || options.isEmpty()) {
      return text.toLowerCase().indexOf(option.toLowerCase()) != -1;
    }
    Set<String> tokens = searchableOptionsRegistrar.getProcessedWords(text);
    if (!force) {
      options.retainAll(tokens);
      boolean highlight = !options.isEmpty();
      return highlight || text.toLowerCase().indexOf(option.toLowerCase()) != -1;
    }
    else {
      options.removeAll(tokens);
      return options.isEmpty();
    }
  }

  public static Runnable lightOptions(final SearchableConfigurable configurable,
                                      final JComponent component,
                                      final String option,
                                      final GlassPanel glassPanel,
                                      final boolean forceSelect) {
    return new Runnable() {
      public void run() {
        traverseComponentsTree(configurable, glassPanel, component, option, forceSelect);
      }
    };
  }

  public static String markup(@NonNls @Nonnull String textToMarkup, @Nullable String filter) {
    if (filter == null || filter.length() == 0) {
      return textToMarkup;
    }
    int bodyStart = textToMarkup.indexOf("<body>");
    int bodyEnd = textToMarkup.indexOf("</body>");
    String head;
    String foot;
    if (bodyStart >= 0) {
      bodyStart += "<body>".length();
      head = textToMarkup.substring(0, bodyStart);
      if (bodyEnd >= 0) {
        foot = textToMarkup.substring(bodyEnd);
      } else {
        foot = "";
      }
      textToMarkup = textToMarkup.substring(bodyStart, bodyEnd);
    } else {
      foot = "";
      head = "";
    }
    Pattern insideHtmlTagPattern = Pattern.compile("[<[^<>]*>]*<[^<>]*");
    SearchableOptionsRegistrar registrar = SearchableOptionsRegistrar.getInstance();
    HashSet<String> quoted = new HashSet<String>();
    filter = processFilter(quoteStrictOccurrences(textToMarkup, filter), quoted);
    Set<String> options = registrar.getProcessedWords(filter);
    Set<String> words = registrar.getProcessedWords(textToMarkup);
    for (String option : options) {
      if (words.contains(option)) {
        textToMarkup = markup(textToMarkup, insideHtmlTagPattern, option);
      }
    }
    for (String stripped : quoted) {
      textToMarkup = markup(textToMarkup, insideHtmlTagPattern, stripped);
    }
    return head + textToMarkup + foot;
  }

  private static String quoteStrictOccurrences(String textToMarkup, String filter) {
    String cur = "";
    String s = textToMarkup.toLowerCase();
    for (String part : filter.split(" ")) {
      if (s.contains(part)) {
        cur += "\"" + part + "\" ";
      }
      else {
        cur += part + " ";
      }
    }
    return cur;
  }

  private static String markup(@NonNls String textToMarkup, Pattern insideHtmlTagPattern, String option) {
    String result = "";
    int styleIdx = textToMarkup.indexOf("<style");
    int styleEndIdx = textToMarkup.indexOf("</style>");
    if (styleIdx < 0 || styleEndIdx < 0) {
      result = markupInText(textToMarkup, insideHtmlTagPattern, option);
    } else {
      result = markup(textToMarkup.substring(0, styleIdx), insideHtmlTagPattern, option) + markup(textToMarkup.substring(styleEndIdx + STYLE_END.length()), insideHtmlTagPattern, option);
    }
    return result;
  }

  private static String markupInText(String textToMarkup, Pattern insideHtmlTagPattern, String option) {
    String result = "";
    int beg = 0;
    int idx;
    while ((idx = StringUtil.indexOfIgnoreCase(textToMarkup, option, beg)) != -1) {
      String prefix = textToMarkup.substring(beg, idx);
      String toMark = textToMarkup.substring(idx, idx + option.length());
      if (insideHtmlTagPattern.matcher(prefix).matches()) {
        int lastIdx = textToMarkup.indexOf(">", idx);
        result += prefix + textToMarkup.substring(idx, lastIdx + 1);
        beg = lastIdx + 1;
      }
      else {
        result += prefix + "<font color='#ffffff' bgColor='#1d5da7'>" + toMark + "</font>";
        beg = idx + option.length();
      }
    }
    result += textToMarkup.substring(beg);
    return result;
  }

  public static void appendFragments(String filter,
                                     @NonNls String text,
                                     @SimpleTextAttributes.StyleAttributeConstant int style,
                                     Color foreground,
                                     Color background,
                                     SimpleColoredComponent textRenderer) {
    if (text == null) return;
    if (filter == null || filter.length() == 0) {
      textRenderer.append(text, new SimpleTextAttributes(background, foreground, JBColor.RED, style));
    }
    else { //markup
      HashSet<String> quoted = new HashSet<String>();
      filter = processFilter(quoteStrictOccurrences(text, filter), quoted);
      TreeMap<Integer, String> indx = new TreeMap<Integer, String>();
      for (String stripped : quoted) {
        int beg = 0;
        int idx;
        while ((idx = StringUtil.indexOfIgnoreCase(text, stripped, beg)) != -1) {
          indx.put(idx, text.substring(idx, idx + stripped.length()));
          beg = idx + stripped.length();
        }
      }

      List<String> selectedWords = new ArrayList<String>();
      int pos = 0;
      for (Integer index : indx.keySet()) {
        String stripped = indx.get(index);
        int start = index.intValue();
        if (pos > start) {
          String highlighted = selectedWords.get(selectedWords.size() - 1);
          if (highlighted.length() < stripped.length()){
            selectedWords.remove(highlighted);
          } else {
            continue;
          }
        }
        appendSelectedWords(text, selectedWords, pos, start, filter);
        selectedWords.add(stripped);
        pos = start + stripped.length();
      }
      appendSelectedWords(text, selectedWords, pos, text.length(), filter);

      int idx = 0;
      for (String word : selectedWords) {
        text = text.substring(idx);
        String before = text.substring(0, text.indexOf(word));
        if (before.length() > 0) textRenderer.append(before, new SimpleTextAttributes(background, foreground, null, style));
        idx = text.indexOf(word) + word.length();
        textRenderer.append(text.substring(idx - word.length(), idx), new SimpleTextAttributes(background,
                                                                                               foreground, null,
                                                                                               style |
                                                                                               SimpleTextAttributes.STYLE_SEARCH_MATCH));
      }
      String after = text.substring(idx, text.length());
      if (after.length() > 0) textRenderer.append(after, new SimpleTextAttributes(background, foreground, null, style));
    }
  }

  private static void appendSelectedWords(String text,
                                          List<String> selectedWords,
                                          int pos,
                                          int end,
                                          String filter) {
    if (pos < end) {
      Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
      String[] words = text.substring(pos, end).split("[\\W&&[^-]]+");
      for (String word : words) {
        if (filters.contains(PorterStemmerUtil.stem(word.toLowerCase()))) {
          selectedWords.add(word);
        }
      }
    }
  }

  @Nullable
  private static JBPopup createPopup(final ConfigurableSearchTextField searchField,
                                     final JBPopup[] activePopup,
                                     final Alarm showHintAlarm,
                                     final Consumer<String> selectConfigurable,
                                     Project project,
                                     int down) {

    String filter = searchField.getText();
    if (filter == null || filter.length() == 0) return null;
    Map<String, Set<String>> hints = SearchableOptionsRegistrar.getInstance().findPossibleExtension(filter, project);
    DefaultListModel model = new DefaultListModel();
    final JList list = new JBList(model);
    for (String groupName : hints.keySet()) {
      model.addElement(groupName);
      Set<String> descriptions = hints.get(groupName);
      if (descriptions != null) {
        for (String hit : descriptions) {
          if (hit == null) continue;
          model.addElement(new OptionDescription(null, groupName, hit, null));
        }
      }
    }
    list.setCellRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof String) {
          setText("------ " + value + " ------");
        }
        else if (value instanceof OptionDescription) {
          setText(((OptionDescription)value).getHit());
        }
        return rendererComponent;
      }
    });


    if (model.size() > 0) {
      Runnable onChosen = new Runnable() {
        public void run() {
          Object selectedValue = list.getSelectedValue();
          if (selectedValue instanceof OptionDescription) {
            final OptionDescription description = ((OptionDescription)selectedValue);
            searchField.setText(description.getHit());
            searchField.addCurrentTextToHistory();
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {     //do not show look up again
                showHintAlarm.cancelAllRequests();
                selectConfigurable.accept(description.getConfigurableId());
              }
            });
          }
        }
      };
      JBPopup popup = new PopupChooserBuilder<>(list)
        .setItemChoosenCallback(onChosen)
        .setRequestFocus(down != 0)
        .createPopup();
      list.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() != KeyEvent.VK_ENTER && e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_DOWN &&
              e.getKeyCode() != KeyEvent.VK_PAGE_UP && e.getKeyCode() != KeyEvent.VK_PAGE_DOWN) {
            searchField.requestFocusInWindow();
            if (cancelPopups(activePopup) && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
              return;
            }
            if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
              searchField.process(
                new KeyEvent(searchField, KeyEvent.KEY_TYPED, e.getWhen(), e.getModifiers(), KeyEvent.VK_UNDEFINED, e.getKeyChar()));
            }
          }
        }

      });
      if (down > 0) {
        if (list.getSelectedIndex() < list.getModel().getSize() - 1) {
          list.setSelectedIndex(list.getSelectedIndex() + 1);
        }
      }
      else if (down < 0) {
        if (list.getSelectedIndex() > 0) {
          list.setSelectedIndex(list.getSelectedIndex() - 1);
        }
      }
      return popup;
    }
    return null;
  }

  public static void showHintPopup(ConfigurableSearchTextField searchField,
                                   JBPopup[] activePopup,
                                   Alarm showHintAlarm,
                                   Consumer<String> selectConfigurable,
                                   Project project) {
    for (JBPopup aPopup : activePopup) {
      if (aPopup != null) {
        aPopup.cancel();
      }
    }

    JBPopup popup = createPopup(searchField, activePopup, showHintAlarm, selectConfigurable, project, 0); //no selection
    if (popup != null) {
      popup.showUnderneathOf(searchField);
      searchField.requestFocusInWindow();
    }

    activePopup[0] = popup;
    activePopup[1] = null;
  }


  public static void registerKeyboardNavigation(final ConfigurableSearchTextField searchField,
                                                final JBPopup[] activePopup,
                                                final Alarm showHintAlarm,
                                                final Consumer<String> selectConfigurable,
                                                final Project project) {
    final Consumer<Integer> shower = new Consumer<Integer>() {
      public void accept(Integer direction) {
        if (activePopup[0] != null) {
          activePopup[0].cancel();
        }

        if (activePopup[1] != null && activePopup[1].isVisible()) {
          return;
        }

        JBPopup popup = createPopup(searchField, activePopup, showHintAlarm, selectConfigurable, project, direction.intValue());
        if (popup != null) {
          popup.showUnderneathOf(searchField);
        }
        activePopup[0] = null;
        activePopup[1] = popup;
      }
    };
    searchField.registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        shower.accept(1);
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    searchField.registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        shower.accept(-1);
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    searchField.addKeyboardListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && searchField.getText().length() > 0) {
          e.consume();
          if (cancelPopups(activePopup)) return;
          searchField.setText("");
        }
        else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          searchField.addCurrentTextToHistory();
          cancelPopups(activePopup);
          if (e.getModifiers() == 0) {
            e.consume();
          }
        }
      }
    });
  }

  private static boolean cancelPopups(JBPopup[] activePopup) {
    for (JBPopup popup : activePopup) {
      if (popup != null && popup.isVisible()) {
        popup.cancel();
        return true;
      }
    }
    return false;
  }

  public static List<Set<String>> findKeys(String filter, Set<String> quoted) {
    filter = processFilter(filter.toLowerCase(), quoted);
    List<Set<String>> keySetList = new ArrayList<Set<String>>();
    SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
    Set<String> words = optionsRegistrar.getProcessedWords(filter);
    for (String word : words) {
      Set<OptionDescription> descriptions = ((SearchableOptionsRegistrarImpl)optionsRegistrar).getAcceptableDescriptions(word);
      Set<String> keySet = new HashSet<String>();
      if (descriptions != null) {
        for (OptionDescription description : descriptions) {
          keySet.add(description.getPath());
        }
      }
      keySetList.add(keySet);
    }
    return keySetList;
  }

  public static String processFilter(String filter, Set<String> quoted) {
    String withoutQuoted = "";
    int beg = 0;
    Matcher matcher = QUOTED.matcher(filter);
    while (matcher.find()) {
      int start = matcher.start(1);
      withoutQuoted += " " + filter.substring(beg, start);
      beg = matcher.end(1);
      String trimmed = filter.substring(start, beg).trim();
      if (trimmed.length() > 0) {
        quoted.add(trimmed);
      }
    }
    return withoutQuoted + " " + filter.substring(beg);
  }

  //to process event
  public static class ConfigurableSearchTextField extends SearchTextFieldWithStoredHistory {
    public ConfigurableSearchTextField() {
      super("ALL_CONFIGURABLES_PANEL_SEARCH_HISTORY");
    }

    public void process(KeyEvent e) {
      ((TextFieldWithProcessing)getTextEditor()).processKeyEvent(e);
    }
  }

  public static List<Configurable> expand(Configurable[] configurables) {
    return expandGroup(configurables);
  }

  public static List<Configurable> expandGroup(Configurable[] configurables) {
    List<Configurable> result = new ArrayList<Configurable>();
    ContainerUtil.addAll(result, configurables);
    for (Configurable each : configurables) {
      addChildren(each, result);
    }
    
    result = ContainerUtil.filter(result, configurable -> !(configurable instanceof SearchableConfigurable.Parent) || ((SearchableConfigurable.Parent)configurable).isVisible());
   
    return result;
  }

  private static void addChildren(Configurable configurable, List<Configurable> list) {
    if (configurable instanceof Configurable.Composite) {
      Configurable[] kids = ((Configurable.Composite)configurable).getConfigurables();
      for (Configurable eachKid : kids) {
        list.add(eachKid);
        addChildren(eachKid, list);
      }
    }
  }

}
