import { useEffect, useState } from "react";
import type { PlatformComponent } from "../generated-api/src/index";
import {
  Activity,
  Boxes,
  Database,
  ExternalLink,
  LayoutGrid,
  LogOut,
  Moon,
  RefreshCw,
  ShieldCheck,
  Sun,
} from "lucide-react";
import type { PlatformIdentity } from "./runtime";
import { summarize, titleCase } from "./status";

type ThemePreference = "light" | "dark" | "system";

const THEME_STORAGE_KEY = "forwardmeasure-theme";

function storedThemePreference(): ThemePreference {
  const value = localStorage.getItem(THEME_STORAGE_KEY);
  return value === "light" || value === "dark" || value === "system"
    ? value
    : "system";
}

function systemPrefersDark(): boolean {
  return typeof window.matchMedia === "function"
    && window.matchMedia("(prefers-color-scheme: dark)").matches;
}

export function App({ identity }: { identity: PlatformIdentity }) {
  const [components, setComponents] = useState<PlatformComponent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [themePreference, setThemePreference] = useState<ThemePreference>(storedThemePreference);
  const [systemDark, setSystemDark] = useState(systemPrefersDark);
  const theme = themePreference === "system"
    ? (systemDark ? "dark" : "light")
    : themePreference;

  async function refresh() {
    setLoading(true);
    setError(undefined);
    try {
      setComponents(await identity.api.listPlatformComponents());
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : String(failure));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void refresh(); }, []);
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem(THEME_STORAGE_KEY, themePreference);
  }, [theme, themePreference]);
  useEffect(() => {
    if (typeof window.matchMedia !== "function") return;
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const update = (event: MediaQueryListEvent) => setSystemDark(event.matches);
    media.addEventListener?.("change", update);
    return () => media.removeEventListener?.("change", update);
  }, []);
  const summary = summarize(components);
  return <div className="platform-shell">
    <aside className="platform-nav">
      <a className="brand" href="./"><span className="brand-mark"><Boxes size={19}/></span><span>ForwardMeasure</span></a>
      <nav aria-label="Platform Navigation">
        <a aria-current="page" href="#overview"><Activity size={18}/>Overview</a>
        <a href="#applications"><LayoutGrid size={18}/>Applications</a>
        <a href="#services"><Database size={18}/>Services</a>
      </nav>
      <div className="identity">
        <strong>{identity.displayName}</strong>
        <span>{identity.tenantName}</span>
        <button type="button" onClick={() => void identity.keycloak.logout({ redirectUri: window.location.origin })}>
          <LogOut size={15}/>Sign Out
        </button>
      </div>
    </aside>
    <main>
      <header className="topbar">
        <div><span className="environment">{identity.environment}</span><span className="session"><ShieldCheck size={15}/>Authenticated Platform View</span></div>
        <div className="topbar-controls">
          <button className="theme-toggle"
            onClick={() => setThemePreference(theme === "light" ? "dark" : "light")}
            aria-label={`Use ${theme === "light" ? "dark" : "light"} theme`}
            type="button">
            {theme === "light" ? <Moon size={16}/> : <Sun size={16}/>}
          </button>
          <button className="refresh" disabled={loading} onClick={() => void refresh()} type="button"><RefreshCw className={loading ? "spinning" : ""} size={16}/>Refresh</button>
        </div>
      </header>
      <section className="hero" id="overview">
        <p className="eyebrow">Shared Platform</p>
        <h1>Operational Clarity Across The Stack.</h1>
        <p>Identity, messaging, schemas, search, analytics and model services—observed through one authenticated, read-only control surface.</p>
      </section>
      {error && <section className="error" role="alert"><strong>Unable To Load Platform Status</strong><span>{error}</span></section>}
      <section className="metrics" aria-label="Platform Summary">
        <article><span>Available</span><strong>{summary.available}</strong><small>Healthy components</small></article>
        <article><span>Needs Attention</span><strong>{summary.attention}</strong><small>Degraded, down or unknown</small></article>
        <article><span>Observed</span><strong>{summary.total}</strong><small>Configured platform components</small></article>
      </section>
      <section className="application-section" id="applications">
        <div className="section-heading"><div><p className="eyebrow">Tenant Applications</p><h2>Continue Your Work</h2></div><span>{identity.applications.length} Applications</span></div>
        {identity.applications.length === 0
          ? <div className="empty">No tenant applications have been configured.</div>
          : <div className="application-grid">{identity.applications.map(application =>
            <a className="application-card" href={application.href} key={application.id}>
              <span><Boxes size={18}/></span>
              <div><h3>{application.name}</h3><p>{application.description}</p></div>
              <ExternalLink size={16}/>
            </a>)}</div>}
      </section>
      <section className="service-section" id="services">
        <div className="section-heading"><div><p className="eyebrow">Live Inventory</p><h2>Platform Services</h2></div><span>{loading ? "Refreshing…" : `${components.length} Components`}</span></div>
        {loading && components.length === 0
          ? <div className="empty">Loading authenticated platform observations…</div>
          : components.length === 0
            ? <div className="empty">No platform components have been configured.</div>
            : <div className="service-grid">{components.map(component => <ComponentCard key={component.id} value={component}/>)}</div>}
      </section>
    </main>
  </div>;
}

function ComponentCard({ value }: { value: PlatformComponent }) {
  return <article className="service-card">
    <div className="card-heading"><span className={`status-dot status-${value.status.toLowerCase()}`}/><span>{titleCase(value.category)}</span></div>
    <h3>{value.name}</h3>
    <p>{value.detail ?? "No health detail was supplied."}</p>
    <div className="card-footer">
      <span>{value.latencyMilliseconds === undefined ? "—" : `${value.latencyMilliseconds} ms`}</span>
      <span>{value.checkedAt.toLocaleTimeString()}</span>
      {value.consoleUrl && <a href={value.consoleUrl} target="_blank" rel="noreferrer">Open <ExternalLink size={14}/></a>}
    </div>
  </article>;
}
