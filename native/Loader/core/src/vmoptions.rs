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
//! .vmoptions file parser. One option per line; `#` comments; trim; skip empty.

use anyhow::{Context, Result};
use log::debug;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::path::Path;

/// Read a vmoptions file, returning one option per line.
/// Missing file → empty vec (not an error; matches current code behavior).
pub fn read(path: &Path) -> Result<Vec<String>> {
    let f = match File::open(path) {
        Ok(f) => f,
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => {
            debug!("vmoptions not found: {path:?}");
            return Ok(vec![]);
        }
        Err(e) => {
            return Err(e).with_context(|| format!("cannot open vmoptions {path:?}"));
        }
    };

    let mut out = Vec::with_capacity(32);
    for line in BufReader::new(f).lines() {
        let line = line.with_context(|| format!("reading {path:?}"))?;
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('#') {
            continue;
        }
        out.push(trimmed.to_string());
    }
    debug!("vmoptions from {path:?}: {} line(s)", out.len());
    Ok(out)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    #[test]
    fn parses_comments_and_blanks() {
        let mut f = tempfile::NamedTempFile::new().unwrap();
        writeln!(f, "# comment").unwrap();
        writeln!(f, "").unwrap();
        writeln!(f, "  -Xmx1g  ").unwrap();
        writeln!(f, "-Dfoo=bar").unwrap();
        let opts = read(f.path()).unwrap();
        assert_eq!(opts, vec!["-Xmx1g".to_string(), "-Dfoo=bar".to_string()]);
    }

    #[test]
    fn missing_file_ok() {
        let opts = read(Path::new("/nonexistent/path/does.not.exist")).unwrap();
        assert!(opts.is_empty());
    }
}
