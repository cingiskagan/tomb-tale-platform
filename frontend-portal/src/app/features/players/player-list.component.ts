import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { PlayerService, Player, PlayerFilterRequest } from '../../core/api';
import { PlayerStatsDialogComponent } from './player-stats-dialog.component';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'app-player-list',
    standalone: true,
    imports: [CommonModule, TableModule, ButtonModule, ToastModule, PlayerStatsDialogComponent],
    providers: [MessageService],
    templateUrl: './player-list.component.html',
    styleUrl: './player-list.component.css'
})
export class PlayerListComponent {
    private readonly playerService = inject(PlayerService);
    private readonly messageService = inject(MessageService);

    players: Player[] = [];
    totalRecords = 0;
    loading = true;
    isDialogVisible = false;
    selectedPlayer: Player | null = null;
    private lastEvent: TableLazyLoadEvent | null = null;

    async loadPlayers(event: TableLazyLoadEvent) {
        this.lastEvent = event;
        this.loading = true;
        try {
            const page = event.first ? Math.floor(event.first / (event.rows || 20)) : 0;
            const filters: PlayerFilterRequest = {};
            const res = await firstValueFrom(this.playerService.listPlayers(filters, page, event.rows || 20));
            this.players = res.content;
            this.totalRecords = res.totalElements;
        } catch (err) {
            console.error('Failed to load players:', err);
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load' });
        } finally {
            this.loading = false;
        }
    }

    openEdit(player: Player) {
        this.selectedPlayer = player;
        this.isDialogVisible = true;
    }

    onSaved() {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Stats updated' });
        if (this.lastEvent) this.loadPlayers(this.lastEvent);
    }
}
