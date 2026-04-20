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
//! Assemble the final `JavaVMInitArgs` option list.
//!
//! Order matters: app.vmoptions come before user vmoptions so users can
//! override defaults (JVM last-wins for duplicate `-D` options).

use crate::path_ext::PathExt;
use crate::variant;
use anyhow::Result;
use std::path::Path;

/// The class-path separator for the host platform.
#[cfg(target_os = "windows")]
pub const CLASS_PATH_SEP: &str = ";";
#[cfg(target_family = "unix")]
pub const CLASS_PATH_SEP: &str = ":";

/// Inputs for option assembly. Keeps the `assemble` function signature stable.
#[derive(Debug, Clone)]
pub struct LaunchInputs<'a> {
    /// `<install>/platform/buildNNN/` — the chosen build.
    pub build_dir: &'a Path,
    /// The application home (e.g. `<install>` on Windows, `*.app` on macOS).
    pub app_home: &'a Path,
    /// `consulo.properties` location.
    pub properties_file: &'a Path,
    /// User vmoptions (from user config dir or `$CONSULO_VM_OPTIONS`).
    pub user_vmoptions: Vec<String>,
    /// App vmoptions (from `<build>/bin/app.vmoptions`).
    pub app_vmoptions: Vec<String>,
}

/// Build the full option list passed to `JNI_CreateJavaVM`.
pub fn assemble(inputs: LaunchInputs<'_>) -> Result<Vec<String>> {
    let mut opts = Vec::with_capacity(64);

    // Module-path boot: only `boot/`; `boot/spi` is NOT included.
    let module_path = inputs.build_dir.join("boot").to_string_checked()?;
    opts.push(format!("--module-path={module_path}"));
    opts.push(format!("-Djdk.module.main={}", variant::MAIN_MODULE));

    // App-level vmoptions (<build>/bin/app.vmoptions) go first so users can override.
    opts.extend(inputs.app_vmoptions);
    // User vmoptions.
    opts.extend(inputs.user_vmoptions);

    // Launcher-emitted properties (non-negotiable).
    let build = inputs.build_dir.to_string_checked()?;
    let app_home = inputs.app_home.to_string_checked()?;
    let props = inputs.properties_file.to_string_checked()?;

    opts.push(format!("-Dconsulo.home.path={build}"));
    opts.push(format!("-Dconsulo.app.home.path={app_home}"));
    opts.push(format!("-Dconsulo.properties.file={props}"));
    opts.push("-Dconsulo.platform.native.launcher=true".to_string());
    opts.push("-Dconsulo.module.path.boot=true".to_string());

    // Deprecated aliases (transition; remove one release cycle after Rust launcher ships).
    opts.push(format!("-Didea.home.path={build}"));
    opts.push(format!("-Didea.properties.file={props}"));

    Ok(opts)
}
