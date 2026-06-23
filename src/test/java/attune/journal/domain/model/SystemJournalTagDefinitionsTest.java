package attune.journal.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemJournalTagDefinitionsTest {

    @Test
    void containsCanonicalUniqueSystemTags() {
        assertThat(SystemJournalTagDefinitions.all()).hasSize(46);
        assertThat(SystemJournalTagDefinitions.all())
                .filteredOn(definition -> definition.category() == JournalTagCategory.CONDITION)
                .hasSize(14);
        assertThat(SystemJournalTagDefinitions.all())
                .filteredOn(definition -> definition.category() == JournalTagCategory.SIDE_EFFECT)
                .hasSize(13);
        assertThat(SystemJournalTagDefinitions.troubleTags()).hasSize(19);
        assertThat(SystemJournalTagDefinitions.all())
                .extracting(definition -> definition.category() + "|"
                        + definition.name() + "|" + definition.tagType())
                .doesNotHaveDuplicates();
    }
}
