# Architecture

`forwardmeasure-platform` has three responsibilities:

1. Aggregate independently owned sibling reactors for a clean, end-to-end build.
2. Publish the compatibility BOM representing one tested set of artifacts.
3. Bind a platform release to immutable source revisions.

It intentionally does not provide a shared Maven parent. Build plugins and
framework choices remain owned by each component. A successful isolated build
proves a component is internally sound; a successful platform build proves the
selected components are mutually compatible.

Object storage is an independently releasable foundation in the train. Its
framework-neutral API and cloud providers precede OKS and Entity Intelligence
in the aggregate reactor so consumers resolve the exact source-built artifacts.

ForwardMeasure Agents follows OKS in the reactor because an immutable agent
release binds to one exact admitted OKS workflow release and invokes that same
execution model. Entity Intelligence follows both so domain applications can
consume governed agent and workflow clients without copying either contract.

The source manifest supports `WORKTREE` only while assembling a development
train. A release requires exact commits and clean repositories. This prevents a
platform version from describing unrepeatable local state.

API specifications and generated clients remain owned by their service
repositories. The platform reactor verifies that consumers compile against the
source-built client artifacts; it does not copy or republish their contracts.

The cross-product UI composition and ownership boundaries are defined in
[`ui-architecture.md`](ui-architecture.md).
