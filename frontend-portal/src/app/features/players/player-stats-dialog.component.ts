import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageService } from 'primeng/api';
import { PlayerService, Player, UpdatePlayerStatsRequest } from '../../core/api';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'app-player-stats-dialog',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, DialogModule, ButtonModule, InputNumberModule],
    templateUrl: './player-stats-dialog.component.html'
})
export class PlayerStatsDialogComponent {
    @Input() visible = false;
    @Input() set player(val: Player | null) {
        this._player = val;
        if (val) this.form.patchValue({ level: val.level, experiencePoints: val.experiencePoints });
    }
    get player() { return this._player; }
    private _player: Player | null = null;

    @Output() visibleChange = new EventEmitter<boolean>();
    @Output() saved = new EventEmitter<void>();

    private readonly fb = inject(FormBuilder);
    private readonly playerService = inject(PlayerService);
    private readonly messageService = inject(MessageService);

    form = this.fb.group({
        level: [1, [Validators.required, Validators.min(1)]],
        experiencePoints: [0, [Validators.required, Validators.min(0)]]
    });
    saving = false;

    close() { this.visibleChange.emit(false); }

    async save() {
        if (this.form.invalid || !this.player) return;
        this.saving = true;
        try {
            await firstValueFrom(this.playerService.updatePlayerStats(this.player.id, this.form.value as UpdatePlayerStatsRequest));
            this.saved.emit();
            this.close();
        } catch (err) {
            console.error('Failed to update stats:', err);
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Update failed' });
        } finally {
            this.saving = false;
        }
    }
}
