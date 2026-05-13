package attune.common.error.conflict;

public class DuplicateTagException extends DuplicateException {
    public DuplicateTagException(String tagType) {
        super("이미 동일한 이름의 " + tagType + " 태그가 존재합니다.");
    }
}
