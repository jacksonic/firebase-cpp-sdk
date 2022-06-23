#!/usr/bin/env python

# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Searches the PATH environment variable for all `patch` executables, and prints
the absolute path of the directory containing the `patch` executable with the
latest version.

This script is used as a workaround for b/236958574, where GitHub Actions on
Windows were failing because the `patch` command used when patching the Snappy
dependency was crashing.
"""

from __future__ import annotations

import functools
import os
import pathlib
import re
import shutil
import subprocess
from typing import Dict, Iterable, Optional, Sequence, Tuple

from absl import app
from absl import logging


def main(argv: Sequence[str]) -> None:
  if len(argv) > 1:
    raise app.UsageError(f"Unexpected argument: {argv[1]}")

  patch_executables = find_patch_executables()
  version_str_by_patch_executable = determine_patch_versions(patch_executables)
  patch_executable_info = find_latest_version(version_str_by_patch_executable)

  print(patch_executable_info.executable.parent)


class PatchExecutableInfo:

  def __init__(self, executable: pathlib.Path, version: str, order: int) -> None:
    self.executable = executable
    self.version = version
    self.order = order

  def version_tuple(self) -> Tuple[int, ...]:
    return tuple(
      int(version_component)
      for version_component in self.version.split(".")
    )

  def __lt__(self, other: PatchExecutableInfo) -> bool:
    self_version_tuple = self.version_tuple()
    other_version_tuple = other.version_tuple()

    if self_version_tuple == other_version_tuple:
      # Order instances by their ordering in the PATH environment variable, if
      # their versions are equal.
      return self.order < other.order
    else:
      # Intentionally invert the sort order by using > instead of < so that
      # newer versions are ordered before older versions.
      return self_version_tuple > other_version_tuple


def find_patch_executables() -> Tuple[pathlib.Path]:
  # Store the executables as keys in a dict. This achieves two goals: (1) it
  # removes duplicates and (2) keeps the keys ordered by their insertion order
  # (because dict guarantees iteration order is the same as insertion order).
  patch_executables: Dict[pathlib.Path, None] = {}

  path = os.environ.get("PATH")
  if not path:
    raise PathEnvironmentVariableNotSetError(
      "The PATH environment variable must be set")

  logging.info("Searching for patch executable in PATH: %s", path)

  for path_entry in path.split(os.pathsep):
    # Clean up garbage that might be in the PATH
    path_entry = path_entry.strip()
    if not path_entry:
      continue

    patch_path_str = shutil.which("patch", path=path_entry)
    if patch_path_str:
      patch_executables[pathlib.Path(patch_path_str)] = None

  logging.info("Found %s patch executables: %s", len(patch_executables),
    ", ".join(str(executable) for executable in patch_executables))
  if not patch_executables:
    raise NoPatchExecutableFoundError("no patch executables found")

  return tuple(patch_executables)


def determine_patch_versions(
  patch_executables: Iterable[pathlib.Path]
) -> Dict[pathlib.Path, str]:
  version_str_by_patch_executable: Dict[pathlib.Path, str] = {}
  for patch_executable in patch_executables:
    logging.info("Calculating version of %s", patch_executable)
    patch_version = determine_patch_version(patch_executable)
    if patch_version is None:
      logging.info("Unable to determine version of %s", patch_executable)
    else:
      logging.info("Version of %s is: %s", patch_executable, patch_version)
      version_str_by_patch_executable[patch_executable] = patch_version

  if not version_str_by_patch_executable:
    raise NoPatchExecutableFoundError("unable to determine the versions "
      "of the patch executables that were found")

  return version_str_by_patch_executable


def find_latest_version(
  version_str_by_patch_executable: Dict[pathlib.Path, str]
) -> PatchExecutableInfo:
  patch_executable_infos = sorted(
    PatchExecutableInfo(patch_executable, patch_version, order)
    for (order, (patch_executable, patch_version))
    in enumerate(version_str_by_patch_executable.items())
  )

  patch_executable_info = patch_executable_infos[0]
  patch_executable = patch_executable_info.executable
  patch_executable_version = patch_executable_info.version

  logging.info("Using patch executable: %s (version %s)", patch_executable,
    patch_executable_version)

  return patch_executable_info


def determine_patch_version(patch_executable: pathlib.Path) -> Optional[str]:
  args = [patch_executable, "--version"]
  logging.info("Running command: %s", subprocess.list2cmdline(args))

  output = subprocess.check_output(args, encoding="utf8", errors="replace")
  for output_line in output.splitlines():
    output_line = output_line.strip()
    match = re.search(r"patch\s+(\d+\.\d+\.\d+)$", output_line)
    if match:
      return match.group(1)

  return None


class PathEnvironmentVariableNotSetError(Exception):
  pass


class NoPatchExecutableFoundError(Exception):
  pass


if __name__ == "__main__":
  app.run(main)
