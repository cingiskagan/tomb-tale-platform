package com.tombtale.commons.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the identity and equality contract of {@link BaseEntity}.
 *
 * <p>Everything here is about the state an entity has before it is ever saved,
 * which is exactly the state the old {@code @PrePersist} assignment did not
 * cover.
 */
class BaseEntityTest {

    /** Minimal concrete subclass; {@link BaseEntity} adds no behaviour to override. */
    private static final class FixtureEntity extends BaseEntity {
    }

    @Test
    @DisplayName("a newly constructed entity already has a publicId")
    void assignsPublicIdOnConstruction() {
        assertThat(new FixtureEntity().getPublicId()).isNotNull();
    }

    @Test
    @DisplayName("each entity gets its own publicId")
    void assignsADistinctPublicIdPerInstance() {
        assertThat(new FixtureEntity().getPublicId())
                .isNotEqualTo(new FixtureEntity().getPublicId());
    }

    @Test
    @DisplayName("the generated key is untouched until the row is inserted")
    void leavesTheInternalKeyNull() {
        assertThat(new FixtureEntity().getId()).isNull();
    }

    @Test
    @DisplayName("entities sharing a publicId are equal and hash alike")
    void equalsOnPublicId() {
        UUID shared = UUID.randomUUID();
        FixtureEntity one = new FixtureEntity();
        FixtureEntity two = new FixtureEntity();
        one.setPublicId(shared);
        two.setPublicId(shared);

        assertThat(one).isEqualTo(two).hasSameHashCodeAs(two);
    }

    @Test
    @DisplayName("entities with different publicIds are not equal")
    void differsOnPublicId() {
        assertThat(new FixtureEntity()).isNotEqualTo(new FixtureEntity());
    }

    @Test
    @DisplayName("an entity is not equal to null or to an unrelated type")
    void rejectsNullAndForeignTypes() {
        FixtureEntity entity = new FixtureEntity();

        assertThat(entity).isNotEqualTo(null).isNotEqualTo("not an entity");
    }

    @Test
    @DisplayName("an unsaved entity stays findable in a Set after its key is assigned")
    void survivesKeyAssignmentInsideASet() {
        FixtureEntity entity = new FixtureEntity();
        Set<BaseEntity> set = new HashSet<>();
        set.add(entity);

        entity.setId(1L);

        assertThat(set).contains(entity);
    }

    // An uninitialised Hibernate proxy reads its own null field in both methods
    // below, so the guards are live code rather than defensive padding.

    @Test
    @DisplayName("an entity without a publicId hashes to zero")
    void hashesToZeroWithoutAPublicId() {
        FixtureEntity entity = new FixtureEntity();
        entity.setPublicId(null);

        assertThat(entity.hashCode()).isZero();
    }

    @Test
    @DisplayName("an entity without a publicId equals nothing but itself")
    void equalsNothingButItselfWithoutAPublicId() {
        FixtureEntity one = new FixtureEntity();
        FixtureEntity two = new FixtureEntity();
        one.setPublicId(null);
        two.setPublicId(null);

        assertThat(one).isEqualTo(one).isNotEqualTo(two);
    }
}
