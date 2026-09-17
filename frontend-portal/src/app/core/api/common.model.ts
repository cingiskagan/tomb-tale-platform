/**
 * Where one page sits in the whole result.
 *
 * Mirrors `PagedResponse.PageInfo` in platform-commons.
 */
export interface PageInfo {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

/**
 * The envelope every paginated list endpoint answers with.
 *
 * Mirrors `PagedResponse<T>` in platform-commons. It is a published contract,
 * not Spring's serialised `Page`, so these four counts are all that arrives.
 */
export interface PagedResponse<T> {
    content: T[];
    page: PageInfo;
}
