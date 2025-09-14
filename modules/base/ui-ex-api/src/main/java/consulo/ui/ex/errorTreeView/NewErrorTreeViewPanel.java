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
package consulo.ui.ex.errorTreeView;

import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.localize.UILocalize;

/**
 * @see NewErrorTreeViewPanelFactory
 */
public interface NewErrorTreeViewPanel extends OccurenceNavigator, MutableErrorTreeView {
    interface ProcessController {
        void stopProcess();

        boolean isProcessStopped();
    }

    static String createExportPrefix(int line) {
        return line < 0 ? "" : UILocalize.errortreePrefixLine(line).get();
    }

    static String createRendererPrefix(int line, int column) {
        if (line < 0) {
            return "";
        }
        if (column < 0) {
            return "(" + line + ")";
        }
        return "(" + line + ", " + column + ")";
    }

    void setProcessController(ProcessController controller);

    void stopProcess();

    boolean canControlProcess();

    boolean isProcessStopped();

    void setCanHideWarningsOrInfos(boolean canHideWarningsOrInfos);
}
