// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.FilePosition;
import consulo.build.ui.event.FileMessageEvent;
import consulo.build.ui.event.FileMessageEventResult;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class FileMessageEventImpl extends MessageEventImpl implements FileMessageEvent {
    private final FilePosition myFilePosition;

    public FileMessageEventImpl(
        Object parentId,
        Kind kind,
        NotificationGroup group,
        LocalizeValue message,
        LocalizeValue detailedMessage,
        FilePosition filePosition
    ) {
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
            public LocalizeValue getDetails() {
                return getDescription();
            }
        };
    }

    @Override
    public FilePosition getFilePosition() {
        return myFilePosition;
    }

    @Override
    public LocalizeValue getHint() {
        LocalizeValue hint = super.getHint();
        if (hint.isEmpty() && myFilePosition.startLine() >= 0) {
            hint = LocalizeValue.of(":" + (myFilePosition.startLine() + 1));
        }
        return hint;
    }

    @Override
    public @Nullable Navigatable getNavigatable(Project project) {
        return new FileNavigatable(project, myFilePosition);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FileMessageEventImpl that = (FileMessageEventImpl) o;
        return Objects.equals(myFilePosition, that.myFilePosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), myFilePosition);
    }
}
