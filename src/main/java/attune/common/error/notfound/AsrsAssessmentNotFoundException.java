package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class AsrsAssessmentNotFoundException extends NotFoundException {
    public AsrsAssessmentNotFoundException() {
        super("ASRS 이력을 찾을 수 없습니다");
    }
}
