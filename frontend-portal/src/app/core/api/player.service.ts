import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Player, PlayerFilterRequest, UpdatePlayerStatsRequest } from './player.models';
import { Page } from './common.model';

@Injectable({ providedIn: 'root' })
export class PlayerService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/players`;

    listPlayers(
        filter: PlayerFilterRequest,
        page = 0,
        size = 20,
        sort?: string
    ): Observable<Page<Player>> {
        let params = new HttpParams().set('page', page).set('size', size);
        if (sort) params = params.set('sort', sort);
        if (filter.displayName) params = params.set('displayName', filter.displayName);
        if (filter.level) params = params.set('level', filter.level);

        return this.http.get<Page<Player>>(this.baseUrl, { params });
    }

    updatePlayerStats(id: number, request: UpdatePlayerStatsRequest): Observable<Player> {
        return this.http.patch<Player>(`${this.baseUrl}/${id}/stats`, request);
    }
}
