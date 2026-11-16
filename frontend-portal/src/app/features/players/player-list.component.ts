import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PlayerService, Player, PlayerFilterRequest, GameCharacter } from '../../core/api';
import { AuthService, PlatformRole } from '../../core/auth';
import { PlayerStatsDialogComponent } from './player-stats-dialog.component';
import { InventoryDialogComponent } from './inventory-dialog.component';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'app-player-list',
    standalone: true,
    imports: [CommonModule, TableModule, ButtonModule, ToastModule, PlayerStatsDialogComponent, InventoryDialogComponent],
    providers: [ConfirmationService, MessageService],
    templateUrl: './player-list.component.html',
    styleUrl: './player-list.component.css'
})
export class PlayerListComponent {
    private readonly playerService = inject(PlayerService);
    private readonly messageService = inject(MessageService);

    /** Only `platform_admin` opens an inventory, because inventory refuses everyone else's. */
    readonly isAdmin = inject(AuthService).hasAnyRole([PlatformRole.PLATFORM_ADMIN]);

    players: Player[] = [];
    totalRecords = 0;
    loading = true;
    isDialogVisible = false;
    selectedPlayer: Player | null = null;
    isInventoryVisible = false;
    inventoryCharacter: GameCharacter | null = null;
    private lastEvent: TableLazyLoadEvent | null = null;

    async loadPlayers(event: TableLazyLoadEvent) {
        this.lastEvent = event;
        this.loading = true;
        try {
            const page = event.first ? Math.floor(event.first / (event.rows || 20)) : 0;
            const filters: PlayerFilterRequest = {};

            let sort: string | undefined;
            if (event.sortField) {
                const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
                const order = event.sortOrder === -1 ? 'desc' : 'asc';
                sort = `${field},${order}`;
            }

            const res = await firstValueFrom(this.playerService.listPlayers(filters, page, event.rows || 20, sort));
            this.players = res.content;
            this.totalRecords = res.page.totalElements;
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

    /** A player has one character for now, so the dialog opens the first. */
    openInventory(player: Player) {
        this.inventoryCharacter = player.characters?.[0] ?? null;
        this.isInventoryVisible = true;
    }

    onSaved() {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Stats updated' });
        if (this.lastEvent) this.loadPlayers(this.lastEvent);
    }
}
