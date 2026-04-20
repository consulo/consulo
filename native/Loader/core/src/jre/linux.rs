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
//! Linux JRE discovery — glob well-known roots, fall back to update-alternatives.
//! Skipped on loongarch64 (see `jre::locate`).

use super::{MIN_JAVA_VERSION, validate_runtime_dir};
use anyhow::{Result, bail};
use log::debug;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

const ROOTS: &[&str] = &["/usr/lib/jvm", "/usr/java", "/opt"];

pub fn search() -> Result<PathBuf> {
    for root in ROOTS {
        let Ok(entries) = fs::read_dir(root) else {
            continue;
        };
        for entry in entries.flatten() {
            if let Ok(p) = validate_runtime_dir(&entry.path()) {
                if meets_min_version(&p) {
                    return Ok(p);
                }
            }
        }
    }

    // update-alternatives (Debian/Ubuntu) fallback.
    if let Ok(p) = via_update_alternatives() {
        if meets_min_version(&p) {
            return Ok(p);
        }
    }

    bail!(
        "no JRE ≥ {MIN_JAVA_VERSION} found in /usr/lib/jvm, /usr/java, /opt, or update-alternatives"
    )
}

fn via_update_alternatives() -> Result<PathBuf> {
    let out = Command::new("update-alternatives")
        .args(["--query", "java"])
        .output()?;
    if !out.status.success() {
        bail!("update-alternatives --query java failed");
    }
    let stdout = String::from_utf8_lossy(&out.stdout);
    for line in stdout.lines() {
        if let Some(rest) = line.strip_prefix("Value: ") {
            let java_bin = Path::new(rest.trim());
            // `<home>/bin/java` → `<home>` = parent of parent.
            if let Some(home) = java_bin.parent().and_then(Path::parent) {
                return validate_runtime_dir(home);
            }
        }
    }
    bail!("no Value: line in update-alternatives output")
}

/// Invoke `<jre>/bin/java -version`, parse the first quoted version token,
/// check the major matches `MIN_JAVA_VERSION`.
fn meets_min_version(jre_home: &Path) -> bool {
    match major_version(jre_home) {
        Some(v) if v >= MIN_JAVA_VERSION => {
            debug!("[JRE] {jre_home:?}: version {v} ≥ {MIN_JAVA_VERSION}");
            true
        }
        Some(v) => {
            debug!("[JRE] {jre_home:?}: version {v} < {MIN_JAVA_VERSION}, skipping");
            false
        }
        None => {
            debug!("[JRE] {jre_home:?}: could not determine version, skipping");
            false
        }
    }
}

fn major_version(jre_home: &Path) -> Option<u32> {
    let java = jre_home.join("bin/java");
    let out = Command::new(&java).arg("-version").output().ok()?;
    // `java -version` prints to stderr; first line looks like:
    //   openjdk version "25.0.1" 2025-10-21
    //   java version "21.0.1" 2023-10-17
    let stderr = String::from_utf8_lossy(&out.stderr);
    let line = stderr.lines().next()?;
    let quoted = line.split('"').nth(1)?;
    let major_str = quoted.split('.').next()?;
    major_str.parse().ok()
}
