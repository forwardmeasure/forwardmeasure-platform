import "@fontsource-variable/inter";
import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { initialize } from "./runtime";
import "./styles.css";

const root = ReactDOM.createRoot(document.getElementById("root")!);
root.render(<div className="boot">Opening The Platform Dashboard…</div>);

initialize()
  .then(identity => root.render(<React.StrictMode><App identity={identity}/></React.StrictMode>))
  .catch(failure => root.render(<main className="fatal" role="alert"><p>Platform Dashboard</p><h1>Unable To Start This View</h1><pre>{failure instanceof Error ? failure.message : String(failure)}</pre></main>));
