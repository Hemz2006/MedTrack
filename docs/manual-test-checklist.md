# MedTrack manual test checklist (on a real Android phone)

Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (USB debugging on), or copy the APK to the phone and open it.

## Setup
- [ ] First launch opens "Set up reminders"; each "Allow" opens the right system screen; statuses update when you come back; "Done" goes to Today. (On Android 13+ exact alarms are granted at install, so the "Exact alarms" row may already show Allowed.)

## Medications
- [ ] Add "pantaprazole" (misspelled): a suggestion "pantoprazole" appears; picking it fills ingredients (40 MG …), formula C16H15F2N3O4S and a "Used for" note.
- [ ] Add "paracetamol": resolves to acetaminophen with formula C8H9NO2.
- [ ] Airplane mode, add "metformin": the medication saves; "Not looked up yet – tap to retry" shows; retry works once you're back online.
- [ ] Camera photo and gallery photo both appear (right way up) in the list, Today and the alarm.
- [ ] Edit a medication, replace its photo, then save. The old photo is gone and the new one shows.

## Reminders
- [ ] Daily time 2 minutes from now: the alarm rings with sound on the lock screen, full screen, showing photo, dose and notes.
- [ ] Taken from the notification: the notification clears; Today shows Taken; History shows it.
- [ ] Snooze: it rings again about 10 minutes later.
- [ ] Ignore a dose for 2 hours: it shows as Missed.
- [ ] Two medications at the same minute: both listed; "Take all" marks both.
- [ ] Reboot the phone before a dose and unlock it: it still rings. (After a reboot, reminders resume only once the phone has been unlocked.)
- [ ] Battery saver on, screen off for 30+ minutes: it still rings on time.
- [ ] Change the time zone: today's times follow the new local time.
- [ ] Every-6-hours and weekdays schedules ring on the expected days and times.
- [ ] A course with an end date: no reminders after the end date.
- [ ] Pause a medication: no more reminders; history kept. Delete: it's gone, including its history.

## Meal reminders (Health Connect)
- [ ] Settings → Health Connect → Connect grants nutrition access.
- [ ] An after-breakfast medication with a 0 min delay: log a breakfast in a Health Connect app (e.g. Samsung Health, MyFitnessPal). The reminder arrives within about 15 minutes. The fallback time doesn't ring that day.
- [ ] No meal logged: the fallback time rings.
- [ ] Revoke Health Connect permission: Today shows a warning; the fallback still rings.
