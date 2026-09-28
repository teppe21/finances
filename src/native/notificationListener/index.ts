import { RawNotification } from '../../types';

export interface NotificationDiagnostics {
  isListenerConnected: boolean;
  lastEventTimestamp: number;
}

export interface NotificationListenerBridge {
  isNotificationAccessGranted(): Promise<boolean>;
  openNotificationAccessSettings(): Promise<void>;
  getDiagnostics(): Promise<NotificationDiagnostics>;
  addListener(callback: (notification: RawNotification) => void): () => void;
}

/**
 * Native bridge to Kotlin BankNotificationModule with fallback for mock / test environment.
 */
class NativeNotificationListenerBridge implements NotificationListenerBridge {
  private listeners: Set<(notification: RawNotification) => void> = new Set();

  async isNotificationAccessGranted(): Promise<boolean> {
    // Check if React Native NativeModules contains BankNotificationModule
    const RN = (global as any).NativeModules;
    if (RN && RN.BankNotificationModule) {
      return RN.BankNotificationModule.isNotificationAccessGranted();
    }
    return true; // Mock true for testing
  }

  async openNotificationAccessSettings(): Promise<void> {
    const RN = (global as any).NativeModules;
    if (RN && RN.BankNotificationModule) {
      return RN.BankNotificationModule.openNotificationAccessSettings();
    }
  }

  async getDiagnostics(): Promise<NotificationDiagnostics> {
    const RN = (global as any).NativeModules;
    if (RN && RN.BankNotificationModule) {
      return RN.BankNotificationModule.getDiagnostics();
    }
    return {
      isListenerConnected: true,
      lastEventTimestamp: Date.now(),
    };
  }

  addListener(callback: (notification: RawNotification) => void): () => void {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);
  }

  // Used by tests or background simulation
  emitSimulatedNotification(notification: RawNotification) {
    this.listeners.forEach((cb) => cb(notification));
  }
}

export const notificationBridge = new NativeNotificationListenerBridge();
