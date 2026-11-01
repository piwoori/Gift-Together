ALTER TABLE contributions
DROP CONSTRAINT IF EXISTS contributions_status_check;

ALTER TABLE contributions
    ADD CONSTRAINT contributions_status_check
        CHECK (
            status IN (
                       'PENDING',
                       'COMPLETED',
                       'CANCELLED',
                       'REFUNDED',
                       'FAILED'
                )
            );