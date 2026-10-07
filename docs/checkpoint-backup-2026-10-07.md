# Work-in-progress recovery checkpoint — 2026-10-07

User authorized checkpoint commits and pushes across repositories. These preserve existing mixed work; validation remains incomplete. Remote shared branches were not advanced.

Backup branch: `backup/ingestion-repair-20261007-181917`

| Repository | Commit | Remote backup |
|---|---|---|
| data-fabric | `456eab343857f0e666802df2ea9cb6302308138e` | verified |
| forwardmeasure-agent-os | `0c1e0999892b76bda32906752b89aee716a40b20` | verified |
| forwardmeasure-authzen | `472306ad92b0285c8495a91b7e69b76180e9e63a` | verified |
| forwardmeasure-data-streaming | `a957b0fb04748d8b46cf6bb6e8079075daa462d5` | verified |
| forwardmeasure-database-migrations | `b15abe2cf365915cf77d2b5ab77e7bb2c064ccbc` | verified |
| forwardmeasure-decision-engine | `f54374c6aad5f302a22b0fbc95176edf72c536f1` | verified |
| forwardmeasure-entity-intelligence | `6d12a166050e87666e800976043f6e23293c139a` | verified |
| forwardmeasure-entity-matching | `32c80e9f23646ad3a7deefa28a51f82701db86a2` | verified |
| forwardmeasure-jpa | `05287a62c8e4747f398f2ef4facea8160854b681` | verified |
| forwardmeasure-nlp | `3da2517a804631644c7fc584da030971b8a86dfe` | verified |
| forwardmeasure-object-storage | `e4c7e40760a7389c39719d7edb675d94dfaeb565` | verified |
| forwardmeasure-openworkflow | `eb3cfb3d0b7a8e5fc3ea42e0dc40a33eef3cf373` | verified |
| forwardmeasure-platform | `1bb1e00db12edb3dd47f532878fd4404a30d9d60` | verified |
| forwardmeasure-platform-operations | `f77a25e315177d398bae4711cc82a0d1b7a37e84` | verified |
| forwardmeasure-testcontainers | `88cfd1a7bfb5a3ef4ccbd6002885bad2a88c7e4b` | verified |
| helm-charts | `00b97ff22b35a70035a07e8211f0afedba3d4cda` | verified |
| livy | `e3807b25ac0a12eb9c88ca1941712d5d373e8485` | failed |
| openworkflow-k8s-setup | `b3117b1f8241dc38097ce1be4b3bc381bd69e491` | verified |

Excluded: separate local `env` shell-settings repository (not inspected for credentials), ignored files/build outputs, and two untracked `data-fabric/.vite/deps` cache files. The unchanged openworkflow-k8s-setup HEAD was also backed up.

Livy has a local checkpoint; its remote push failed because HTTPS credentials are unavailable. The SSH fallback confirms that the existing remote is `apache/livy` and the authenticated `forwardmeasure` account lacks push permission. A writable Livy fork is needed to push this checkpoint.

Restore by cloning each existing remote and checking out the backup branch. Use the commit table for the exact cross-repository snapshot. Worktree branches remain their original branches; subsequent repairs will be newer than this checkpoint.

This manifest is committed separately in forwardmeasure-platform on the same backup branch; its row above identifies the production/test snapshot before adding this manifest.
