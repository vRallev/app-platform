#!/usr/bin/env bash

set -euo pipefail

readonly expected_d2_version="v0.7.1"
readonly d2_bin="${D2_BIN:-d2}"
readonly fira_code_dir="${FIRA_CODE_DIR:?Set FIRA_CODE_DIR to the Fira Code TTF directory.}"
readonly script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly docs_dir="$(cd "${script_dir}/.." && pwd)"
readonly source_dir="${script_dir}/presenter"
readonly output_dir="${docs_dir}/images/presenter"

if [[ "$("${d2_bin}" --version)" != "${expected_d2_version}" ]]; then
  echo "Expected D2 ${expected_d2_version}. Set D2_BIN to the pinned executable." >&2
  exit 1
fi

readonly font_regular="${fira_code_dir}/FiraCode-Regular.ttf"
readonly font_semibold="${fira_code_dir}/FiraCode-SemiBold.ttf"
readonly font_bold="${fira_code_dir}/FiraCode-Bold.ttf"
readonly light_output="${output_dir}/model-driven-navigation-light.svg"
readonly dark_output="${output_dir}/model-driven-navigation-dark.svg"

mkdir -p "${output_dir}"

"${d2_bin}" \
  --layout=elk \
  --pad=16 \
  --theme=0 \
  --omit-version \
  --font-regular "${font_regular}" \
  --font-italic "${font_regular}" \
  --font-bold "${font_bold}" \
  --font-semibold "${font_semibold}" \
  --font-mono "${font_regular}" \
  --font-mono-italic "${font_regular}" \
  --font-mono-bold "${font_bold}" \
  --font-mono-semibold "${font_semibold}" \
  "${source_dir}/model-driven-navigation.d2" \
  "${light_output}"

cp "${light_output}" "${dark_output}"
perl -0pi -e 's/fill="#FFFFFF" class=" fill-N7" stroke-width="0"/fill="#1E2129" style="fill:#1E2129" stroke-width="0"/; s/#ECECFF/#1E2028/g; s/#333333/#E6E6E6/g; s/#222222/#E6E6E6/g; s/#6AA84F/#8EC07C/g' "${dark_output}"
