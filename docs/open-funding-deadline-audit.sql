BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;

WITH candidates AS (
    SELECT id, expired_at, created_at, updated_at,
           expired_at AT TIME ZONE 'Asia/Seoul' AS deadline_if_seoul,
           expired_at AT TIME ZONE 'UTC' AS deadline_if_utc
    FROM public.fundings
    WHERE status = 'OPEN'
)
SELECT id AS funding_id, expired_at, created_at, updated_at,
       deadline_if_seoul AT TIME ZONE 'UTC' AS seoul_interpretation_utc,
       deadline_if_utc AT TIME ZONE 'UTC' AS utc_interpretation_utc,
       CURRENT_TIMESTAMP AT TIME ZONE 'UTC' AS checked_at_utc,
       CASE
           WHEN deadline_if_seoul <= CURRENT_TIMESTAMP AND deadline_if_utc <= CURRENT_TIMESTAMP
               THEN 'due_under_both'
           WHEN deadline_if_seoul <= CURRENT_TIMESTAMP OR deadline_if_utc <= CURRENT_TIMESTAMP
               THEN 'interpretation_changes_expiry'
           ELSE 'future_under_both'
       END AS expiry_comparison
FROM candidates
ORDER BY expired_at, id;

COMMIT;
