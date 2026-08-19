// Identificadores de cliente. Se usa randomUUID cuando esta disponible y se cae
// a getRandomValues en contextos no seguros, donde el navegador no lo expone.
export function createClientId() {
  if (typeof crypto?.randomUUID === "function") {
    return crypto.randomUUID();
  }
  if (typeof crypto?.getRandomValues === "function") {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    return [...bytes].map((byte, index) => {
      const value = byte.toString(16).padStart(2, "0");
      return [4, 6, 8, 10].includes(index) ? `-${value}` : value;
    }).join("");
  }
  return `client-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
