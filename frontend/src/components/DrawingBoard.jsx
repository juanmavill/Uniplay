import React from "react";
import { Brush, Eraser } from "lucide-react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { clearCanvas, drawDelta } from "../lib/canvas.js";
import { PanelTitle } from "./PanelTitle.jsx";

export function DrawingBoard({ roomCode, playerId, gatewayBase, canDraw }) {
  const canvasRef = React.useRef(null);
  const stompRef = React.useRef(null);
  const drawingRef = React.useRef(false);
  const previousPointRef = React.useRef(null);
  const [color, setColor] = React.useState("#0f8b8d");
  const [width, setWidth] = React.useState(5);

  React.useEffect(() => {
    if (!roomCode) {
      return undefined;
    }
    const client = new Client({
      webSocketFactory: () => new SockJS(`${gatewayBase}/ws`),
      reconnectDelay: 3000
    });
    client.activate();
    stompRef.current = client;
    return () => client.deactivate();
  }, [gatewayBase, roomCode]);

  React.useEffect(() => {
    const handler = (event) => drawDelta(canvasRef.current, event.detail);
    window.addEventListener("uniplay:draw", handler);
    return () => window.removeEventListener("uniplay:draw", handler);
  }, []);

  React.useEffect(() => {
    const handler = () => {
      drawingRef.current = false;
      previousPointRef.current = null;
      clearCanvas(canvasRef.current);
    };
    window.addEventListener("uniplay:clear-canvas", handler);
    return () => window.removeEventListener("uniplay:clear-canvas", handler);
  }, []);

  React.useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      canvas.width = Math.floor(rect.width * window.devicePixelRatio);
      canvas.height = Math.floor(rect.height * window.devicePixelRatio);
    };
    resize();
    window.addEventListener("resize", resize);
    return () => window.removeEventListener("resize", resize);
  }, []);

  function pointerPoint(event) {
    const rect = canvasRef.current.getBoundingClientRect();
    return {
      x: (event.clientX - rect.left) / rect.width,
      y: (event.clientY - rect.top) / rect.height
    };
  }

  function startDrawing(event) {
    if (!canDraw) {
      return;
    }
    drawingRef.current = true;
    previousPointRef.current = pointerPoint(event);
  }

  function stopDrawing() {
    drawingRef.current = false;
    previousPointRef.current = null;
  }

  function moveDrawing(event) {
    if (!canDraw || !drawingRef.current || !previousPointRef.current || !roomCode) {
      return;
    }
    const nextPoint = pointerPoint(event);
    const delta = {
      playerId: playerId || "anonymous",
      fromX: previousPointRef.current.x,
      fromY: previousPointRef.current.y,
      toX: nextPoint.x,
      toY: nextPoint.y,
      color,
      width
    };
    drawDelta(canvasRef.current, delta);
    if (stompRef.current?.connected) {
      stompRef.current.publish({
        destination: `/app/rooms/${roomCode}/draw`,
        body: JSON.stringify(delta)
      });
    }
    previousPointRef.current = nextPoint;
  }

  return (
    <section className="canvas-shell">
      <div className="canvas-tools">
        <PanelTitle icon={<Brush size={18} />} title="Lienzo" />
        <div className="tool-group">
          <input type="color" value={color} onChange={(event) => setColor(event.target.value)} aria-label="Color" disabled={!canDraw} />
          <input type="range" min="2" max="12" value={width} onChange={(event) => setWidth(Number(event.target.value))} aria-label="Grosor" disabled={!canDraw} />
          <button onClick={() => clearCanvas(canvasRef.current)} disabled={!canDraw} title="Limpiar">
            <Eraser size={16} />
          </button>
        </div>
      </div>
      <div className="canvas-frame">
        <canvas
          id="main-drawing-canvas"
          ref={canvasRef}
          className="drawing-canvas"
          onPointerDown={startDrawing}
          onPointerMove={moveDrawing}
          onPointerUp={stopDrawing}
          onPointerLeave={stopDrawing}
        />
        {!canDraw && <div className="canvas-lock">Esperando turno</div>}
      </div>
    </section>
  );
}
