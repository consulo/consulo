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
//! User-data directory resolution per OS.
//! Windows: SHGetKnownFolderPath (modern API, replaces CSIDL-based lookups).
//! macOS:   ~/Library/{Application Support,Caches}.
//! Linux:   XDG base dirs with fallbacks.

use anyhow::{Context, Result};
use std::env;
use std::path::PathBuf;

/// Subdir name under user-data for platform builds.
pub const APP_NAME: &str = "Consulo Platform";

// ===== User home =====

#[cfg(target_os = "windows")]
pub fn get_user_home() -> Result<PathBuf> {
    env::var("USERPROFILE")
        .map(PathBuf::from)
        .context("USERPROFILE not set")
}

#[cfg(target_family = "unix")]
#[allow(deprecated)]
pub fn get_user_home() -> Result<PathBuf> {
    env::home_dir().context("cannot determine user home")
}

// ===== Config dir (Roaming on Windows, Application Support on mac) =====

#[cfg(target_os = "windows")]
pub fn get_config_home() -> Result<PathBuf> {
    windows_known_folder(&windows::Win32::UI::Shell::FOLDERID_RoamingAppData)
}

#[cfg(target_os = "macos")]
pub fn get_config_home() -> Result<PathBuf> {
    Ok(get_user_home()?.join("Library/Application Support"))
}

#[cfg(target_os = "linux")]
pub fn get_config_home() -> Result<PathBuf> {
    xdg_dir("XDG_CONFIG_HOME", ".config")
}

// ===== Cache dir =====

#[cfg(target_os = "windows")]
pub fn get_caches_home() -> Result<PathBuf> {
    windows_known_folder(&windows::Win32::UI::Shell::FOLDERID_LocalAppData)
}

#[cfg(target_os = "macos")]
pub fn get_caches_home() -> Result<PathBuf> {
    Ok(get_user_home()?.join("Library/Caches"))
}

#[cfg(target_os = "linux")]
pub fn get_caches_home() -> Result<PathBuf> {
    xdg_dir("XDG_CACHE_HOME", ".cache")
}

// ===== Data dir (user builds live here on Linux) =====

#[cfg(target_os = "linux")]
pub fn get_data_home() -> Result<PathBuf> {
    xdg_dir("XDG_DATA_HOME", ".local/share")
}

// ===== Implementation helpers =====

#[cfg(target_os = "windows")]
fn windows_known_folder(rfid: &windows::core::GUID) -> Result<PathBuf> {
    use windows::Win32::UI::Shell;
    use windows::core::PWSTR;
    let result: PWSTR = unsafe { Shell::SHGetKnownFolderPath(rfid, Shell::KF_FLAG_CREATE, None) }
        .context("SHGetKnownFolderPath failed")?;
    let s = unsafe { result.to_string() }.context("known folder path not UTF-16")?;
    Ok(PathBuf::from(s))
}

#[cfg(target_os = "linux")]
fn xdg_dir(env_var: &str, fallback: &str) -> Result<PathBuf> {
    if let Ok(v) = env::var(env_var) {
        let p = PathBuf::from(v);
        if p.is_absolute() {
            return Ok(p);
        }
    }
    Ok(get_user_home()?.join(fallback))
}
