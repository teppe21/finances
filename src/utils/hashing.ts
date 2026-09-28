/**
 * Fast, deterministic string hashing function (FNV-1a 32-bit variant expanded to hex).
 * Generates identical hashes across runs and platforms without requiring external crypto.
 */
export function simpleHash(input: string): string {
  let h1 = 0x811c9dc5;
  let h2 = 0xcbf29ce4;

  for (let i = 0; i < input.length; i++) {
    const ch = input.charCodeAt(i);
    h1 ^= ch;
    h1 = Math.imul(h1, 0x01000193);

    h2 ^= ch;
    h2 = Math.imul(h2, 0x100000001b3);
  }

  const part1 = (h1 >>> 0).toString(16).padStart(8, '0');
  const part2 = (h2 >>> 0).toString(16).padStart(8, '0');
  return `${part1}${part2}`;
}

/**
 * Creates a unique transaction ID.
 */
export function generateId(prefix: string = 'tx'): string {
  const time = Date.now().toString(36);
  const rand = Math.random().toString(36).substring(2, 8);
  return `${prefix}_${time}_${rand}`;
}
