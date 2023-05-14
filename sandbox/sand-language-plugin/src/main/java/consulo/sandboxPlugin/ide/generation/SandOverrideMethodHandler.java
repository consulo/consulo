/*
 * Copyright 2013-2022 consulo.io
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
package consulo.sandboxPlugin.ide.generation;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.generation.ClassMember;
import consulo.language.editor.generation.MemberChooserBuilder;
import consulo.language.editor.generation.OverrideMethodHandler;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.KeyWithDefaultValue;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17-Jul-22
 */
@ExtensionImpl
public class SandOverrideMethodHandler implements OverrideMethodHandler {
  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    SandClassNode parent = new SandClassNode();

    MemberChooserBuilder<ClassMember> builder = MemberChooserBuilder.create(new ClassMember[]{new SandClassMember(parent)});
    builder.withTitle(LocalizeValue.localizeTODO("Select Test"));
    builder.withOption(KeyWithDefaultValue.create("Option 1", () -> Boolean.FALSE), LocalizeValue.localizeTODO("Option 1"));
    builder.withOption(KeyWithDefaultValue.create("Option 2", () -> Boolean.FALSE), LocalizeValue.localizeTODO("Option 2"));
    builder.withOption(KeyWithDefaultValue.create("Option 3", () -> Boolean.TRUE), LocalizeValue.localizeTODO("Option 3"));

    builder.showAsync(project, dataHolder -> {
      
    });
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }
}
