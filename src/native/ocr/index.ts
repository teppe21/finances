export interface OcrTextBlock {
  text: string;
  lines: string[];
}

export interface OcrResult {
  text: string;
  blocks?: OcrTextBlock[];
}

export interface OcrBridge {
  recognizeTextFromUri(imageUri: string): Promise<OcrResult>;
}

class NativeOcrBridge implements OcrBridge {
  async recognizeTextFromUri(imageUri: string): Promise<OcrResult> {
    const RN = (global as any).NativeModules;
    if (RN && RN.MlKitOcrModule) {
      return RN.MlKitOcrModule.recognizeTextFromUri(imageUri);
    }

    // Default mock response when testing or previewing outside native Android
    return {
      text: 'NYUGTA\nSPAR MAGYARORSZÁG KFT.\n2026.03.27 18:32\nTEJ 2.8% 1L 450 Ft\nKENYÉR 1KG 890 Ft\nÖSSZESEN: 1 340 Ft\nKÉSZPÉNZ: 1 340 Ft',
    };
  }
}

export const ocrBridge = new NativeOcrBridge();
