// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.ui.console;

import consulo.process.ProcessOutputType;
import consulo.process.ProcessOutputTypes;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexey Kudravtsev
 */
public class ConsoleViewContentType {
  private static final Logger LOG = Logger.getInstance(ConsoleViewContentType.class);

  private final String myName;
  private final TextAttributes myTextAttributes;
  private final TextAttributesKey myTextAttributesKey;

  private static final Map<Key, ConsoleViewContentType> ourRegisteredTypes = new HashMap<>();

  public static final EditorColorKey CONSOLE_BACKGROUND_KEY = EditorColorKey.createColorKey("CONSOLE_BACKGROUND_KEY");

  public static final TextAttributesKey LOG_DEBUG_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("LOG_DEBUG_OUTPUT");
  public static final TextAttributesKey LOG_VERBOSE_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("LOG_VERBOSE_OUTPUT");
  public static final TextAttributesKey LOG_INFO_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("LOG_INFO_OUTPUT");
  public static final TextAttributesKey LOG_WARNING_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("LOG_WARNING_OUTPUT");
  public static final TextAttributesKey LOG_ERROR_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("LOG_ERROR_OUTPUT");
  public static final TextAttributesKey LOG_EXPIRED_ENTRY = TextAttributesKey.createTextAttributesKey("LOG_EXPIRED_ENTRY");
  public static final TextAttributesKey NORMAL_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("CONSOLE_NORMAL_OUTPUT");
  public static final TextAttributesKey ERROR_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("CONSOLE_ERROR_OUTPUT");
  public static final TextAttributesKey USER_INPUT_KEY = TextAttributesKey.createTextAttributesKey("CONSOLE_USER_INPUT");
  public static final TextAttributesKey SYSTEM_OUTPUT_KEY = TextAttributesKey.createTextAttributesKey("CONSOLE_SYSTEM_OUTPUT");

  public static final ConsoleViewContentType LOG_DEBUG_OUTPUT = new ConsoleViewContentType("LOG_DEBUG_OUTPUT", LOG_DEBUG_OUTPUT_KEY);
  public static final ConsoleViewContentType LOG_VERBOSE_OUTPUT = new ConsoleViewContentType("LOG_VERBOSE_OUTPUT", LOG_VERBOSE_OUTPUT_KEY);
  public static final ConsoleViewContentType LOG_INFO_OUTPUT = new ConsoleViewContentType("LOG_INFO_OUTPUT", LOG_INFO_OUTPUT_KEY);
  public static final ConsoleViewContentType LOG_WARNING_OUTPUT = new ConsoleViewContentType("LOG_WARNING_OUTPUT", LOG_WARNING_OUTPUT_KEY);
  public static final ConsoleViewContentType LOG_ERROR_OUTPUT = new ConsoleViewContentType("LOG_ERROR_OUTPUT", LOG_ERROR_OUTPUT_KEY);
  public static final ConsoleViewContentType NORMAL_OUTPUT = new ConsoleViewContentType("NORMAL_OUTPUT", NORMAL_OUTPUT_KEY);
  public static final ConsoleViewContentType ERROR_OUTPUT = new ConsoleViewContentType("ERROR_OUTPUT", ERROR_OUTPUT_KEY);
  public static final ConsoleViewContentType SYSTEM_OUTPUT = new ConsoleViewContentType("SYSTEM_OUTPUT", SYSTEM_OUTPUT_KEY);
  public static final ConsoleViewContentType USER_INPUT = new ConsoleViewContentType("USER_INPUT", USER_INPUT_KEY);

  public static final ConsoleViewContentType[] OUTPUT_TYPES = {NORMAL_OUTPUT, ERROR_OUTPUT, USER_INPUT, SYSTEM_OUTPUT};

  static {
    ourRegisteredTypes.put(ProcessOutputTypes.SYSTEM, SYSTEM_OUTPUT);
    ourRegisteredTypes.put(ProcessOutputTypes.STDOUT, NORMAL_OUTPUT);
    ourRegisteredTypes.put(ProcessOutputTypes.STDERR, ERROR_OUTPUT);
  }

  public ConsoleViewContentType(String name, TextAttributes textAttributes) {
    myName = name;
    myTextAttributes = textAttributes;
    myTextAttributesKey = null;
  }

  public ConsoleViewContentType(String name, TextAttributesKey textAttributesKey) {
    myName = name;
    myTextAttributes = null;
    myTextAttributesKey = textAttributesKey;
  }

  public String toString() {
    return myName;
  }

  /**
   * Returns {@code TextAttributes} instance defining the visual representation of text.
   * <p> A subclass might override this method.
   *
   * @return not-null TextAttributes instance
   */
  public TextAttributes getAttributes() {
    if (myTextAttributesKey != null) {
      return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(myTextAttributesKey);
    }
    return myTextAttributes;
  }

  @Nullable
  public TextAttributes getForcedAttributes() {
    return myTextAttributes;
  }

  @Nullable
  public TextAttributesKey getAttributesKey() {
    return myTextAttributesKey;
  }

  @Nonnull
  public static ConsoleViewContentType registerNewConsoleViewType(@Nonnull Key key, @Nonnull TextAttributesKey attributesKey) {
    ConsoleViewContentType type = new ConsoleViewContentType(key.toString(), attributesKey);
    registerNewConsoleViewType(key, type);
    return type;
  }

  public static synchronized void registerNewConsoleViewType(@Nonnull Key processOutputType, @Nonnull ConsoleViewContentType attributes) {
    ourRegisteredTypes.put(processOutputType, attributes);
  }

  @Nonnull
  public static synchronized ConsoleViewContentType getConsoleViewType(@Nonnull Key processOutputType) {
    ConsoleViewContentType type = ourRegisteredTypes.get(processOutputType);
    if (type != null) {
      return type;
    }
    LOG.warn("Unregistered " + processOutputType.getClass().getName() + ": " + ProcessOutputType.getKeyNameForLogging(processOutputType));
    return SYSTEM_OUTPUT;
  }

  @Nonnull
  public static synchronized Collection<ConsoleViewContentType> getRegisteredTypes() {
    return ourRegisteredTypes.values();
  }
}
