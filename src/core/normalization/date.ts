const HU_MONTHS: Record<string, string> = {
  januar: '01',
  január: '01',
  februar: '02',
  február: '02',
  marcius: '03',
  március: '03',
  aprilis: '04',
  április: '04',
  majus: '05',
  május: '05',
  junius: '06',
  június: '06',
  julius: '07',
  július: '07',
  augusztus: '08',
  szeptember: '09',
  oktober: '10',
  október: '10',
  november: '11',
  december: '12',
};

/**
 * Normalizes varied date strings into standard YYYY-MM-DD.
 */
export function normalizeDate(input: string, fallback?: string): string {
  if (!input || typeof input !== 'string') {
    return fallback || new Date().toISOString().substring(0, 10);
  }

  const trimmed = input.trim();

  // Pattern 1: ISO YYYY-MM-DD or YYYY-MM-DD...
  const isoMatch = trimmed.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/);
  if (isoMatch) {
    const year = isoMatch[1];
    const month = isoMatch[2].padStart(2, '0');
    const day = isoMatch[3].padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // Pattern 2: Hungarian text date like "2026. március 1." or "2026. márc. 1."
  const huTextMatch = trimmed.match(/(\d{4})\.\s*([a-záéíóöőúüű]+)\.?\s*(\d{1,2})/i);
  if (huTextMatch) {
    const year = huTextMatch[1];
    const rawMonth = huTextMatch[2].toLowerCase();
    const day = huTextMatch[3].padStart(2, '0');

    let monthStr = '01';
    for (const [name, num] of Object.entries(HU_MONTHS)) {
      if (rawMonth.startsWith(name.substring(0, 4))) {
        monthStr = num;
        break;
      }
    }
    return `${year}-${monthStr}-${day}`;
  }

  // Pattern 3: European DD.MM.YYYY or DD/MM/YYYY
  const euMatch = trimmed.match(/^(\d{1,2})[./-](\d{1,2})[./-](\d{4})/);
  if (euMatch) {
    const day = euMatch[1].padStart(2, '0');
    const month = euMatch[2].padStart(2, '0');
    const year = euMatch[3];
    return `${year}-${month}-${day}`;
  }

  // Fallback: Attempt Date parsing
  const parsed = new Date(trimmed);
  if (!isNaN(parsed.getTime())) {
    return parsed.toISOString().substring(0, 10);
  }

  return fallback || new Date().toISOString().substring(0, 10);
}

/**
 * Returns month key in YYYY-MM format.
 */
export function getMonthKey(dateStr: string): string {
  const normalized = normalizeDate(dateStr);
  return normalized.substring(0, 7);
}
