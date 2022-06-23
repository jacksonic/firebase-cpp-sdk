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

import os
import pathlib
import re
import shutil
import subprocess
from typing import Collection, Dict, Iterator, Optional, Sequence, Tuple

from absl import app
from absl import logging


def main(argv: Sequence[str]) -> None:
  if len(argv) > 1:
    raise app.UsageError(f"Unexpected argument: {argv[1]}")

  # Map the patch executable paths to their versions; the versions are
  # initialized to None, and will be resolved later. Note that the logic below
  # uses the guaranteed ordering of dict keys so that executables found in
  # earlier PATH entries take precedence over those found in later entries.
  patch_executables: Dict[pathlib.Path, Optional[str]] = {
    patch_executable: None for patch_executable in find_patch_executables()
  }

  logging.info("Found %s patch executables: %s", len(patch_executables),
    ", ".join(str(executable) for executable in patch_executables))
  if not patch_executables:
    raise NoPatchExecutableFoundError("no patch executables found")

  for patch_executable in tuple(patch_executables.keys()):
    logging.info("Calculating version of %s", patch_executable)
    patch_version = determine_patch_version(patch_executable)
    if patch_version is None:
      logging.info("Unable to determine version of %s", patch_executable)
      del patch_executables[patch_executable]
    else:
      logging.info("Version of %s is: %s", patch_executable, patch_version)
      patch_executables[patch_executable] = patch_version

  if not patch_executables:
    raise NoPatchExecutableFoundError("unable to determine the versions "
      "of the patch executables that were found")

  # Sort the patch executable paths by version. Since list.sort() is stable, if
  # two patch executables have the same version then preference will be given to
  # the one that occurs earlier in the PATH.
  patch_executable_paths = list(patch_executables.keys())
  patch_executable_paths.sort(
    key = lambda x: sort_key_from_version(patch_executables[x]))

  patch_executable_path = patch_executable_paths[0]
  logging.info("Using patch executable: %s (version %s)", patch_executable_path,
    patch_executables[patch_executable_path])

  print(patch_executable_path)


def find_patch_executables() -> Iterator[pathlib.Path]:
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
      yield pathlib.Path(patch_path_str)


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


def sort_key_from_version(version: str) -> Tuple[int, ...]:
  return tuple(
    int(version_component)
    for version_component in version.split(".")
  )


class PathEnvironmentVariableNotSetError(Exception):
  pass


class NoPatchExecutableFoundError(Exception):
  pass


if __name__ == "__main__":
  app.run(main)
