import React from "react";

export function StatusLine({ label, value, tone = "muted" }) {
  return (
    <div className="status-line">
      <span>{label}</span>
      <strong className={tone}>{value}</strong>
    </div>
  );
}
