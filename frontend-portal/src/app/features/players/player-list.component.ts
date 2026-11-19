import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { PlayerService, Player, PlayerFilterRequest, GameCharacter } from '../../core/api';
import { AuthService, PlatformRole } from '../../core/auth';
import { NotificationService, PageQuery, firstPage, pageOf, sortBy, turnPage } from '../../core/ui';
import { PlayerStatsDialogComponent } from './player-stats-dialog.component';
import { InventoryDialogComponent } from './inventory-dialog.component';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'app-player-list',
    standalone: true,
    imports: [
        CommonModule,
        MatButtonModule,
        MatIconModule,
        MatPaginatorModule,
        MatProgressBarModule,
        MatSortModule,
        MatTableModule,
        PlayerStatsDialogComponent,
        InventoryDialogComponent,
    ],
    templateUrl: './player-list.component.html',
    styleUrl: './player-list.component.css'
})
export class PlayerListComponent implements OnInit {
    private readonly playerService = inject(PlayerService);
    private readonly notifications = inject(NotificationService);

    /** Only `platform_admin` opens an inventory, because inventory refuses everyone else's. */
    readonly isAdmin = inject(AuthService).hasAnyRole([PlatformRole.PLATFORM_ADMIN]);

    readonly columns = ['publicId', 'displayName', 'level', 'xp', 'actions'];

    players: Player[] = [];
    totalRecords = 0;
    loading = true;
    isDialogVisible = false;
    selectedPlayer: Player | null = null;
    isInventoryVisible = false;
    inventoryCharacter: GameCharacter | null = null;
    query: PageQuery = firstPage(20);

    ngOnInit() {
        void this.loadPlayers(this.query);
    }

    sortPlayers(sort: Sort) {
        void this.loadPlayers(sortBy(this.query, sort));
    }

    pagePlayers(event: PageEvent) {
        void this.loadPlayers(turnPage(this.query, event));
    }

    async loadPlayers(query: PageQuery) {
        this.query = query;
        this.loading = true;
        try {
            const filters: PlayerFilterRequest = {};
            const { page, size, sort } = pageOf(query);
            const res = await firstValueFrom(this.playerService.listPlayers(filters, page, size, sort));
            this.players = res.content;
            this.totalRecords = res.page.totalElements;
        } catch (err) {
            console.error('Failed to load players:', err);
            this.notifications.error('Failed to load');
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
        this.notifications.success('Stats updated');
        void this.loadPlayers(this.query);
    }
}
