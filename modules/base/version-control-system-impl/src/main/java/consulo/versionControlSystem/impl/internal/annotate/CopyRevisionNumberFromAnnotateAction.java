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
package consulo.versionControlSystem.impl.internal.annotate;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.action.UpToDateLineNumberListener;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.ui.ex.awt.transferable.TextTransferable;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class CopyRevisionNumberFromAnnotateAction extends AnAction implements UpToDateLineNumberListener {
    private final FileAnnotation myAnnotation;
    private int myLineNumber = -1;

    public CopyRevisionNumberFromAnnotateAction(FileAnnotation annotation) {
        super("Copy revision number");
        myAnnotation = annotation;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (myLineNumber < 0) {
            return;
        }
        VcsRevisionNumber revisionNumber = myAnnotation.getLineRevisionNumber(myLineNumber);
        if (revisionNumber != null) {
            String revision = revisionNumber.asString();
            CopyPasteManager.getInstance().setContents(new TextTransferable(revision));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean enabled = myLineNumber >= 0 && myAnnotation.getLineRevisionNumber(myLineNumber) != null;
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void accept(int integer) {
        myLineNumber = integer;
    }
}
