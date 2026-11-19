import { firstPage, pageOf, sortBy, turnPage } from './page-query';

describe('page query', () => {
  /** The backend reads the sort as `field,direction`. */
  it('sends the page, the size and the sort as the backend reads them', () => {
    expect(pageOf(turnPage(firstPage(20, 'code', 'asc'), { pageIndex: 2, pageSize: 50, length: 300 }))).toEqual({
      page: 2,
      size: 50,
      sort: 'code,asc',
    });
  });

  /** A sort on another column must not keep the old page number, which may not exist under the new order. */
  it('goes back to the first page when the sort changes', () => {
    const third = turnPage(firstPage(20, 'code', 'asc'), { pageIndex: 2, pageSize: 20, length: 300 });

    expect(sortBy(third, { active: 'price', direction: 'desc' })).toEqual(firstPage(20, 'price', 'desc'));
  });

  /** A table with no sort, or a cleared one, sends none and takes the backend's order. */
  it('sends no sort when none is picked', () => {
    expect(pageOf(firstPage(20)).sort).toBeUndefined();
    expect(pageOf(sortBy(firstPage(20), { active: 'code', direction: '' })).sort).toBeUndefined();
  });
});
