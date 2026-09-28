/**
 * Strips diacritics / accents for robust matching (e.g. 'kávézó' -> 'kavezo', 'Lidl Áruház' -> 'lidl aruhaz').
 */
export function removeDiacritics(text: string): string {
  if (!text) return '';
  return text
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

/**
 * Normalizes text for search and merchant comparison:
 * lowercase, stripped diacritics, trimmed, consolidated whitespace.
 */
export function normalizeSearchText(text: string): string {
  if (!text) return '';
  return removeDiacritics(text)
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Cleans merchant names from transaction lines by stripping noise words, card hints, etc.
 */
export function cleanMerchantName(raw: string): string {
  if (!raw) return '';
  let cleaned = raw.trim();

  // Strip common bank notification prefixes/suffixes
  cleaned = cleaned
    .replace(/^(vásárlás|vasarlas|card payment|pos purchase|payment to|fizetés|fizetes)\s*[:\-–]?\s*/i, '')
    .replace(/\s*(kft\.?|zrt\.?|bt\.?|nyrt\.?|gmbh|ltd\.?|inc\.?|étterem|etterem|vendéglő|kávézó|kavezo)$/i, '')
    .replace(/\s*#\d+.*$/, '')
    .trim();

  // If entirely uppercase and multi-word, convert to Title Case
  if (cleaned.length > 3 && cleaned === cleaned.toUpperCase() && !/^[A-Z]{2,4}$/.test(cleaned)) {
    cleaned = cleaned
      .toLowerCase()
      .split(' ')
      .map((w) => (w.length > 0 ? w[0].toUpperCase() + w.slice(1) : ''))
      .join(' ');
  }

  return cleaned;
}
