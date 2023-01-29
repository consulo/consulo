// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows to register a language extension for a group of languages defined by a certain criterion.
 * To use this, specify the ID of a meta-language in the "{@code language}" attribute of an extension in {@code plugin.xml}.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class MetaLanguage extends Language {
  public static final ExtensionPointName<MetaLanguage> EP_NAME = ExtensionPointName.create(MetaLanguage.class);

  protected MetaLanguage(@Nonnull String ID) {
    super(ID);
  }

  @Nonnull
  public static List<MetaLanguage> all() {
    return EP_NAME.getExtensionList();
  }

  /**
   * Checks if the given language matches the criterion of this meta-language.
   */
  public abstract boolean matchesLanguage(@Nonnull Language language);

  /**
   * Returns the list of all languages matching this meta-language.
   */
  @Nonnull
  public Collection<Language> getMatchingLanguages() {
    return Language.getRegisteredLanguages().stream().filter(this::matchesLanguage).collect(Collectors.toList());
  }
}
