import { AbstractControl, FormArray, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Stat, Stats } from '../../core/api';

/** One stat and its value, as the stats editor shows it. */
export type StatRow = FormGroup<{
  stat: FormControl<Stat | null>;
  value: FormControl<number | null>;
}>;

export function statRow(stat: Stat | null = null, value: number | null = null): StatRow {
  return new FormGroup({
    stat: new FormControl<Stat | null>(stat, Validators.required),
    value: new FormControl<number | null>(value, Validators.required),
  });
}

/** The rows for stats the backend sent, in the order it sent them. */
export function statRows(stats: Stats): StatRow[] {
  return (Object.entries(stats) as [Stat, number][]).map(([stat, value]) => statRow(stat, value));
}

/** The rows as the backend takes them. */
export function statsOf(rows: FormArray<StatRow>): Stats {
  const stats: Stats = {};
  for (const row of rows.controls) {
    const { stat, value } = row.getRawValue();
    if (stat !== null && value !== null) {
      stats[stat] = value;
    }
  }
  return stats;
}

/** A stat may appear once, or the second row would silently replace the first. */
export function uniqueStats(control: AbstractControl): ValidationErrors | null {
  const stats = (control as FormArray<StatRow>).controls
    .map((row) => row.controls.stat.value)
    .filter((stat) => stat !== null);
  return new Set(stats).size === stats.length ? null : { duplicateStat: true };
}

export function statArray(stats: Stats = {}): FormArray<StatRow> {
  return new FormArray<StatRow>(statRows(stats), uniqueStats);
}
