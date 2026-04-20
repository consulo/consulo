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
//! Windows JRE discovery via registry walk.
//! Checks current LTS (25) first, then previous LTS (21), then `CurrentVersion`.

use super::validate_runtime_dir;
use anyhow::{Result, bail};
use std::path::{Path, PathBuf};
use winreg::RegKey;
use winreg::enums::*;

pub fn search() -> Result<PathBuf> {
    for flags in [KEY_READ, KEY_READ | KEY_WOW64_32KEY] {
        // Pinned LTS versions first.
        for version in ["25", "21"] {
            for subkey in [
                format!("Software\\JavaSoft\\JDK\\{version}"),
                format!("Software\\JavaSoft\\JRE\\{version}"),
            ] {
                if let Ok(p) = open_java_home(&subkey, flags) {
                    return Ok(p);
                }
            }
        }
        // CurrentVersion fallback.
        for root in ["Software\\JavaSoft\\JDK", "Software\\JavaSoft\\JRE"] {
            if let Ok(p) = via_current_version(root, flags) {
                return Ok(p);
            }
        }
    }
    bail!("no JRE found in registry")
}

fn open_java_home(subkey: &str, flags: u32) -> Result<PathBuf> {
    let hklm = RegKey::predef(HKEY_LOCAL_MACHINE);
    let key = hklm.open_subkey_with_flags(subkey, flags)?;
    let java_home: String = key.get_value("JavaHome")?;
    validate_runtime_dir(Path::new(&java_home))
}

fn via_current_version(root: &str, flags: u32) -> Result<PathBuf> {
    let hklm = RegKey::predef(HKEY_LOCAL_MACHINE);
    let root_key = hklm.open_subkey_with_flags(root, flags)?;
    let version: String = root_key.get_value("CurrentVersion")?;
    open_java_home(&format!("{root}\\{version}"), flags)
}
