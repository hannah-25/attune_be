package attune.journal.domain.repository;

public interface CatalogTagView {
    Long getLegacyTagId();
    String getName();
    String getTagType();
    boolean isVisible();
}
