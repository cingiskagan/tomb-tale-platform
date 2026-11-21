import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { STATS } from '../../core/api';
import { StatRow, statRow } from './stats-form';

/** Edits stat names and values, for a template's base stats or a set's bonus. */
@Component({
  selector: 'app-stats-editor',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  templateUrl: './stats-editor.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './items.css',
})
export class StatsEditorComponent {
  @Input({ required: true }) rows!: FormArray<StatRow>;
  @Input() label = 'Stats';

  readonly statOptions = STATS.map((stat) => ({ label: stat, value: stat }));

  add() {
    this.rows.push(statRow());
  }

  remove(index: number) {
    this.rows.removeAt(index);
  }
}
