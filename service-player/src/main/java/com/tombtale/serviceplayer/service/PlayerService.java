package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdatePlayerStatsRequest;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.exception.PlayerNotFoundException;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business-logic layer for player read operations.
 *
 * <p>
 * Read-only methods default to {@code readOnly = true} for
 * Hibernate flush-mode optimisation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    /**
     * Returns a paginated, filtered list of players.
     *
     * <p>
     * Delegates dynamic predicate construction to the QueryDSL
     * repository fragment.
     *
     * @param filter   optional filter criteria (all fields nullable)
     * @param pageable pagination and sorting parameters
     * @return a page of matching player DTOs
     */
    public Page<PlayerResponse> listPlayers(
            PlayerFilterRequest filter,
            Pageable pageable) {
        return playerRepository.findByFilter(filter, pageable)
                .map(playerMapper::toResponse);
    }

    /**
     * Updates the core progression stats (level, XP) of a specific player.
     *
     * @param id      the internal database ID of the player
     * @param request the new stats payload
     * @return the updated player DTO
     * @throws PlayerNotFoundException if the player is not found
     */
    @Transactional
    public PlayerResponse updatePlayerStats(Long id, UpdatePlayerStatsRequest request) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        player.setLevel(request.getLevel());
        player.setExperiencePoints(request.getExperiencePoints());
        Player saved = playerRepository.save(player);
        return playerMapper.toResponse(saved);
    }
}
