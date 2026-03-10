/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.desktop.awt.internal.project;

import consulo.disposer.Disposable;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author peter
 */
public class DumbUnawareHider extends JPanel {
    public static DumbUnawareHider wrapGently(@Nonnull Project project,
                                              @Nonnull JComponent dumbUnawareContent,
                                              @Nonnull Disposable parentDisposable) {
        DumbService dumbService = DumbService.getInstance(project);

        final DumbUnawareHider wrapper = new DumbUnawareHider(dumbUnawareContent);
        wrapper.setContentVisible(!dumbService.isDumb());
        project.getMessageBus().connect(parentDisposable).subscribe(DumbModeListener.class, new DumbModeListener() {

            @Override
            public void enteredDumbMode() {
                wrapper.setContentVisible(false);
            }

            @Override
            public void exitDumbMode() {
                wrapper.setContentVisible(true);
            }
        });

        return wrapper;
    }

    private static final String CONTENT = "content";
    private static final String EXCUSE = "excuse";

    public DumbUnawareHider(JComponent dumbUnawareContent) {
        super(new CardLayout());
        add(dumbUnawareContent, CONTENT);
        JLabel label = new JLabel("This view is not available until indices are built");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, EXCUSE);
    }

    public void setContentVisible(boolean show) {
        ((CardLayout) getLayout()).show(this, show ? CONTENT : EXCUSE);
    }
}
