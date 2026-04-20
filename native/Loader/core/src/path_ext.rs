//! Path utilities. `strip_ns_prefix` on Windows is load-bearing for JVM
//! and classloader path handling (see PLAN.md § Platform Hacks).

use anyhow::{Result, anyhow};
use std::path::{Path, PathBuf};

pub trait PathExt {
    fn parent_or_err(&self) -> Result<PathBuf>;
    fn to_string_checked(&self) -> Result<String>;
    fn is_executable(&self) -> Result<bool>;
    /// Strips `\\?\` and `\\?\UNC\` namespace prefixes on Windows.
    /// No-op on Unix.
    fn strip_ns_prefix(&self) -> Result<PathBuf>;
}

impl PathExt for Path {
    fn parent_or_err(&self) -> Result<PathBuf> {
        self.parent()
            .map(|p| p.to_path_buf())
            .ok_or_else(|| anyhow!("no parent directory for {self:?}"))
    }

    fn to_string_checked(&self) -> Result<String> {
        self.to_str()
            .map(String::from)
            .ok_or_else(|| anyhow!("non-UTF-8 path: {self:?}"))
    }

    #[cfg(target_os = "windows")]
    fn is_executable(&self) -> Result<bool> {
        Ok(self.is_file())
    }

    #[cfg(target_family = "unix")]
    fn is_executable(&self) -> Result<bool> {
        use std::os::unix::fs::PermissionsExt;
        let md = self.metadata()?;
        Ok(md.is_file() && md.permissions().mode() & 0o111 != 0)
    }

    #[cfg(target_os = "windows")]
    fn strip_ns_prefix(&self) -> Result<PathBuf> {
        let s = self.to_string_checked()?;
        Ok(if let Some(tail) = s.strip_prefix("\\\\?\\UNC\\") {
            PathBuf::from(format!("\\\\{tail}"))
        } else if let Some(tail) = s.strip_prefix("\\\\?\\") {
            PathBuf::from(tail)
        } else {
            self.to_path_buf()
        })
    }

    #[cfg(target_family = "unix")]
    fn strip_ns_prefix(&self) -> Result<PathBuf> {
        Ok(self.to_path_buf())
    }
}
