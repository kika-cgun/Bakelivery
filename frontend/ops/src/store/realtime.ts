import { create } from 'zustand';
import { createRealtimeConnection } from '@ui/lib/websocket';
import type { RealtimeEvent } from '@ui/types';

interface RealtimeState {
  connected: boolean;
  lastEvent: RealtimeEvent | null;
  cleanup: (() => void) | null;
  connect: (onEvent: (e: RealtimeEvent) => void) => void;
  disconnect: () => void;
}

export const useRealtimeStore = create<RealtimeState>((set, get) => ({
  connected: false,
  lastEvent: null,
  cleanup: null,

  connect: (onEvent) => {
    get().disconnect(); // clean up previous connection

    const cleanup = createRealtimeConnection({
      url: '/ws/orders',
      onEvent: (e) => {
        set({ lastEvent: e });
        onEvent(e);
      },
      onConnect: () => set({ connected: true }),
      onDisconnect: () => set({ connected: false }),
    });

    set({ cleanup });
  },

  disconnect: () => {
    get().cleanup?.();
    set({ cleanup: null, connected: false });
  },
}));
