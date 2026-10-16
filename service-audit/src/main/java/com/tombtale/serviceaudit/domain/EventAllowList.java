package com.tombtale.serviceaudit.domain;

import com.tombtale.serviceaudit.exception.UnlistedEventException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The data fields {@code events} keeps, per event type and version. A field a producer adds later stays out
 * until someone lists it here, because {@code events} keeps everything forever (log policy).
 */
public final class EventAllowList {

    private static final Map<Key, Set<String>> FIELDS = Map.of(
            new Key("player.created", 1), Set.of("playerPublicId", "characterPublicId"),
            new Key("purchase.created", 1), Set.of("purchasePublicId", "playerPublicId", "itemCode", "quantity"),
            new Key("item.granted", 1), Set.of("purchasePublicId"),
            new Key("item.grant-rejected", 1), Set.of("purchasePublicId", "reason"));

    private EventAllowList() {
    }

    /**
     * Copies the listed fields of an event's data, in their order.
     *
     * @param eventType    the event's type
     * @param eventVersion the event's version
     * @param data         the data as the producer sent it
     * @return the listed fields the data carries
     * @throws UnlistedEventException if the type and version have no list
     */
    public static Map<String, Object> keep(String eventType, int eventVersion, Map<String, Object> data) {
        Set<String> listed = FIELDS.get(new Key(eventType, eventVersion));
        if (listed == null) {
            throw new UnlistedEventException(eventType, eventVersion);
        }
        // Not Collectors.toMap, which refuses a null value.
        return data.entrySet().stream()
                .filter(field -> listed.contains(field.getKey()))
                .collect(LinkedHashMap::new, (kept, field) -> kept.put(field.getKey(), field.getValue()), Map::putAll);
    }

    private record Key(String eventType, int eventVersion) {
    }
}
