/**
 * Format ISO-8601 UTC timestamp to localized date string
 * Follows "UTC Everywhere" principle - receive UTC, convert to local for display
 */
export function formatDate(isoString: string): string {
  const date = new Date(isoString);

  // Check for infinity date (9999-12-01)
  if (date.getFullYear() >= 9999) {
    return '∞';
  }

  return date.toLocaleString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Asia/Tokyo'
  });
}

/**
 * Format date for display without time
 */
export function formatDateOnly(isoString: string): string {
  const date = new Date(isoString);

  if (date.getFullYear() >= 9999) {
    return '∞';
  }

  return date.toLocaleDateString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: 'Asia/Tokyo'
  });
}

/**
 * Check if a timestamp represents an active/current record
 */
export function isActive(thruDate: string): boolean {
  const date = new Date(thruDate);
  return date.getFullYear() >= 9999;
}
