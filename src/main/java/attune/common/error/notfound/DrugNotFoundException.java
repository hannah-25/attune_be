package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class DrugNotFoundException extends NotFoundException {
    public DrugNotFoundException() {
        super("의약품 정보를 찾을 수 없습니다.");
    }
}
