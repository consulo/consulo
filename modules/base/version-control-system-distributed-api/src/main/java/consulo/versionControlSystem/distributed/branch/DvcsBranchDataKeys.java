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
package consulo.versionControlSystem.distributed.branch;

import consulo.util.dataholder.Key;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public interface DvcsBranchDataKeys {
    Key<Repository> REPOSITORY = Key.create("Dvcs.Branches.Repository");

    Key<DvcsRef> SELECTED_REF = Key.create("Dvcs.Branches.SelectedRef");
}
