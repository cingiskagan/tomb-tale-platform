import { AbstractControl, ValidationErrors } from '@angular/forms';

/** A price in whole cents, as the form took it under PrimeNG: 1.5 and 1.25 pass, 1.255 does not. */
export function centsOnly(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === undefined || value === '') return null;
  return /^-?\d+(\.\d{1,2})?$/.test(String(value)) ? null : { centsOnly: true };
}
