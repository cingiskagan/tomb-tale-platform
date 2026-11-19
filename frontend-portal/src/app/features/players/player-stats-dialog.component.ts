import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PlayerService, Player, UpdateCharacterStatsRequest } from '../../core/api';
import { DialogDirective, NotificationService } from '../../core/ui';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'app-player-stats-dialog',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatProgressSpinnerModule,
        DialogDirective,
    ],
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
            this.form.enable();
        } else {
            this.form.reset({ level: 1, experiencePoints: 0 });
            this.form.disable();
        }
    }
    get player() { return this._player; }
    private _player: Player | null = null;

    @Output() visibleChange = new EventEmitter<boolean>();
    @Output() saved = new EventEmitter<void>();

    private readonly fb = inject(FormBuilder);
    private readonly playerService = inject(PlayerService);
    private readonly notifications = inject(NotificationService);

    form = this.fb.group({
        level: [1, [Validators.required, Validators.min(1)]],
        experiencePoints: [0, [Validators.required, Validators.min(0)]]
    });
    saving = false;

    /** The player's name, then their character's, which the form edits. */
    get playerLabel(): string {
        const character = this._player?.characters?.[0];
        return (this._player?.displayName || 'Loading...') + (character ? ` (${character.name})` : '');
    }

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
            this.notifications.error('Update failed');
        } finally {
            this.saving = false;
        }
    }
}
