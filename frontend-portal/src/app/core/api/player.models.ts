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
    level?: number;
}

export interface UpdatePlayerStatsRequest {
    level: number;
    experiencePoints: number;
}
