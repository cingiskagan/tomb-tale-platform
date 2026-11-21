import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { DealerService, ItemTemplateResponse, Rarity, StandEntry } from '../../core/api';
import { DialogDirective } from '../../core/ui';
import { StatsEditorComponent } from '../items/stats-editor.component';
import { StatRow, linesOf } from '../items/stats-form';
import { problemDetail } from '../items/problem-detail';

export const RARITY_OPTIONS = Object.values(Rarity).map((rarity) => ({ label: rarity, value: rarity }));

/** A custom stand entry: every copy a player buys carries exactly the bonus the admin picks here. */
@Component({
  selector: 'app-custom-entry-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    DialogDirective,
    StatsEditorComponent,
  ],
  templateUrl: './custom-entry-form.component.html',
  styleUrl: './dealers.css',
})
export class CustomEntryFormComponent {
  private readonly dealerService = inject(DealerService);

  @Input() dealerCode: string | null = null;
  @Input() template: ItemTemplateResponse | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<StandEntry>();

  private _visible = false;
  /** Moves when the dialog opens or closes, so a late answer never closes the next one. */
  private session = 0;

  @Input()
  set visible(value: boolean) {
    this._visible = value;
    if (value) this.reset();
  }
  get visible() {
    return this._visible;
  }

  readonly rarities = RARITY_OPTIONS;
  readonly form = new FormGroup({
    rarity: new FormControl(Rarity.RED, { nonNullable: true }),
    // No unique-stat check, because a bonus may carry a stat more than once.
    bonus: new FormArray<StatRow>([]),
  });

  saving = false;
  error: string | null = null;

  async save() {
    if (this.form.invalid || !this.dealerCode || !this.template || this.saving) return;
    const session = this.session;
    this.saving = true;
    this.error = null;
    try {
      const entry = await firstValueFrom(
        this.dealerService.addEntry(this.dealerCode, {
          templateId: this.template.publicId,
          rarity: this.form.controls.rarity.value,
          bonus: linesOf(this.form.controls.bonus),
        }),
      );
      this.saved.emit(entry);
      if (session === this.session) this.close();
    } catch (error) {
      if (session === this.session) this.error = problemDetail(error, 'Failed to add the custom item');
    } finally {
      if (session === this.session) this.saving = false;
    }
  }

  close() {
    this.session++;
    this._visible = false;
    this.visibleChange.emit(false);
  }

  private reset() {
    this.session++;
    this.saving = false;
    this.error = null;
    this.form.reset({ rarity: Rarity.RED });
    this.form.controls.bonus.clear();
  }
}
