import React from "react";

export function PanelTitle({ icon, title, action }) {
  return (
    <div className="panel-title">
      <div>{icon}<h2>{title}</h2></div>
      {action}
    </div>
  );
}
