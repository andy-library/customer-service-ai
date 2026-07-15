#!/usr/bin/env bash
# Install andy-library/microservice-framework into the local Maven repository.
# Safe to re-run. Also patches starter-parent BOM import versions (see note below).
set -euo pipefail

FRAMEWORK_VERSION="${FRAMEWORK_VERSION:-1.0.0-alpha.1}"
CLONE_DIR="${FRAMEWORK_CLONE_DIR:-/tmp/microservice-framework}"
REPO_URL="${FRAMEWORK_REPO_URL:-https://github.com/andy-library/microservice-framework.git}"
M2_MARKER="${HOME}/.m2/repository/com/microservice/framework/microservice-framework-starter-parent/${FRAMEWORK_VERSION}/microservice-framework-starter-parent-${FRAMEWORK_VERSION}.pom"

install_parent_and_starters() {
  if [[ ! -d "${CLONE_DIR}/.git" ]]; then
    echo "Cloning ${REPO_URL} into ${CLONE_DIR} ..."
    if ! git clone --depth 1 --branch "v${FRAMEWORK_VERSION}" "${REPO_URL}" "${CLONE_DIR}" 2>/dev/null; then
      git clone --depth 1 "${REPO_URL}" "${CLONE_DIR}"
    fi
  fi

  PARENT_DIR="${CLONE_DIR}/SourceCode/microservice-framework-parent"
  if [[ ! -f "${PARENT_DIR}/pom.xml" ]]; then
    echo "ERROR: parent POM not found at ${PARENT_DIR}/pom.xml" >&2
    exit 1
  fi

  # Skip unit + invoker ITs: we only need artifacts in local m2 for consumer builds (CI/dev).
  local mvn_install=(mvn clean install -DskipTests -Dinvoker.skip=true -B)

  echo "Installing microservice-framework parent (${FRAMEWORK_VERSION}) ..."
  (cd "${PARENT_DIR}" && "${mvn_install[@]}")

  SOURCE_ROOT="${CLONE_DIR}/SourceCode"
  for starter in \
    microservice-framework-common-starter \
    microservice-framework-json-starter \
    microservice-framework-web-starter \
    microservice-framework-logging-starter \
    microservice-framework-observability-starter \
    microservice-framework-async-starter \
    microservice-framework-database-starter \
    microservice-framework-security-starter
  do
    if [[ -f "${SOURCE_ROOT}/${starter}/pom.xml" ]]; then
      echo "Installing ${starter} ..."
      (cd "${SOURCE_ROOT}/${starter}" && "${mvn_install[@]}")
    fi
  done
}

# Note: starter-parent uses ${project.version} for BOM imports. Consumer apps
# with a different version (e.g. 0.1.0-SNAPSHOT) make Maven resolve BOMs to the
# *consumer* version and fail. Pin imports to FRAMEWORK_VERSION after install.
patch_starter_parent_bom_imports() {
  python3 - "$FRAMEWORK_VERSION" <<'PY'
import sys
from pathlib import Path

ver = sys.argv[1]
p = Path.home() / (
    f".m2/repository/com/microservice/framework/"
    f"microservice-framework-starter-parent/{ver}/"
    f"microservice-framework-starter-parent-{ver}.pom"
)
if not p.exists():
    print(f"WARN: {p} missing; skip patch", file=sys.stderr)
    raise SystemExit(1)
text = p.read_text()
old = """        <version>${project.version}</version>
        <type>pom</type>
        <scope>import</scope>"""
new = f"""        <version>{ver}</version>
        <type>pom</type>
        <scope>import</scope>"""
if old in text:
    p.write_text(text.replace(old, new))
    print(f"Patched starter-parent BOM imports -> {ver}")
else:
    print("starter-parent BOM imports already concrete (or format differs)")
PY
}

FORCE_REINSTALL="${FORCE_REINSTALL:-0}"
if [[ "${FORCE_REINSTALL}" == "1" ]] || [[ ! -f "${M2_MARKER}" ]]; then
  install_parent_and_starters
else
  echo "Framework ${FRAMEWORK_VERSION} already present in local m2."
fi

patch_starter_parent_bom_imports
echo "Ready: microservice-framework ${FRAMEWORK_VERSION}"
