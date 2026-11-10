import { statArray, statRow, statsOf } from './stats-form';

describe('stats form', () => {
  it('turns stats into rows and back', () => {
    const rows = statArray({ FIRE_DAMAGE: 12, CRIT_CHANCE: 3 });

    expect(rows.length).toBe(2);
    expect(statsOf(rows)).toEqual({ FIRE_DAMAGE: 12, CRIT_CHANCE: 3 });
  });

  it('leaves out a row that is not filled in yet', () => {
    const rows = statArray({ ARMOR: 5 });
    rows.push(statRow());

    expect(statsOf(rows)).toEqual({ ARMOR: 5 });
    expect(rows.invalid).toBeTrue();
  });

  it('refuses the same stat twice', () => {
    const rows = statArray({ ARMOR: 5 });
    rows.push(statRow('ARMOR', 7));

    expect(rows.hasError('duplicateStat')).toBeTrue();
  });
});
