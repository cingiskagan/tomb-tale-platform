import { Component, Input } from '@angular/core';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { STATS } from '../../core/api';
import { StatRow, statRow } from './stats-form';

/** Edits stat names and values, for a template's base stats or a set's bonus. */
@Component({
  selector: 'app-stats-editor',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, InputNumberModule, SelectModule],
  templateUrl: './stats-editor.component.html',
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
