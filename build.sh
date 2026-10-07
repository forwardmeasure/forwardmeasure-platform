#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
set -eou pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
maven_command=("../forwardmeasure-openworkflow/scripts/build-bounded.sh" -f "${PWD}/reactor.xml")

usage() {
	cat <<'EOF'
Usage: ./build.sh [OPTIONS]

Builds the forwardmeasure-platform multi-repo reactor and pushes the
resulting container images. With no options, builds every module.

Options:
  --module <target>       Build only this module and what it depends on
                           (mvn -pl <target> -am). Use for a normal scoped
                           build of one module.
  --resume-from <target>  A previous full build failed partway through;
                           continue the full reactor build starting at
                           <target> instead of rebuilding everything from
                           scratch (mvn -rf <target>). Fix the failing
                           module first, then pass it here.
  --full                  Build every module (default).
  --skip-push             Build only; don't docker push the images.
  --no-cache              Rebuild Docker layers without using the cache
                           (Docker caching is enabled by default).
  --no-build-cache       Disable Maven build-cache reuse for this invocation.
  --run-tests             Run tests and enforce coverage (tests skipped by default).
  --coverage              Collect Jacoco code coverage via the "coverage"
                           profile (disabled by default).
  -h, --help              Show this help and exit.

<target> accepts either a path relative to reactor.xml, or Maven's
"[groupId]:artifactId" shorthand, e.g.
":decision-engine-api-language-bindings-python-grpc".

Examples:
  ./build.sh
      Full build of every module, then push images from the selected reactor.

  ./build.sh --module :decision-engine-api-language-bindings-python-grpc
      Build only that module and its dependencies, then push.

  ./build.sh --resume-from :decision-engine-api-language-bindings-python-grpc
      A full build failed at that module. After fixing it, continue the
      full reactor build from there rather than starting over.

  ./build.sh --run-tests --coverage
      Full build with tests actually run and Jacoco coverage collected.
EOF
}

mode=full
target=
push=true
skip_tests=true
coverage=false
no_cache=false
build_cache=true

while [ $# -gt 0 ]; do
	case "$1" in
		--module)
			[ -n "${2:-}" ] || { echo "--module requires a value" >&2; exit 1; }
			mode=module
			target="$2"
			shift 2
			;;
		--resume-from)
			[ -n "${2:-}" ] || { echo "--resume-from requires a value" >&2; exit 1; }
			mode=resume
			target="$2"
			shift 2
			;;
		--full)
			mode=full
			shift
			;;
		--skip-push)
			push=false
			shift
			;;
		--run-tests)
			skip_tests=false
			shift
			;;
		--no-cache)
			no_cache=true
			shift
			;;
		--no-build-cache)
			build_cache=false
			shift
			;;
		--coverage)
			coverage=true
			shift
			;;
		-h|--help)
			usage
			exit 0
			;;
		*)
			echo "Unknown argument: $1" >&2
			usage >&2
			exit 1
			;;
	esac
done

profiles=container-image
if [ "$coverage" = true ] || [ "$skip_tests" = false ]; then
	profiles="$profiles,coverage"
fi

case "$mode" in
	module)
		"${maven_command[@]}" -pl "$target" -am spotless:apply
		"${maven_command[@]}" -pl "$target" -am -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache="$no_cache" -Dmaven.build.cache.enabled="$build_cache" -Dmaven.test.skip=false -DskipTests="$skip_tests" clean install
		;;
	resume)
		"${maven_command[@]}" -rf "$target" spotless:apply
		"${maven_command[@]}" -rf "$target" -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache="$no_cache" -Dmaven.build.cache.enabled="$build_cache" -Dmaven.test.skip=false -DskipTests="$skip_tests" clean install
		;;
	full)
		"${maven_command[@]}" spotless:apply
		"${maven_command[@]}" -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache="$no_cache" -Dmaven.build.cache.enabled="$build_cache" -Dmaven.test.skip=false -DskipTests="$skip_tests" clean install
		;;
esac

# Container publication is owned by the selected Maven modules; never push unrelated local images.
