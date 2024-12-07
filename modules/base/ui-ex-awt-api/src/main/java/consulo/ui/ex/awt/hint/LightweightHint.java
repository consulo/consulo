/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ui.ex.awt.hint;

import consulo.ui.ex.RelativePoint;
import consulo.util.dataholder.UserDataHolder;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2024-12-06
 */
public interface LightweightHint extends Hint, UserDataHolder {
    boolean canControlAutoHide();

    boolean isInsideHint(RelativePoint target);

    void setAutoHideTester(Predicate<TooltipEvent> autoHideTester);
}
