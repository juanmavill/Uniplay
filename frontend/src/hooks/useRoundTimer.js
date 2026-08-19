import React from "react";

export function useRoundTimer(round) {
  const [now, setNow] = React.useState(Date.now());
  React.useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);
  if (!round?.endsAt) {
    return "--:--";
  }
  const remainingSeconds = Math.max(0, Math.ceil((new Date(round.endsAt).getTime() - now) / 1000));
  return `${String(Math.floor(remainingSeconds / 60)).padStart(2, "0")}:${String(remainingSeconds % 60).padStart(2, "0")}`;
}
