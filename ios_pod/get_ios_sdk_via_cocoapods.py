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
Downloads the sources for the firebase-ios-sdk using CocoaPods.

Usage: %s <src_dir> <dest_dir>
"""

import pathlib
import shutil
import subprocess
from typing import Sequence

from absl import app
from absl import logging


def main(argv: Sequence[str]) -> None:
  if len(argv) < 2:
    raise app.UsageError(f"missing required argument: src_dir")
  if len(argv) < 3:
    raise app.UsageError(f"missing required argument: dest_dir")
  if len(argv) > 3:
    raise app.UsageError(f"unexpeted argument: {argv[3]}")

  src_dir = pathlib.Path(argv[1])
  dest_dir = pathlib.Path(argv[2])

  if not dest_dir.exists():
    logging.info("Creating directory: %s", dest_dir)
    dest_dir.mkdir(parents=True, exist_ok=True)

  copy_files(src_dir, dest_dir)
  generate_xcode_project(dest_dir)
  run_cocoapods(dest_dir)


def copy_files(src_dir: pathlib.Path, dest_dir: pathlib.Path) -> None:
  dest_filename_by_source_filename = {
    "Podfile": "Podfile",
    "empty.cc": "empty.cc",
    "empty_CMakeLists.txt": "CMakeLists.txt",
  }

  for (src_filename, dest_filename) in sorted(dest_filename_by_source_filename.items()):
    src_file = src_dir / src_filename
    dest_file = dest_dir / dest_filename
    logging.info("Copying %s to %s", src_file, dest_file)
    shutil.copy(src_file, dest_file)


def generate_xcode_project(project_dir: pathlib.Path) -> None:
  args = [
    "cmake",
    ".",
    "-G",
    "Xcode",
  ]
  args_str = subprocess.list2cmdline(args)

  logging.info("Generating Xcode project by running %s in %s", args_str, project_dir)
  subprocess.run(args, cwd=project_dir, check=True)


def run_cocoapods(project_dir: pathlib.Path) -> None:
  args = [
    "pod",
    "install",
  ]
  args_str = subprocess.list2cmdline(args)
  logging.info("Running %s in %s", args_str, project_dir)
  subprocess.run(args, cwd=project_dir, check=True)


if __name__ == "__main__":
  app.run(main)
