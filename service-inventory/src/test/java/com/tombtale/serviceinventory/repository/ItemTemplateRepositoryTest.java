package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ItemTemplateRepositoryTest extends FixedAuthorTestBase {

    private static final String SWORD_IRON = "SWORD_IRON";

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsATemplateByItsExactCode() {
        persist(aTemplate(SWORD_IRON, 1));

        assertThat(itemTemplateRepository.findByCode(SWORD_IRON)).isPresent();
        assertThat(itemTemplateRepository.findByCode("sword_iron")).isEmpty();
    }

    @Test
    void refusesASecondTemplateWithTheSameCode() {
        persist(aTemplate(SWORD_IRON, 1));

        assertThatThrownBy(() -> persist(aTemplate(SWORD_IRON, 1)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uq_item_templates_code");
    }

    @Test
    void refusesAStackSizeBelowOne() {
        assertThatThrownBy(() -> persist(aTemplate(SWORD_IRON, 0)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_item_templates_max_stack_size");
    }

    private static ItemTemplate aTemplate(String code, int maxStackSize) {
        return ItemTemplate.builder()
                .code(code)
                .maxStackSize(maxStackSize)
                .build();
    }

    private void persist(ItemTemplate template) {
        entityManager.persist(template);
        entityManager.flush();
        entityManager.clear();
    }
}
