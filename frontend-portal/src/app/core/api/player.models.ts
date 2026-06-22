export interface Player {
    id: number;
    zitadelUserId: string;
    displayName: string;
    level: number;
    experiencePoints: number;
    createdAt: string;
    updatedAt: string;
}

export interface PlayerFilterRequest {
    displayName?: string;
    minLevel?: number;
    maxLevel?: number;
}

export interface UpdatePlayerStatsRequest {
    level: number;
    experiencePoints: number;
}
