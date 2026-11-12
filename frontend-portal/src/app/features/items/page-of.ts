import { TableLazyLoadEvent } from 'primeng/table';

const PAGE_SIZE = 20;

/** The page, size and sort a lazy table asks for, as the backend's paged lists take them. */
export function pageOf(event: TableLazyLoadEvent): { page: number; size: number; sort?: string } {
  const size = event.rows || PAGE_SIZE;
  const page = event.first ? Math.floor(event.first / size) : 0;
  const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
  const sort = field ? `${field},${event.sortOrder === -1 ? 'desc' : 'asc'}` : undefined;
  return { page, size, sort };
}
