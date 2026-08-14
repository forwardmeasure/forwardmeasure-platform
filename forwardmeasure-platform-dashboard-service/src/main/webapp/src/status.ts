import type { PlatformComponent } from "../generated-api/src/index";

export type PlatformSummary = {
  available: number;
  attention: number;
  total: number;
};

export function summarize(components: PlatformComponent[]): PlatformSummary {
  return components.reduce((result, component) => {
    result.total += 1;
    if (component.status === "UP") result.available += 1;
    else result.attention += 1;
    return result;
  }, { available: 0, attention: 0, total: 0 });
}

export function titleCase(value: string): string {
  return value.toLowerCase().split("_")
    .map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}
