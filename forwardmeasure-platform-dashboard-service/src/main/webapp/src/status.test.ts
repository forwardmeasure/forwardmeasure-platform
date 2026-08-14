import { describe, expect, it } from "vitest";
import { summarize, titleCase } from "./status";

describe("platform status presentation", () => {
  it("separates available components from components requiring attention", () => {
    const summary = summarize([
      { id: "kafka", name: "Kafka", category: "MESSAGING", status: "UP", checkedAt: new Date() },
      { id: "search", name: "Search", category: "SEARCH", status: "DOWN", checkedAt: new Date() },
    ]);
    expect(summary).toEqual({ available: 1, attention: 1, total: 2 });
  });

  it("presents contract enums as business labels", () => {
    expect(titleCase("SCHEMA_GOVERNANCE")).toBe("Schema Governance");
  });
});
