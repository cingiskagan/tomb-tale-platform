import { PageEvent } from '@angular/material/paginator';
import { Sort, SortDirection } from '@angular/material/sort';

/** The page a table shows. The paginator and the sort header bind to it, so it survives their re-creation. */
export interface PageQuery {
  page: number;
  size: number;
  sort: Sort;
}

export function initQuery(size: number, active = '', direction: SortDirection = ''): PageQuery {
  return { page: 0, size, sort: { active, direction } };
}

/** The query after the admin turns a page or changes its size. */
export function turnPage(query: PageQuery, event: PageEvent): PageQuery {
  return { ...query, page: event.pageIndex, size: event.pageSize };
}

/** A new sort starts again from the first page. */
export function sortBy(query: PageQuery, sort: Sort): PageQuery {
  return { ...query, page: 0, sort };
}

/** The page, size and sort as the backend's paged lists take them. */
export function pageOf(query: PageQuery): { page: number; size: number; sort?: string } {
  const { active, direction } = query.sort;
  return { page: query.page, size: query.size, sort: active && direction ? `${active},${direction}` : undefined };
}
