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
package consulo.desktop.awt.language.editor;

import consulo.language.editor.generation.ClassMember;
import consulo.language.editor.internal.MemberChooserBuilderBase;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UnprotectedUserDataHolder;
import consulo.util.dataholder.UserDataHolder;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 20-Aug-22
 */
public class DesktopAWTMemberChooserBuilder<T extends ClassMember> extends MemberChooserBuilderBase<T> {
  public DesktopAWTMemberChooserBuilder(T[] elements) {
    super(elements);
  }

  @RequiredUIAccess
  @Override
  public void showAsync(@Nonnull Project project, @Nonnull Consumer<UserDataHolder> consumer) {
    MemberChooserImpl<T> dialog = new MemberChooserImpl<T>(myElements, myAllowEmptySelection, myAllowMultipleSelection, myOptions, project);
    dialog.setTitle(myTitle.get());
    dialog.showAsync().doWhenDone(() -> {
      List<T> selectedElements = dialog.getSelectedElements();

      UnprotectedUserDataHolder result = new UnprotectedUserDataHolder();
      result.putUserData(ClassMember.KEY_OF_LIST, (List<ClassMember>)selectedElements);
      for (Map.Entry<Key<Boolean>, CheckBox> entry : dialog.getOptionComponents().entrySet()) {
        result.putUserData(entry.getKey(), entry.getValue().getValue());
      }

      consumer.accept(result);
    });
  }
}
