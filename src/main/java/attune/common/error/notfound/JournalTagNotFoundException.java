package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class JournalTagNotFoundException extends NotFoundException {

    public JournalTagNotFoundException() {
        super("Journal catalog tag not found");
    }
}
