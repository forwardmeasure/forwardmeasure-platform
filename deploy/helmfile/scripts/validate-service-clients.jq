(.tenants | type == "array" and length > 0)
  and (.platform.identity.serviceClients | type == "object" and length > 0)
  and (.platform.identity.serviceClients | all(.[]; .enabled | type == "boolean"))
  and (.platform.identity.serviceClients | [.[] | select(.enabled)] | . as $clients |
    (length > 0) and (map(.clientId) | length == (unique | length))
    and (map(.secretKey) | length == (unique | length))
    and all(.[];
      (.name | type == "string" and length > 0)
      and (.clientId | type == "string" and test("^[A-Za-z0-9._-]+$"))
      and (.secretKey | type == "string" and test("^[A-Z0-9_]+$")
        and . != "FORWARDMEASURE_ADMIN_CONFIDENTIAL_CLIENT_SECRET")
      and (.secretRemoteKey | type == "string" and length > 0)
      and (.authorizationServicesEnabled | type == "boolean")
      and (.organizationClaim | type == "boolean")))
