import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Player, PlayerFilterRequest, UpdateCharacterStatsRequest, UpdateMyProfileRequest, GameCharacter } from './player.models';
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

        return this.http.get<Page<Player>>(this.baseUrl, { params });
    }

    getMyProfile(): Observable<Player> {
        return this.http.get<Player>(`${this.baseUrl}/me`);
    }

    updateMyProfile(request: UpdateMyProfileRequest): Observable<Player> {
        return this.http.patch<Player>(`${this.baseUrl}/me`, request);
    }

    updateCharacterStats(publicId: string, characterPublicId: string, request: UpdateCharacterStatsRequest): Observable<GameCharacter> {
        return this.http.patch<GameCharacter>(`${this.baseUrl}/${publicId}/characters/${characterPublicId}/stats`, request);
    }
}
