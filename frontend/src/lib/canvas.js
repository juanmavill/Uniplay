// Dibujo sobre el lienzo. Los deltas llegan normalizados entre 0 y 1.
export function drawDelta(canvas, delta) {
  if (!canvas || !delta) {
    return;
  }
  const context = canvas.getContext("2d");
  const ratio = window.devicePixelRatio || 1;
  context.save();
  context.scale(ratio, ratio);
  context.strokeStyle = delta.color || "#0f8b8d";
  context.lineWidth = delta.width || 4;
  context.lineCap = "round";
  context.lineJoin = "round";
  const rect = canvas.getBoundingClientRect();
  context.beginPath();
  context.moveTo(delta.fromX * rect.width, delta.fromY * rect.height);
  context.lineTo(delta.toX * rect.width, delta.toY * rect.height);
  context.stroke();
  context.restore();
}

export function clearCanvas(canvas) {
  if (!canvas) {
    return;
  }
  canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
}
