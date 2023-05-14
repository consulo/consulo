/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.ServiceManager;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.im.InputContext;
import java.util.Locale;

/**
 * @author Denis Fokin
 */

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "KeyboardSettings", storages = @Storage("keyboard.xml"))
public class KeyboardSettingsExternalizable implements PersistentStateComponent<KeyboardSettingsExternalizable.OptionSet> {

  private static final String[] supportedNonEnglishLanguages = {"de", "fr", "it", "uk"};

  public static boolean isSupportedKeyboardLayout(@Nonnull Component component) {
    if (Registry.is("ide.keyboard.dvorak")) return true;
    if (SystemInfo.isMac) return false;
    String keyboardLayoutLanguage = getLanguageForComponent(component);
    for (String language : supportedNonEnglishLanguages) {
      if (language.equals(keyboardLayoutLanguage)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static String getLanguageForComponent(@Nonnull Component component) {
    final Locale locale = getLocaleForComponent(component);
    return locale == null ? null : locale.getLanguage();
  }

  @Nullable
  protected static Locale getLocaleForComponent(@Nonnull Component component) {
    final InputContext context = component.getInputContext();
    return context == null ? null : context.getLocale();
  }

  @Nullable
  public static String getDisplayLanguageNameForComponent(@Nonnull Component component) {
    final Locale locale = getLocaleForComponent(component);
    return locale == null ? null : locale.getDisplayLanguage();
  }

  public static final class OptionSet {
    public boolean USE_NON_ENGLISH_KEYBOARD = false;
  }

  private OptionSet myOptions = new OptionSet();

  public static KeyboardSettingsExternalizable getInstance() {
    if (ApplicationManager.getApplication().isDisposed()) {
      return new KeyboardSettingsExternalizable();
    }
    else {
      return ServiceManager.getService(KeyboardSettingsExternalizable.class);
    }
  }

  @Nullable
  @Override
  public OptionSet getState() {
    return myOptions;
  }

  @Override
  public void loadState(OptionSet state) {
    myOptions = state;
  }

  public boolean isUkrainianKeyboard(Component c) {
    return c != null && "uk".equals(c.getInputContext().getLocale().getLanguage());
  }

  public boolean isNonEnglishKeyboardSupportEnabled() {
    return myOptions.USE_NON_ENGLISH_KEYBOARD;
  }

  public void setNonEnglishKeyboardSupportEnabled(boolean enabled) {
    myOptions.USE_NON_ENGLISH_KEYBOARD = enabled;
  }

}
