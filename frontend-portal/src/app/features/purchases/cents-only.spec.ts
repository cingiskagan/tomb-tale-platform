import { FormControl } from '@angular/forms';
import { centsOnly } from './cents-only';

describe('centsOnly', () => {
  /** Whole numbers and up to two decimals pass, a third decimal does not. */
  it('takes a price with at most two decimals', () => {
    expect(centsOnly(new FormControl(12))).toBeNull();
    expect(centsOnly(new FormControl(1.5))).toBeNull();
    expect(centsOnly(new FormControl(1.25))).toBeNull();
    expect(centsOnly(new FormControl(1.255))).toEqual({ centsOnly: true });
  });

  /** An empty field is the required validator's to report, not this one's. */
  it('leaves an empty field alone', () => {
    expect(centsOnly(new FormControl(null))).toBeNull();
  });
});
