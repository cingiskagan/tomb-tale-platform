export interface GameCharacter {
    publicId: string;
    name: string;
    level: number;
    experiencePoints: number;
    createdAt: string;
}

export interface Player {
    publicId: string;
    displayName: string;
    profileIcon: string;
    characters: GameCharacter[];
    createdAt: string;
}

export interface PlayerFilterRequest {
    displayName?: string;
}

export interface UpdateMyProfileRequest {
    displayName: string;
    profileIcon?: string;
}

export interface UpdateCharacterStatsRequest {
    level: number;
    experiencePoints: number;
}
