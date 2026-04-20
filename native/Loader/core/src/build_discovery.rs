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
//! Enumerate `<install>/platform/build*` and user-dir `build*`, pick highest.

use crate::paths;
use anyhow::{Result, bail};
use log::debug;
use std::cmp::Ordering;
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Debug, Clone)]
pub struct ResolvedBuild {
    pub path: PathBuf,
    pub from_user_dir: bool,
}

/// Scan the install's `platform/` dir and the user's Consulo Platform dir;
/// return the build with the highest (numeric-aware) name.
pub fn resolve(install_dir: &Path) -> Result<ResolvedBuild> {
    let platform_dir = install_dir.join("platform");
    let mut candidates: Vec<(String, PathBuf, bool)> = Vec::new();

    collect_builds(&platform_dir, false, &mut candidates);
    if let Ok(user_dir) = user_build_root() {
        collect_builds(&user_dir, true, &mut candidates);
    }

    if candidates.is_empty() {
        bail!("no platform build found under {platform_dir:?}");
    }

    candidates.sort_by(|a, b| natural_cmp(&a.0, &b.0));
    let (_, path, from_user_dir) = candidates.pop().expect("non-empty");

    debug!("selected build {path:?} (user_dir={from_user_dir})");
    Ok(ResolvedBuild { path, from_user_dir })
}

fn collect_builds(dir: &Path, from_user_dir: bool, out: &mut Vec<(String, PathBuf, bool)>) {
    let entries = match fs::read_dir(dir) {
        Ok(e) => e,
        Err(_) => return,
    };
    for entry in entries.flatten() {
        let Ok(ft) = entry.file_type() else { continue };
        if !ft.is_dir() {
            continue;
        }
        let name = entry.file_name().to_string_lossy().into_owned();
        if !name.starts_with("build") {
            continue;
        }
        out.push((name, entry.path(), from_user_dir));
    }
}

#[cfg(any(target_os = "windows", target_os = "macos"))]
fn user_build_root() -> Result<PathBuf> {
    Ok(paths::get_config_home()?.join(paths::APP_NAME))
}

#[cfg(target_os = "linux")]
fn user_build_root() -> Result<PathBuf> {
    Ok(paths::get_data_home()?.join(paths::APP_NAME))
}

/// Numeric-aware compare: "build10" > "build9".
fn natural_cmp(a: &str, b: &str) -> Ordering {
    let ap = split_alpha_numeric(a);
    let bp = split_alpha_numeric(b);
    for (x, y) in ap.iter().zip(bp.iter()) {
        let ord = match (x.parse::<u64>(), y.parse::<u64>()) {
            (Ok(xn), Ok(yn)) => xn.cmp(&yn),
            _ => x.cmp(y),
        };
        if ord != Ordering::Equal {
            return ord;
        }
    }
    ap.len().cmp(&bp.len())
}

fn split_alpha_numeric(s: &str) -> Vec<&str> {
    let mut out = Vec::new();
    let mut start = 0;
    let mut in_digits = false;
    for (i, c) in s.char_indices() {
        let is_digit = c.is_ascii_digit();
        if i == 0 {
            in_digits = is_digit;
            continue;
        }
        if is_digit != in_digits {
            out.push(&s[start..i]);
            start = i;
            in_digits = is_digit;
        }
    }
    if start < s.len() {
        out.push(&s[start..]);
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn natural_order() {
        let mut v = vec!["build2", "build10", "build1", "build9"];
        v.sort_by(|a, b| natural_cmp(a, b));
        assert_eq!(v, vec!["build1", "build2", "build9", "build10"]);
    }
}
