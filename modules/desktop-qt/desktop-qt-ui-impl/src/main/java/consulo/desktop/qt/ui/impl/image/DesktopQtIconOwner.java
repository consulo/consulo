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
package consulo.desktop.qt.ui.impl.image;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * Anything which handed a rendered icon to qt - a pixmap of a label, an icon of an action - rather than letting
 * it be resolved at paint time. Such a snapshot stands for the icon library it was taken from, so it has to be
 * taken again whenever the active library changes.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public interface DesktopQtIconOwner {
    @RequiredUIAccess
    void refreshIcons();
}
