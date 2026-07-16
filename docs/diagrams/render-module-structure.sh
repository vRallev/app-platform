#!/usr/bin/env bash

set -euo pipefail

readonly expected_d2_version="v0.7.1"
readonly d2_bin="${D2_BIN:-d2}"
readonly fira_code_dir="${FIRA_CODE_DIR:?Set FIRA_CODE_DIR to the Fira Code TTF directory.}"
readonly script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly docs_dir="$(cd "${script_dir}/.." && pwd)"
readonly source_dir="${script_dir}/module-structure"
readonly output_dir="${docs_dir}/images/module-structure"

if [[ "$("${d2_bin}" --version)" != "${expected_d2_version}" ]]; then
  echo "Expected D2 ${expected_d2_version}. Set D2_BIN to the pinned executable." >&2
  exit 1
fi

readonly font_regular="${fira_code_dir}/FiraCode-Regular.ttf"
readonly font_semibold="${fira_code_dir}/FiraCode-SemiBold.ttf"
readonly font_bold="${fira_code_dir}/FiraCode-Bold.ttf"

readonly diagrams=(
  transitive-dependency
  move-implementations
  split-implementations
  forbidden-dependency
  module-types
  full-example
)

mkdir -p "${output_dir}"

for diagram in "${diagrams[@]}"; do
  light_output="${output_dir}/${diagram}-light.svg"
  dark_output="${output_dir}/${diagram}-dark.svg"

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
    "${source_dir}/${diagram}.d2" \
    "${light_output}"

  if [[ "${diagram}" == "forbidden-dependency" ]]; then
    # D2 centers the label box on the edge, but Fira Code's X glyph sits low within that box.
    perl -0pi -e 's/(<text[^>]*)(>✕<\/text>)/$1 transform="translate(0 -5)"$2/' "${light_output}"
  fi

  if [[ "${diagram}" == "full-example" ]]; then
    perl "${script_dir}/postprocess-full-example.pl" "${light_output}"
  fi

  cp "${light_output}" "${dark_output}"
  perl -0pi -e 's/fill="#FFFFFF" class=" fill-N7" stroke-width="0"/fill="#1E2129" style="fill:#1E2129" stroke-width="0"/; s/#ECECFF/#1E2028/g; s/#333333/#E6E6E6/g; s/#222222/#E6E6E6/g; s/#D9EAD3/#26372B/g; s/#6AA84F/#8EC07C/g; s/#FCE5CD/#3C3022/g; s/#E69138/#D79921/g' "${dark_output}"
done
