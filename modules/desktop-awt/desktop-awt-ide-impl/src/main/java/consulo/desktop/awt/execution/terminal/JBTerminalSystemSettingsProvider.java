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
package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.settings.DefaultTabbedSettingsProvider;
import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.colorScheme.*;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.colorScheme.impl.FontPreferencesImpl;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.terminal.TerminalConsoleSettings;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.KeymapManager;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author traff
 */
public class JBTerminalSystemSettingsProvider extends DefaultTabbedSettingsProvider {
  private Set<TerminalSettingsListener> myListeners = new HashSet<>();

  private final MyColorSchemeDelegate myColorScheme;

  private final TerminalConsoleSettings myTerminalConsoleSettings;

  public JBTerminalSystemSettingsProvider(Application application, TerminalConsoleSettings consoleSettings, Disposable parent) {
    myColorScheme = createBoundColorSchemeDelegate(null);
    myTerminalConsoleSettings = consoleSettings;

    MessageBusConnection busConnection = application.getMessageBus().connect(parent);
    busConnection.subscribe(UISettingsListener.class, uiSettings ->
    {
      int size;
      if (UISettings.getInstance().PRESENTATION_MODE) {
        size = UISettings.getInstance().PRESENTATION_MODE_FONT_SIZE;
      }
      else {
        size = myColorScheme.getGlobal().getConsoleFontSize();
      }

      if (myColorScheme.getConsoleFontSize() != size) {
        myColorScheme.setConsoleFontSize(size);
        fireFontChanged();
      }
    });

    busConnection.subscribe(EditorColorsListener.class, new EditorColorsListener() {
      @Override
      public void globalSchemeChange(EditorColorsScheme scheme) {
        myColorScheme.updateGlobalScheme(scheme);
        fireFontChanged();
      }
    });
  }

  public TerminalConsoleSettings getTerminalConsoleSettings() {
    return myTerminalConsoleSettings;
  }

  @Override
  public KeyStroke[] getCopyKeyStrokes() {
    return getKeyStrokesByActionId("$Copy");
  }

  @Override
  public KeyStroke[] getPasteKeyStrokes() {
    return getKeyStrokesByActionId("$Paste");
  }

  @Override
  public KeyStroke[] getNextTabKeyStrokes() {
    return getKeyStrokesByActionId("NextTab");
  }

  @Override
  public KeyStroke[] getPreviousTabKeyStrokes() {
    return getKeyStrokesByActionId("PreviousTab");
  }

  @Override
  public ColorPalette getTerminalColorPalette() {
    return new JBTerminalSchemeColorPalette(myColorScheme);
  }

  private KeyStroke[] getKeyStrokesByActionId(String actionId) {
    List<KeyStroke> keyStrokes = new ArrayList<KeyStroke>();
    Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId);
    for (Shortcut sc : shortcuts) {
      if (sc instanceof KeyboardShortcut) {
        KeyStroke ks = ((KeyboardShortcut)sc).getFirstKeyStroke();
        keyStrokes.add(ks);
      }
    }

    return keyStrokes.toArray(new KeyStroke[keyStrokes.size()]);
  }

  @Override
  public boolean shouldCloseTabOnLogout(TtyConnector ttyConnector) {
    return myTerminalConsoleSettings.closeSessionOnLogout();
  }

  @Override
  public String tabName(TtyConnector ttyConnector, String sessionName) {
    //for local terminal use name from settings
    if (ttyConnector instanceof PtyProcessTtyConnector) {
      return myTerminalConsoleSettings.getTabNameOrDefault();
    }
    else {
      return sessionName;
    }
  }

  @Override
  public float getLineSpace() {
    return myColorScheme.getConsoleLineSpacing();
  }

  @Override
  public boolean useInverseSelectionColor() {
    return false;
  }

  @Override
  public TextStyle getSelectionColor() {
    return new TextStyle(TerminalColor.awt(TargetAWT.to(myColorScheme.getColor(EditorColors.SELECTION_FOREGROUND_COLOR))),
                         TerminalColor.awt(TargetAWT.to(myColorScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR))));
  }

  @Override
  public TextStyle getDefaultStyle() {
    return new TextStyle(TerminalColor.awt(TargetAWT.to(myColorScheme.getDefaultForeground())),
                         TerminalColor.awt(TargetAWT.to(myColorScheme.getDefaultBackground())));
  }

  @Override
  public Font getTerminalFont() {
    Font normalFont = Font.decode(getFontName());

    if (normalFont == null) {
      normalFont = super.getTerminalFont();
    }

    normalFont = normalFont.deriveFont(getTerminalFontSize());

    return normalFont;
  }

  public String getFontName() {
    List<String> fonts = myColorScheme.getConsoleFontPreferences().getEffectiveFontFamilies();

    if (fonts.size() > 0) {
      return fonts.get(0);
    }

    return "Monospaced-14";
  }

  @Override
  public float getTerminalFontSize() {
    return (float)myColorScheme.getConsoleFontSize();
  }


  @Override
  public boolean useAntialiasing() {
    return true; // we return true here because all the settings are checked again in UiSettings.setupAntialiasing
  }

  @Override
  public int caretBlinkingMs() {
    if (!EditorSettingsExternalizable.getInstance().isBlinkCaret()) {
      return 0;
    }
    return EditorSettingsExternalizable.getInstance().getBlinkPeriod();
  }

  @Override
  public int getBufferMaxLinesCount() {
    final int linesCount = 1000;
    if (linesCount > 0) {
      return linesCount;
    }
    else {
      return super.getBufferMaxLinesCount();
    }
  }

  public EditorColorsScheme getColorScheme() {
    return myColorScheme;
  }

  @Override
  public boolean audibleBell() {
    return myTerminalConsoleSettings.isSoundBell();
  }

  @Override
  public boolean enableMouseReporting() {
    return myTerminalConsoleSettings.isMouseReporting();
  }

  @Override
  public boolean copyOnSelect() {
    return myTerminalConsoleSettings.isCopyOnSelection();
  }

  @Override
  public boolean pasteOnMiddleMouseClick() {
    return myTerminalConsoleSettings.isPasteOnMiddleMouseButton();
  }

  @Nonnull
  private static MyColorSchemeDelegate createBoundColorSchemeDelegate(@Nullable final EditorColorsScheme
                                                                        customGlobalScheme) {
    return new MyColorSchemeDelegate(customGlobalScheme);
  }

  @Override
  public boolean forceActionOnMouseReporting() {
    return true;
  }

  private static class MyColorSchemeDelegate implements EditorColorsScheme {
    private final ModifiableFontPreferences myFontPreferences = new FontPreferencesImpl();
    private final HashMap<TextAttributesKey, TextAttributes> myOwnAttributes = new HashMap<TextAttributesKey,
      TextAttributes>();
    private final HashMap<EditorColorKey, ColorValue> myOwnColors = new HashMap<EditorColorKey, ColorValue>();
    private Map<EditorFontType, Font> myFontsMap = null;
    private String myFaceName = null;
    private EditorColorsScheme myGlobalScheme;

    private int myConsoleFontSize = -1;

    private MyColorSchemeDelegate(@Nullable final EditorColorsScheme globalScheme) {
      updateGlobalScheme(globalScheme);
      initFonts();
    }

    private EditorColorsScheme getGlobal() {
      return myGlobalScheme;
    }

    @Nonnull
    @Override
    public String getName() {
      return getGlobal().getName();
    }


    protected void initFonts() {
      String consoleFontName = getConsoleFontName();
      int consoleFontSize = getConsoleFontSize();
      myFontPreferences.clear();
      myFontPreferences.register(consoleFontName, consoleFontSize);

      myFontsMap = new EnumMap<EditorFontType, Font>(EditorFontType.class);

      Font plainFont = new Font(consoleFontName, Font.PLAIN, consoleFontSize);
      Font boldFont = new Font(consoleFontName, Font.BOLD, consoleFontSize);
      Font italicFont = new Font(consoleFontName, Font.ITALIC, consoleFontSize);
      Font boldItalicFont = new Font(consoleFontName, Font.BOLD | Font.ITALIC, consoleFontSize);

      myFontsMap.put(EditorFontType.PLAIN, plainFont);
      myFontsMap.put(EditorFontType.BOLD, boldFont);
      myFontsMap.put(EditorFontType.ITALIC, italicFont);
      myFontsMap.put(EditorFontType.BOLD_ITALIC, boldItalicFont);
    }

    @Override
    public void setName(String name) {
      getGlobal().setName(name);
    }

    @Override
    public TextAttributes getAttributes(TextAttributesKey key) {
      if (myOwnAttributes.containsKey(key)) {
        return myOwnAttributes.get(key);
      }
      return getGlobal().getAttributes(key);
    }

    @Override
    public void fillAttributes(@Nonnull Map<TextAttributesKey, TextAttributes> map) {

    }

    @Override
    public void setAttributes(TextAttributesKey key, TextAttributes attributes) {
      myOwnAttributes.put(key, attributes);
    }

    @Nonnull
    @Override
    public ColorValue getDefaultBackground() {
      ColorValue color = getGlobal().getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY);
      return color != null ? color : getGlobal().getDefaultBackground();
    }

    @Nonnull
    @Override
    public ColorValue getDefaultForeground() {
      ColorValue foregroundColor = getGlobal().getAttributes(ConsoleViewContentType.NORMAL_OUTPUT_KEY)
                                              .getForegroundColor();
      return foregroundColor != null ? foregroundColor : getGlobal().getDefaultForeground();
    }

    @Override
    public ColorValue getColor(EditorColorKey key) {
      if (myOwnColors.containsKey(key)) {
        return myOwnColors.get(key);
      }
      return getGlobal().getColor(key);
    }

    @Override
    public void setColor(EditorColorKey key, ColorValue color) {
      myOwnColors.put(key, color);
    }

    @Override
    public void fillColors(Map<EditorColorKey, ColorValue> map) {

    }

    @Nonnull
    @Override
    public FontPreferences getFontPreferences() {
      return myGlobalScheme.getFontPreferences();
    }

    @Override
    public void setFontPreferences(@Nonnull FontPreferences preferences) {
      throw new IllegalStateException();
    }

    @Override
    public int getEditorFontSize() {
      return getGlobal().getEditorFontSize();
    }

    @Override
    public int getEditorFontSize(boolean scale) {
      return getGlobal().getEditorFontSize(scale);
    }

    @Override
    public void setEditorFontSize(int fontSize) {

    }

    @Override
    public FontSize getQuickDocFontSize() {
      return myGlobalScheme.getQuickDocFontSize();
    }

    @Override
    public void setQuickDocFontSize(@Nonnull FontSize fontSize) {
      myGlobalScheme.setQuickDocFontSize(fontSize);
    }

    @Override
    public String getEditorFontName() {
      return getGlobal().getEditorFontName();
    }

    @Override
    public void setEditorFontName(String fontName) {
      throw new IllegalStateException();
    }

    @Override
    public Font getFont(EditorFontType key) {
      if (myFontsMap != null) {
        Font font = myFontsMap.get(key);
        if (font != null) {
          return font;
        }
      }
      return getGlobal().getFont(key);
    }

    @Override
    public void setFont(EditorFontType key, Font font) {
      if (myFontsMap == null) {
        initFonts();
      }
      myFontsMap.put(key, font);
    }

    @Override
    public float getLineSpacing() {
      return getGlobal().getLineSpacing();
    }

    @Override
    public void setLineSpacing(float lineSpacing) {
      getGlobal().setLineSpacing(lineSpacing);
    }

    @Override
    @Nullable
    public MyColorSchemeDelegate clone() {
      return null;
    }

    @Override
    public void readExternal(Element element) {
    }

    public void updateGlobalScheme(EditorColorsScheme scheme) {
      myFontsMap = null;
      myGlobalScheme = scheme == null ? EditorColorsManager.getInstance().getGlobalScheme() : scheme;
    }

    @Nonnull
    @Override
    public FontPreferences getConsoleFontPreferences() {
      return myFontPreferences;
    }

    @Override
    public void setConsoleFontPreferences(@Nonnull FontPreferences preferences) {
      preferences.copyTo(myFontPreferences);
      initFonts();
    }

    @Override
    public String getConsoleFontName() {
      if (myFaceName == null) {
        return getGlobal().getConsoleFontName();
      }
      else {
        return myFaceName;
      }
    }

    @Override
    public void setConsoleFontName(String fontName) {
      myFaceName = fontName;
      initFonts();
    }

    @Override
    public int getConsoleFontSize() {
      return getConsoleFontSize(true);
    }

    @Override
    public int getConsoleFontSize(boolean scale) {
      if (myConsoleFontSize == -1) {
        return getGlobal().getConsoleFontSize(scale);
      }
      else {
        return JBUI.scale(myConsoleFontSize);
      }
    }

    @Override
    public void setConsoleFontSize(int fontSize) {
      myConsoleFontSize = fontSize;
      initFonts();
    }

    @Override
    public float getConsoleLineSpacing() {
      return getGlobal().getConsoleLineSpacing();
    }

    @Override
    public void setConsoleLineSpacing(float lineSpacing) {
      getGlobal().setConsoleLineSpacing(lineSpacing);
    }
  }

  public void addListener(TerminalSettingsListener listener) {
    myListeners.add(listener);
  }

  public void removeListener(TerminalSettingsListener listener) {
    myListeners.remove(listener);
  }

  public void fireFontChanged() {
    for (TerminalSettingsListener l : myListeners) {
      l.fontChanged();
    }
  }
}
