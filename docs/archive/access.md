| Login | Username | Realm |
|---|---|---|
| Keycloak administration | **`admin`** | `master` |
| FOWF Studio | **`platform-admin`** | `forwardmeasure` |
| FEI Studio | **`platform-admin`** | `forwardmeasure` |

FEI Studio: [https://lux.kriyagentic.com/ei/studio/](https://lux.kriyagentic.com/ei/studio/).
Other configured tenants use `https://<tenant-host>/ei/studio/`. The tenant dashboard links directly
there; `/ei`, `/ei/`, and `/ei/studio` redirect to the trailing-slash URL on the same host.
Sign in with `platform-admin`, using the same account and password as FOWF Studio.
The shared Deployment and Service are `entity-intelligence-studio-service` in namespace
`entity-intelligence`; the browser-facing hostname remains tenant-specific.

Deployment correction (2026-10-06): this supersedes the dedicated-product-hostname change.
Rebuild/push only the selected FEI Studio image (`entity-intelligence-studio-quarkus` on this cluster),
then rerun the platform installer. Studio API URLs now preserve the public gateway prefix.
Tenant certificates and Keycloak tenant redirect URIs already cover this URL; no chart publication
is required. Routing under a tenant hostname does not replace the APIs' JWT organization checks.

Keycloak admin:
kubectl -n keycloak get secret keycloak-admin-credentials \
  -o jsonpath='{.data.KEYCLOAK_ADMIN_PASSWORD}' | base64 --decode; echo

  FOWF and FEI Studio account:

kubectl -n keycloak get secret keycloak-bootstrap-admin-user-credentials \
  -o jsonpath='{.data.password}' | base64 --decode; echo
