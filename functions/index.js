/**
 * EasyLink Cloud Functions.
 *
 * waitlistDigest — emails a summary of new easylinkcare.com waitlist signups
 * twice a day (09:00 and 21:00 America/New_York). Sends nothing when there are
 * no new signups, so an email in the inbox always means news.
 *
 * Email is sent through Gmail SMTP. One-time setup:
 *   1. Google Account → Security → 2-Step Verification → App passwords
 *      → create one named "easylink-digest".
 *   2. firebase functions:secrets:set GMAIL_APP_PASSWORD   (paste it there)
 *   3. firebase deploy --only functions
 */
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret, defineString } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const nodemailer = require("nodemailer");

initializeApp();

const GMAIL_APP_PASSWORD = defineSecret("GMAIL_APP_PASSWORD");
const GMAIL_USER = defineString("GMAIL_USER", {
  default: "colinrtucker@gmail.com",
  description: "Gmail account that sends the digest (also the recipient)",
});

/** Cursor doc remembering where the previous digest left off. */
const CURSOR_PATH = "meta/waitlistDigest";

exports.waitlistDigest = onSchedule(
  {
    schedule: "0 9,21 * * *",
    timeZone: "America/New_York",
    secrets: [GMAIL_APP_PASSWORD],
    retryCount: 2,
  },
  async () => {
    const db = getFirestore();

    // Everything since the last successful digest (first run: last 12h).
    const cursorRef = db.doc(CURSOR_PATH);
    const cursorSnap = await cursorRef.get();
    const since =
      cursorSnap.get("lastRunAt") ??
      Timestamp.fromMillis(Date.now() - 12 * 60 * 60 * 1000);

    const newSignups = await db
      .collection("waitlist")
      .where("createdAt", ">", since)
      .orderBy("createdAt", "asc")
      .get();

    // Advance the cursor even when empty, so a burst right before a quiet
    // period is never re-reported later.
    await cursorRef.set({ lastRunAt: Timestamp.now() }, { merge: true });

    if (newSignups.empty) {
      console.log("No new waitlist signups — skipping email.");
      return;
    }

    const total = (await db.collection("waitlist").count().get()).data().count;
    const lines = newSignups.docs.map((d) => {
      const at = d.get("createdAt")?.toDate()?.toISOString() ?? "?";
      return `  • ${d.id}   (${at})`;
    });

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: { user: GMAIL_USER.value(), pass: GMAIL_APP_PASSWORD.value() },
    });

    await transporter.sendMail({
      from: `"EasyLink Waitlist" <${GMAIL_USER.value()}>`,
      to: GMAIL_USER.value(),
      subject: `EasyLink waitlist: ${newSignups.size} new signup${
        newSignups.size === 1 ? "" : "s"
      } (${total} total)`,
      text: [
        `New signups on easylinkcare.com since the last digest:`,
        ``,
        ...lines,
        ``,
        `Total waitlist size: ${total}`,
        ``,
        `— easylink-apps · waitlistDigest (9am & 9pm ET)`,
      ].join("\n"),
    });

    console.log(`Emailed digest: ${newSignups.size} new, ${total} total.`);
  },
);
