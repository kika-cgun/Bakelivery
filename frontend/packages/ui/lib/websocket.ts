import type { RealtimeEvent } from '../types';

type EventHandler = (event: RealtimeEvent) => void;

interface WSOptions {
  url: string;
  onEvent: EventHandler;
  onConnect?: () => void;
  onDisconnect?: () => void;
}

const MAX_RETRIES = 5;
const BASE_DELAY_MS = 1_000;

/**
 * Creates a managed WebSocket connection with exponential backoff reconnect.
 * Returns a cleanup function.
 */
export function createRealtimeConnection(opts: WSOptions): () => void {
  let ws: WebSocket | null = null;
  let retryCount = 0;
  let destroyed = false;

  function connect() {
    if (destroyed) return;

    ws = new WebSocket(opts.url);

    ws.onopen = () => {
      retryCount = 0;
      opts.onConnect?.();
    };

    ws.onmessage = (e) => {
      try {
        const event: RealtimeEvent = JSON.parse(e.data);
        opts.onEvent(event);
      } catch {
        // ignore malformed messages
      }
    };

    ws.onclose = () => {
      opts.onDisconnect?.();
      if (!destroyed && retryCount < MAX_RETRIES) {
        const delay = BASE_DELAY_MS * Math.pow(2, retryCount);
        retryCount++;
        setTimeout(connect, delay);
      }
    };

    ws.onerror = () => {
      ws?.close();
    };
  }

  connect();

  return () => {
    destroyed = true;
    ws?.close();
  };
}
