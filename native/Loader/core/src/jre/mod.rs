//! JRE discovery. Shared entry point; per-OS modules handle the search.

use anyhow::{Context, Result, bail};
use log::debug;
use std::env;
use std::path::{Path, PathBuf};

pub const MIN_JAVA_VERSION: u32 = 25;
pub const ENV_VAR: &str = "CONSULO_JRE";

#[cfg(target_os = "linux")]
pub mod linux;
#[cfg(target_os = "macos")]
pub mod macos;
#[cfg(target_os = "windows")]
pub mod windows;

/// Locate a usable JRE. Returns the JRE home (the directory whose `bin/java`
/// is the launcher — `Contents/Home` on macOS).
///
/// Lookup order (first match wins):
///   1. `$CONSULO_JRE`
///   2. `<build>/jre` (bundled)
///   3. `$JAVA_HOME`
///   4. Per-OS system search (skipped on loongarch64 — bundled JDK only)
pub fn locate(build_dir: &Path) -> Result<PathBuf> {
    debug!("[JRE] {ENV_VAR}?");
    if let Ok(p) = from_env(ENV_VAR) {
        return Ok(p);
    }

    let bundled = build_dir.join("jre");
    debug!("[JRE] bundled at {bundled:?}?");
    if let Ok(p) = validate_runtime_dir(&bundled) {
        return Ok(p);
    }

    debug!("[JRE] JAVA_HOME?");
    if let Ok(p) = from_env("JAVA_HOME") {
        return Ok(p);
    }

    // loongarch64: system probing is not wanted — we ship our own JDK.
    #[cfg(target_arch = "loongarch64")]
    {
        bail!(
            "no JRE: set {ENV_VAR} or place the bundled JRE at {}",
            bundled.display()
        )
    }

    #[cfg(not(target_arch = "loongarch64"))]
    {
        debug!("[JRE] system search");
        #[cfg(target_os = "windows")]
        {
            if let Ok(p) = windows::search() {
                return Ok(p);
            }
        }
        #[cfg(target_os = "macos")]
        {
            if let Ok(p) = macos::search() {
                return Ok(p);
            }
        }
        #[cfg(target_os = "linux")]
        {
            if let Ok(p) = linux::search() {
                return Ok(p);
            }
        }
        bail!(
            "no JRE found. Set {ENV_VAR} or install a JDK ≥ {MIN_JAVA_VERSION}.",
        )
    }
}

fn from_env(var: &str) -> Result<PathBuf> {
    let val = env::var(var).with_context(|| format!("{var} not set"))?;
    validate_runtime_dir(Path::new(&val))
}

/// Check that `<root>/bin/java[.exe]` exists and is executable.
/// Returns the adjusted home (`Contents/Home` appended on macOS when applicable).
pub fn validate_runtime_dir(root: &Path) -> Result<PathBuf> {
    let adjusted = if cfg!(target_os = "macos") && root.join("Contents/Home").is_dir() {
        root.join("Contents/Home")
    } else {
        root.to_path_buf()
    };
    let java = adjusted.join(if cfg!(target_os = "windows") {
        "bin\\java.exe"
    } else {
        "bin/java"
    });
    if !java.is_file() {
        bail!("java not found at {java:?}");
    }
    Ok(adjusted)
}
