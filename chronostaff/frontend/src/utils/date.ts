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
 * CRITICAL: Use toLocaleString() to ensure timezone is properly applied,
 * then extract only the date part
 */
export function formatDateOnly(isoString: string): string {
  console.log('DEBUG formatDateOnly - Input:', isoString);
  const date = new Date(isoString);
  console.log('DEBUG formatDateOnly - Date object:', date.toISOString());

  if (date.getFullYear() >= 9999) {
    return '∞';
  }

  // Use toLocaleString() with timeZone to get proper JST conversion,
  // then extract only the date part
  const fullString = date.toLocaleString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Asia/Tokyo'
  });

  console.log('DEBUG formatDateOnly - Full locale string:', fullString);

  // Extract date part (YYYY/MM/DD) from "YYYY/MM/DD HH:MM"
  const result = fullString.split(' ')[0];
  console.log('DEBUG formatDateOnly - Result:', result);
  return result;
}

/**
 * Check if a timestamp represents an active/current record
 */
export function isActive(thruDate: string): boolean {
  const date = new Date(thruDate);
  return date.getFullYear() >= 9999;
}
