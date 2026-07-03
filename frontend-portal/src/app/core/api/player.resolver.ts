import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { PlayerService } from './player.service';
import { Player } from './player.models';

/**
 * Resolver that fetches the authenticated user's profile before a route activates.
 * This acts as the Just-In-Time (JIT) provisioning trigger: if the player doesn't 
 * exist in the database, the backend automatically creates them during this GET request.
 */
export const playerProfileResolver: ResolveFn<Player | null> = () => {
  return inject(PlayerService).getMyProfile().pipe(
    catchError(() => of(null))
  );
};
