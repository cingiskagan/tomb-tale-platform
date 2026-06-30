import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageService } from 'primeng/api';
import { PlayerService, Player, UpdateCharacterStatsRequest } from '../../core/api';
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
        if (val?.characters && val.characters.length > 0) {
            const firstCharacter = val.characters[0];
            this.form.patchValue({
                level: firstCharacter.level,
                experiencePoints: firstCharacter.experiencePoints
            });
        } else {
            this.form.reset({ level: 1, experiencePoints: 0 });
        }
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
        if (this.form.invalid || !this.player?.characters || this.player.characters.length === 0) return;
        this.saving = true;

        const characterPublicId = this.player.characters[0].publicId;

        try {
            await firstValueFrom(this.playerService.updateCharacterStats(
                this.player.publicId,
                characterPublicId,
                this.form.value as UpdateCharacterStatsRequest
            ));
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
