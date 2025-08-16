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
package consulo.ide.impl.idea.ide.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.ui.ex.action.OptionDescription;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.OptionsTopHitProvider;
import consulo.ui.ex.action.SearchTopHitProvider;

import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class UISimpleSettingsProvider implements SearchTopHitProvider, OptionsTopHitProvider.CoveredByToggleActions {
  private static OptionDescription CYCLING_SCROLLING = AppearanceOptionsTopHitProvider.appearance("Cyclic scrolling", "CYCLE_SCROLLING");
  private static OptionDescription MEMORY_INDICATOR = AppearanceOptionsTopHitProvider.appearance("Show Memory Indicator",
                                                                                                 "SHOW_MEMORY_INDICATOR");
  private static OptionDescription SHOW_MAIN_TOOLBAR = AppearanceOptionsTopHitProvider.appearance("Show Main Toolbar", "SHOW_MAIN_TOOLBAR");
  private static OptionDescription SHOW_NAVIGATION_BAR = AppearanceOptionsTopHitProvider.appearance("Show Navigation Bar",
                                                                                                    "SHOW_NAVIGATION_BAR");
  private static OptionDescription HIDE_TOOL_STRIPES = AppearanceOptionsTopHitProvider.appearance("Hide Tool Window Bars",
                                                                                                  "HIDE_TOOL_STRIPES");
  private static OptionDescription SHOW_STATUS_BAR = AppearanceOptionsTopHitProvider.appearance("Show Status Bar", "SHOW_STATUS_BAR");
  private static OptionDescription IS_BLOCK_CURSOR =
    EditorOptionsTopHitProvider.editor(LocalizeValue.localizeTODO("Show Block Cursor"), "IS_BLOCK_CURSOR");
  private static OptionDescription IS_WHITESPACES_SHOWN =
    EditorOptionsTopHitProvider.editor(LocalizeValue.localizeTODO("Show Whitespaces"), "IS_WHITESPACES_SHOWN");
  private static OptionDescription ARE_LINE_NUMBERS_SHOWN = EditorOptionsTopHitProvider.editor(LocalizeValue.localizeTODO(
                                                                                                 "Show Line Numbers"),
                                                                                               "ARE_LINE_NUMBERS_SHOWN");
  private static OptionDescription SHOW_METHOD_SEPARATORS =
    new DaemonCodeAnalyzerOptionDescription("SHOW_METHOD_SEPARATORS", "Show Method Separators", "appearance");


  @Override
  public void consumeTopHits(String pattern, Consumer<Object> collector, ComponentManager project) {
    pattern = pattern.trim().toLowerCase();
    if (StringUtil.isBetween(pattern, "cyc", "cyclic ") || StringUtil.isBetween(pattern, "scr", "scroll ")) {
      collector.accept(CYCLING_SCROLLING);
    }
    else if (patternContains(pattern, "memo")) {
      collector.accept(MEMORY_INDICATOR);
    }
    else if (StringUtil.isBetween(pattern, "nav", "navigation bar ") || StringUtil.isBetween(pattern, "navb", "navbar ")) {
      collector.accept(SHOW_NAVIGATION_BAR);
    }
    else if (StringUtil.isBetween(pattern, "tool", "toolbar ")) {
      collector.accept(SHOW_MAIN_TOOLBAR);
    }
    else if (StringUtil.isBetween(pattern, "tool w", "tool window bars") || StringUtil.isBetween(pattern, "toolw", "toolwindow ")) {
      collector.accept(HIDE_TOOL_STRIPES);
    }
    else if (StringUtil.isBetween(pattern, "stat", "status bar ")) {
      collector.accept(SHOW_STATUS_BAR);
    }
    else if (StringUtil.isBetween(pattern, "curs", "cursor ") || StringUtil.isBetween(pattern, "block ", "block cursor ")
      || StringUtil.isBetween(pattern, "caret", "caret ") || StringUtil.isBetween(pattern, "block ", "block caret ")) {
      collector.accept(IS_BLOCK_CURSOR);
    }
    else if (StringUtil.isBetween(pattern, "whites", "whitespaces ") || StringUtil.isBetween(pattern, "show whi", "show whitespaces ")) {
      collector.accept(IS_WHITESPACES_SHOWN);
    }
    else if (StringUtil.isBetween(pattern, "line ", "line numbers ") || StringUtil.isBetween(pattern, "show li", "show line numbers ")) {
      collector.accept(ARE_LINE_NUMBERS_SHOWN);
    }
    else if (StringUtil.isBetween(pattern, "separa ", "separators ") || StringUtil.isBetween(pattern, "method s", "method separators ")) {
      collector.accept(SHOW_METHOD_SEPARATORS);
    }
  }

  private static boolean patternContains(String pattern, String search) {
    for (String s : pattern.split(" ")) {
      if (s.contains(search)) {
        return true;
      }
    }
    return false;
  }
}
