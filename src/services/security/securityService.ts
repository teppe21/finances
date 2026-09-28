import { SettingsRepository } from '../../database/repositories/settingsRepository';
import { simpleHash } from '../../utils/hashing';

export type LockType = 'off' | 'pin' | 'biometric';

export class SecurityService {
  constructor(private settingsRepo: SettingsRepository = new SettingsRepository()) {}

  async getLockType(): Promise<LockType> {
    const val = await this.settingsRepo.get('security_lock_type', 'off');
    return val as LockType;
  }

  async setLockType(type: LockType): Promise<void> {
    await this.settingsRepo.set('security_lock_type', type);
  }

  async setPin(pin: string): Promise<void> {
    if (!pin || pin.length < 4) {
      throw new Error('A PIN kódnak legalább 4 számjegyből kell állnia.');
    }
    const salt = 'sec_finance_salt_2026';
    const hash = simpleHash(`${salt}_${pin}`);
    await this.settingsRepo.set('security_pin_hash', hash);
    await this.setLockType('pin');
  }

  async verifyPin(enteredPin: string): Promise<boolean> {
    const storedHash = await this.settingsRepo.get('security_pin_hash', '');
    if (!storedHash) return true;
    const salt = 'sec_finance_salt_2026';
    const enteredHash = simpleHash(`${salt}_${enteredPin}`);
    return storedHash === enteredHash;
  }

  async isBiometricAvailable(): Promise<boolean> {
    // In React Native environment, expo-local-authentication checks hardware
    return true;
  }
}
