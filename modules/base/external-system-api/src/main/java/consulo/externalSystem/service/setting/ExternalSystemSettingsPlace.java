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
package consulo.externalSystem.service.setting;

/**
 * Where the settings of an external project are being shown. The same settings are edited in both places, but not every option makes
 * sense in both - options about what to do with an already linked project have nothing to answer while it is still being imported.
 *
 * @author VISTALL
 */
public enum ExternalSystemSettingsPlace {
    /**
     * The settings page of a project which already has the external project linked.
     */
    SETTINGS,
    /**
     * The wizard importing the external project for the first time.
     */
    IMPORT
}
