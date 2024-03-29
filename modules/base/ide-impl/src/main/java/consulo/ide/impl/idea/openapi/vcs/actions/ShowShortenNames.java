/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.application.dumb.DumbAware;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;

/**
 * @author Konstantin Bulenkov
 */
public class ShowShortenNames extends ActionGroup {
  private final AnAction[] myChildren;

  public ShowShortenNames(final EditorGutterComponentEx gutter) {
    super("Names", true);
    final ArrayList<AnAction> kids = new ArrayList<>(ShortNameType.values().length);
    for (ShortNameType type : ShortNameType.values()) {
      kids.add(new SetShortNameTypeAction(type, gutter));
    }
    myChildren = kids.toArray(new AnAction[kids.size()]);
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@jakarta.annotation.Nullable AnActionEvent e) {
    return myChildren;
  }

  public static ShortNameType getType() {
    for (ShortNameType type : ShortNameType.values()) {
      if (type.isSet()) {
        return type;
      }
    }
    return ShortNameType.LASTNAME;
  }

  public static class SetShortNameTypeAction extends ToggleAction implements DumbAware {
    private final ShortNameType myType;
    private final EditorGutterComponentEx myGutter;

    public SetShortNameTypeAction(ShortNameType type, EditorGutterComponentEx gutter) {
      super(type.getDescription());
      myType = type;
      myGutter = gutter;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myType == getType();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean enabled) {
      if (enabled) {
        myType.set();
      }
      myGutter.revalidateMarkup();
    }
  }
}
