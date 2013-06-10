package com.intellij.lang;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:59/30.05.13
 */
public interface LanguageVersion {
  Key<LanguageVersion> KEY = Key.create("LANGUAGE_VERSION");

  @NotNull
  @NonNls
  String getName();

  @NonNls
  Language getLanguage();
}
