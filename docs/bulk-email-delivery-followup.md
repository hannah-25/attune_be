# Bulk email delivery follow-up

## Current state

Notice and terms-update emails are dispatched after transaction commit from `MailEventListener`.
The listener runs asynchronously and reads active users in pages, but each SMTP request is still sent
sequentially on a shared application executor.

This is acceptable only while recipient volume is small. At larger volumes it can:

- occupy a shared async worker for a long time;
- delay unrelated async work;
- stop midway when the application restarts;
- provide no durable retry or delivery progress tracking.

## Recommended design

Move bulk email delivery to a durable, dedicated pipeline:

1. Save a bulk-email job after the notice or terms transaction commits.
2. Process recipients in stable ID-based batches instead of offset pagination.
3. Use a dedicated executor, Spring Batch worker, or message queue consumer.
4. Persist per-batch progress and retry transient failures with bounded backoff.
5. Prefer a provider bulk API such as AWS SES Bulk Send when the mail provider is selected.
6. Record delivery metrics: queued, attempted, succeeded, failed, and permanently rejected.

Do not submit one unbounded `@Async` task per recipient. That would move the bottleneck into the
executor queue and could exhaust memory under a large broadcast.

## Suggested implementation order

- Introduce a dedicated bulk-mail executor as an immediate isolation measure.
- Add a `bulk_email_jobs` table and resumable batch processor.
- Integrate the selected provider's bulk API and retry/error classification.
- Add operational metrics, alerts, and an administrator-visible delivery summary.

## Completion criteria

- API and transaction threads never perform recipient SMTP calls.
- A process restart resumes an incomplete broadcast without duplicate delivery.
- Individual recipient failures do not stop later recipients.
- Bulk email cannot starve notification, report, or other async workloads.
- Load testing demonstrates acceptable throughput for the expected maximum recipient count.
