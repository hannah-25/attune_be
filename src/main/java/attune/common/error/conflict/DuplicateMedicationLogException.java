package attune.common.error.conflict;

import attune.common.error.ConflictException;

public class DuplicateMedicationLogException extends ConflictException {
    public DuplicateMedicationLogException() { super("이미 해당 스케줄에 대한 오늘의 복약 기록이 존재합니다."); }
}
