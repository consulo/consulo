// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.fileSet;

import consulo.language.psi.PsiFile;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

public interface FileSetDescriptor {
  /**
   * Checks if the file set entry matches the given PSI file.
   *
   * @param psiFile The PSI file to check against.
   * @return True if there is a match, false otherwise.
   */
  boolean matches(PsiFile psiFile);

  default @Nullable String getName() {
    return null;
  }

  
  String getType();

  @Nullable String getPattern();

  void setPattern(@Nullable String pattern);

  
  default State getState() {
    return new FileSetDescriptor.State(getType(), getName(), getPattern());
  }

  @Tag("fileSet")
  final class State {
    @Attribute("type")
    public String type;

    @Attribute("name")
    public @Nullable String name;

    @Attribute("pattern")
    public @Nullable String pattern;

    public State() {
    }

    public State(String type, @Nullable String name, @Nullable String pattern) {
      this.type = type;
      this.name = name;
      this.pattern = pattern;
    }
  }
}
