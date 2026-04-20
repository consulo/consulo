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
//! macOS JRE discovery — scan `JavaVirtualMachines` dirs for `.jdk` / `.jre` bundles.

use super::{MIN_JAVA_VERSION, validate_runtime_dir};
use anyhow::{Result, bail};
use std::fs;
use std::path::{Path, PathBuf};

pub fn search() -> Result<PathBuf> {
    let home = std::env::var_os("HOME").map(PathBuf::from);
    let mut roots: Vec<PathBuf> = vec![
        PathBuf::from("/Library/Java/JavaVirtualMachines"),
        PathBuf::from("/System/Library/Java/JavaVirtualMachines"),
    ];
    if let Some(h) = home {
        roots.insert(0, h.join("Library/Java/JavaVirtualMachines"));
    }

    let mut candidates: Vec<(u32, PathBuf)> = Vec::new();
    for root in &roots {
        collect_bundles(root, &mut candidates);
    }

    // Highest version first.
    candidates.sort_by(|a, b| b.0.cmp(&a.0));

    for (_, bundle) in candidates {
        if let Ok(p) = validate_runtime_dir(&bundle) {
            return Ok(p);
        }
    }
    bail!("no matching JVM bundle ≥ {MIN_JAVA_VERSION}")
}

fn collect_bundles(root: &Path, out: &mut Vec<(u32, PathBuf)>) {
    let Ok(entries) = fs::read_dir(root) else {
        return;
    };
    for entry in entries.flatten() {
        let p = entry.path();
        let name = p
            .file_name()
            .map(|n| n.to_string_lossy().to_string())
            .unwrap_or_default();
        if !(name.ends_with(".jdk") || name.ends_with(".jre")) {
            continue;
        }
        let v = extract_major_version(&name);
        if v >= MIN_JAVA_VERSION {
            out.push((v, p));
        }
    }
}

/// Extract the major version from a bundle name like "jdk-25.0.1.jdk".
/// Falls back to parsing the Info.plist JVMVersion key in a later iteration.
fn extract_major_version(name: &str) -> u32 {
    // Skip non-digit prefix.
    let start = name.find(|c: char| c.is_ascii_digit()).unwrap_or(name.len());
    let rest = &name[start..];
    let digits: String = rest.chars().take_while(|c| c.is_ascii_digit()).collect();
    digits.parse().unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn parses_version() {
        assert_eq!(extract_major_version("jdk-25.0.1.jdk"), 25);
        assert_eq!(extract_major_version("jdk-21.0.5.jdk"), 21);
        assert_eq!(extract_major_version("zulu-17.jdk"), 17);
        assert_eq!(extract_major_version("nope.jdk"), 0);
    }
}
