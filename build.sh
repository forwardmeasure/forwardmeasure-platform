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

cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-platform

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
  --run-tests             Run tests (skipped by default).
  --coverage              Collect Jacoco code coverage via the "coverage"
                           profile (disabled by default).
  -h, --help              Show this help and exit.

<target> accepts either a path relative to reactor.xml, or Maven's
"[groupId]:artifactId" shorthand, e.g.
":decision-engine-api-language-bindings-python-grpc".

Examples:
  ./build.sh
      Full build of every module, then push all images.

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
if [ "$coverage" = true ]; then
	profiles="$profiles,coverage"
fi

case "$mode" in
	module)
		mvn -f reactor.xml -pl "$target" -am spotless:apply
		mvn -f reactor.xml -pl "$target" -am -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache=true -DskipTests="$skip_tests" clean install
		;;
	resume)
		mvn -f reactor.xml -rf "$target" spotless:apply
		mvn -f reactor.xml -rf "$target" -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache=true -DskipTests="$skip_tests" clean install
		;;
	full)
		mvn -f reactor.xml spotless:apply
		mvn -f reactor.xml -P"$profiles" -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache=true -DskipTests="$skip_tests" clean install
		;;
esac

if [ "$push" = true ]; then
	for i in $(docker images | grep forwardmeasure | awk '{ print $1 }')
	do
		docker push "$i"
	done
fi
