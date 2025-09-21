import re
import sys
import os

DEFAULT_FILE_PATH = '../../VERSION.md'

def parse_semantic_version(version_string):
  # Define a regular expression for semantic versioning
  pattern = re.compile(r'^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)(?:-(?P<prerelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?P<buildmetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$')

  # Try to match the version string with the pattern
  match = pattern.match(version_string)

  # If there is no match, raise an error
  if not match:
      raise ValueError("Invalid semantic version format")

  # Extract major, minor, patch, and pre-release versions from the match
  major = match.group('major')
  minor = match.group('minor')
  patch = match.group('patch')
  prerelease = match.group('prerelease')

  # Return the parsed version as a tuple
  return major, minor, patch, prerelease

def set_output(name, value):
  with open(os.environ['GITHUB_OUTPUT'], 'a') as fh:
    print(f'{name}={value}', file=fh)

filepath = DEFAULT_FILE_PATH

# Check if a filename is provided as a command-line argument
if len(sys.argv) == 2:
  filepath = sys.argv[1]

# Read version from the specified file
try:
  with open(filepath, "r") as file:
      version_string = file.read().strip()
except FileNotFoundError:
  print(f"Error: File '{filepath}' not found")
  sys.exit(1)

# Example usage
try:
  major, minor, patch, prerelease = parse_semantic_version(version_string)
  is_prerelease = 'true' if prerelease else 'false'
  is_patch = 'false' if patch == '0' else 'true'
  print(f"Version: v{major}.{minor}.{patch}-{prerelease}")
  set_output("version-number", f"v{major}.{minor}.{patch}")
  set_output("raw-version-number", f"{major}.{minor}.{patch}")
  set_output("major-version", f"v{major}")
  set_output("is-prerelease", is_prerelease)
  set_output("is-patch", is_patch)
except ValueError as e:
  print(f"Error: {e}")
  sys.exit(1)