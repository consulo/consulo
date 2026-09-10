/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * Invoked when a link inside a rich message box body is activated. A frontend which cannot report
 * link activation simply never calls this, so a caller must not rely on it as the only way out.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
@FunctionalInterface
public interface MessageLinkHandler {
    /**
     * @param href the raw target of the link, not user visible
     */
    @RequiredUIAccess
    void linkActivated(String href);
}
