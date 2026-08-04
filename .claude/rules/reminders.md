---
paths:
  - "app/src/main/java/com/subkan/data/reminder/**"
  - "app/src/main/java/com/subkan/ui/permissions/**"
---

# Payment reminder rules

## Two alarms, not one per subscription

There is exactly one alarm per `ReminderKind` — the evening-before reminder and the morning-of one.
The alarm carries only which kind it is; `ReminderReceiver` asks the database what is due when it
fires.

This is load-bearing, not an optimisation:

- Adding, editing or deleting a subscription needs no rescheduling at all.
- Several charges on the same day produce one notification instead of five.
- The number of booked alarms does not grow with the user's list.

Do not "improve" this into per-subscription alarms. It would reintroduce every one of those
problems and require hooking the repository's write paths.

## Every alarm re-books the next one

`ReminderReceiver` calls `rescheduleAll()` at the end of its work, unconditionally and inside the
`finally`. An alarm that fires without booking tomorrow's stops the feature after one day, and
nothing surfaces the failure.

Anything that changes a notification setting must also call `rescheduleAll()`. Both *whether* and
*when* a reminder fires are decided at scheduling time, so a changed time stays un-booked until
something reschedules — which might not be until the next reboot.

## Inexact alarms are the deliberate choice

`setAndAllowWhileIdle`, never `setExactAndAllowWhileIdle`. The exact variant needs
`SCHEDULE_EXACT_ALARM`, which is a second runtime prompt and is policy-restricted on Play. A payment
reminder is fine landing a few minutes either side of 20:00, and the inexact variant still fires in
Doze.

If a future feature genuinely needs to-the-minute delivery, that is a product decision with a
permission cost attached — not a quiet change to this file.

## Recurrence is derived, never written back

`nextPaymentDate` is an anchor. Nothing in the app advances it, so reminders use
`Subscription.nextOccurrenceOnOrAfter` to roll it forward by the billing cycle.

Do not "fix" this by updating the stored date when a payment passes. The list's 「毎月5日」 label
reads the anchor, and rewriting it would drag the anchor off the 31st the first time it landed in
February. Short months clamp on the way out and the anchor stays put — that is the whole point.

## Permission

`POST_NOTIFICATIONS` is requested once, tracked by `notificationPermissionRequested` in settings
rather than inferred: the system dialog appears only the first time, and afterwards "denied" and
"never asked" are indistinguishable to the app. The flag being unset for existing installs is what
makes them see the prompt on their next open.

Re-check on `ON_RESUME`, always. The permission can be granted or revoked in system settings while
the app is backgrounded and nothing tells the app about it. When it is held, re-book the alarms —
that is the only path by which reminders start working after the user says yes in Settings rather
than in the dialog.

Never post without checking `areNotificationsEnabled()` first: it throws on some OEM builds and is a
silent no-op on others.
