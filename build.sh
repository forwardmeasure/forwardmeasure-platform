#!/bin/bash
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
EOF
}

mode=full
target=
push=true

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

case "$mode" in
	module)
		mvn -f reactor.xml -pl "$target" -am spotless:apply
		mvn -T1C -f reactor.xml -pl "$target" -am -Pcontainer-image -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache=true -DskipTests clean install
		;;
	resume)
		mvn -f reactor.xml -rf "$target" spotless:apply
		mvn -T1C -f reactor.xml -rf "$target" -Pcontainer-image -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache=true -DskipTests clean install
		;;
	full)
		mvn -f reactor.xml spotless:apply
		mvn -T1C -f reactor.xml -Pcontainer-image -Drat.skip=true -Dcontainer-image.push="$push" -Ddocker.nocache=true -DskipTests clean install
		;;
esac

if [ "$push" = true ]; then
	for i in $(docker images | grep forwardmeasure | awk '{ print $1 }')
	do
		docker push "$i"
	done
fi
