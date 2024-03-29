// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.FilePosition;
import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.FileMessageEvent;
import consulo.build.ui.event.FileMessageEventResult;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class FileMessageEventImpl extends MessageEventImpl implements FileMessageEvent {

  private final FilePosition myFilePosition;

  public FileMessageEventImpl(@Nonnull Object parentId,
                              @Nonnull Kind kind,
                              @Nonnull NotificationGroup group,
                              @Nonnull @BuildEventsNls.Message String message,
                              @Nullable @BuildEventsNls.Description String detailedMessage,
                              @Nonnull FilePosition filePosition) {
    super(parentId, kind, group, message, detailedMessage);
    myFilePosition = filePosition;
  }

  @Override
  public FileMessageEventResult getResult() {
    return new FileMessageEventResult() {
      @Override
      public FilePosition getFilePosition() {
        return myFilePosition;
      }

      @Override
      public Kind getKind() {
        return FileMessageEventImpl.this.getKind();
      }

      @Override
      @Nullable
      public String getDetails() {
        return getDescription();
      }
    };
  }

  @Override
  public FilePosition getFilePosition() {
    return myFilePosition;
  }

  @Override
  public
  @Nullable
  String getHint() {
    String hint = super.getHint();
    if (hint == null && myFilePosition.getStartLine() >= 0) {
      hint = ":" + (myFilePosition.getStartLine() + 1);
    }
    return hint;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@Nonnull Project project) {
    return new FileNavigatable(project, myFilePosition);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    FileMessageEventImpl event = (FileMessageEventImpl)o;
    return Objects.equals(myFilePosition, event.myFilePosition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), myFilePosition);
  }
}
