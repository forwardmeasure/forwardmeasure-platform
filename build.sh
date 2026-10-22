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
  --only <targets>        Build exactly these modules (comma-separated) and
                           nothing upstream of them (mvn -pl <targets>). Their
                           dependencies are taken from the local repository, so
                           they must have been installed by an earlier build.
                           Use to redeploy one app after changing only its own
                           modules.
  --resume-from <target>  A previous full build failed partway through;
                           continue the full reactor build starting at
                           <target> instead of rebuilding everything from
                           scratch (mvn -rf <target>). Fix the failing
                           module first, then pass it here.
  --full                  Build every module (default).
  --threads <count>       Use this many Maven reactor threads (positive integer).
  --skip-push             Build only; don't docker push the images.
  --no-cache              Rebuild Docker layers without using the cache
                           (Docker caching is enabled by default).
  --no-build-cache       Disable Maven build-cache reuse for this invocation.
  --run-tests             Run unit tests and enforce coverage (tests skipped by default).
                           Integration tests (*IT) do not run; use --full-suite.
  --full-suite            Run unit and integration tests (the "full-suite" profile).
                           Implies --run-tests.
                           Both need DOCKER_HUB_USERNAME and DOCKER_HUB_TOKEN set
                           (a test pulls a real private image).
  --with-itineris         Also build forwardmeasure-itineris (the reactor's itineris profile);
                           off by default because most builds do not touch it.
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

  ./build.sh --only :forwardmeasure-entity-intelligence-studio-webapp,:forwardmeasure-entity-intelligence-studio-quarkus-service
      Rebuild only FEI Studio's webapp and its Quarkus image, then push.

  ./build.sh --resume-from :decision-engine-api-language-bindings-python-grpc
      A full build failed at that module. After fixing it, continue the
      full reactor build from there rather than starting over.

  ./build.sh --run-tests --coverage
      Full build with tests actually run and Jacoco coverage collected.

  ./build.sh --threads 2
      Build with two Maven reactor threads, then push images.
EOF
}

mode=full
target=
push=true
skip_tests=true
full_suite=false
coverage=false
with_itineris=false
no_cache=false
build_cache=true
threads=

while [ $# -gt 0 ]; do
	case "$1" in
		--module)
			[ -n "${2:-}" ] || { echo "--module requires a value" >&2; exit 1; }
			mode=module
			target="$2"
			shift 2
			;;
		--only)
			[ -n "${2:-}" ] || { echo "--only requires a value" >&2; exit 1; }
			mode=only
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
		--threads)
			[[ "${2:-}" =~ ^[1-9][0-9]*$ ]] || { echo "--threads requires a positive integer" >&2; exit 1; }
			threads="$2"
			shift 2
			;;
		--skip-push)
			push=false
			shift
			;;
		--run-tests)
			skip_tests=false
			shift
			;;
		--full-suite)
			skip_tests=false
			full_suite=true
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
		--with-itineris)
			with_itineris=true
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

# Test runs pull a real private image (fowf's KubernetesJobLifecycleTest proves image pull
# secrets against Docker Hub); refuse to start a long build that is bound to fail at that test.
if [ "$skip_tests" = false ]; then
	for credential in DOCKER_HUB_USERNAME DOCKER_HUB_TOKEN; do
		[ -n "${!credential:-}" ] || {
			echo "$credential must be set to run tests (Docker Hub credentials for the private-image pull test)" >&2
			exit 1
		}
	done
fi

if [ -n "$threads" ]; then
	# Pass -T directly for launchers without MAVEN_ARGS support. Maven 3 takes the first -T,
	# so prepend the same value to MAVEN_ARGS to override any inherited thread setting.
	export MAVEN_ARGS="-T $threads${MAVEN_ARGS:+ $MAVEN_ARGS}"
	maven_command+=(-T "$threads")
	printf 'Maven reactor threads: %s\n' "$threads"
fi

profiles=container-image
if [ "$coverage" = true ] || [ "$skip_tests" = false ]; then
	profiles="$profiles,coverage"
fi
if [ "$full_suite" = true ]; then
	profiles="$profiles,full-suite"
fi
if [ "$with_itineris" = true ]; then
	profiles="$profiles,itineris"
fi

build_started=$(date +%s)

case "$mode" in
	module)
		"${maven_command[@]}" -pl "$target" -am spotless:apply
		"${maven_command[@]}" -pl "$target" -am -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache="$no_cache" -Dmaven.build.cache.enabled="$build_cache" -Dmaven.test.skip=false -DskipTests="$skip_tests" clean install
		;;
	only)
		"${maven_command[@]}" -pl "$target" spotless:apply
		"${maven_command[@]}" -pl "$target" -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache="$no_cache" -Dmaven.build.cache.enabled="$build_cache" -Dmaven.test.skip=false -DskipTests="$skip_tests" clean install
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
# Every image this build created must have reached the registry: a module that builds an image
# without a push step would otherwise be skipped silently, and no deploy would ever pick it up.
if [ "$push" = true ]; then
	python3 scripts/verify-pushed-images.py "$build_started"
fi
