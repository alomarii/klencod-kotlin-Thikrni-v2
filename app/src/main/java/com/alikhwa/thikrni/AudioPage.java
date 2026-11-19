package com.alikhwa.thikrni;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** الصوتيات: عدة تسجيلات، عشوائي أو بالترتيب، معاينة، إضافة وحذف. */
final class AudioPage extends Page {
    private LinearLayout listBox;
    private TextView tvStatus;
    private File playing;

    AudioPage(MainActivity a, Ui ui) {
        super(a, ui);
        build();
    }

    @Override
    void build() {
        Theme t = ui.t;
        content.addView(ui.pageTitle("الصوتيات", null));

        LinearLayout modeCard = ui.card();
        modeCard.addView(ui.sectionTitle("طريقة التشغيل"));
        modeCard.addView(ui.switchRow("تشغيل الصوت", p.sound(), b -> p.setSound(b)));
        modeCard.addView(ui.divider());
        TextView lbl = ui.text("اختيار التسجيل عند كل تذكير:", 15, Color.WHITE, true);
        lbl.setGravity(Gravity.START);
        lbl.setPadding(0, ui.dp(4), 0, ui.dp(8));
        modeCard.addView(lbl);
        final Ui.Chips[] mode = new Ui.Chips[1];
        mode[0] = ui.chips(new String[]{"عشوائي", "بالترتيب"}, 2, p.sequential() ? 1 : 0, i -> {
            p.setSequential(i == 1);
            mode[0].select(i);
        });
        modeCard.addView(mode[0]);
        modeCard.addView(ui.hint("العشوائي لا يكرر التسجيل نفسه مرتين متتاليتين، وبالترتيب يمرّ على التسجيلات واحدًا بعد الآخر."));
        content.addView(modeCard);

        LinearLayout listCard = ui.card();
        listCard.addView(ui.sectionTitle("تسجيلاتي"));
        tvStatus = ui.text("", 13, t.sub, false);
        tvStatus.setGravity(Gravity.START);
        listCard.addView(tvStatus);
        listBox = new LinearLayout(act);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(listBox, new LinearLayout.LayoutParams(-1, -2));

        TextView add = ui.goldButton("إضافة تسجيل صوتي");
        add.setOnClickListener(v -> act.pickAudio());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.dp(50));
        lp.topMargin = ui.dp(14);
        listCard.addView(add, lp);

        TextView test = ui.outlineButton("جرّب التذكير الآن");
        test.setOnClickListener(v -> {
            if (!SoundPlayer.hasAudio(act)) {
                Toast.makeText(act, "أضف تسجيلًا صوتيًا أولًا", Toast.LENGTH_LONG).show();
            } else {
                stopPlaying();
                SoundPlayer.play(act, "test", null);
                Toast.makeText(act, "جارٍ تشغيل الصوت...", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, ui.dp(46));
        lp2.topMargin = ui.dp(10);
        listCard.addView(test, lp2);
        listCard.addView(ui.hint("اختر تسجيلات بصوت قارئ فصيح للصلاة على النبي (mp3 / m4a / ogg / wav). تُنسخ إلى التطبيق وتعمل بدون إنترنت. الحد الأقصى ١٢ تسجيلًا، ٣٠ ميغابايت لكل تسجيل."));
        content.addView(listCard);
        refreshList();
    }

    private void stopPlaying() {
        SoundPlayer.stopPreview();
        playing = null;
    }

    void refreshList() {
        listBox.removeAllViews();
        List<Tracks.Track> tracks = Tracks.list(act);
        boolean raw = SoundPlayer.hasRaw(act);
        if (tracks.isEmpty()) {
            tvStatus.setText(raw ? "لا توجد تسجيلات مضافة؛ يُستخدم التسجيل المرفق مع التطبيق."
                    : "لم تُضف تسجيلات بعد. أضف تسجيلًا لتسمعه عند كل تذكير.");
        } else {
            tvStatus.setText("عدد التسجيلات: " + HijriUtil.digits(tracks.size()));
        }
        for (int i = 0; i < tracks.size(); i++) {
            final Tracks.Track tr = tracks.get(i);
            LinearLayout row = new LinearLayout(act);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, ui.dp(8), 0, ui.dp(8));
            TextView name = ui.text(HijriUtil.digits(i + 1) + ". " + tr.name, 15, Color.WHITE, false);
            name.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

            final TextView play = ui.pillButton(tr.file.equals(playing) ? "إيقاف" : "تشغيل", ui.t.accent);
            play.setOnClickListener(v -> {
                if (tr.file.equals(playing)) {
                    stopPlaying();
                    refreshList();
                } else {
                    playing = tr.file;
                    SoundPlayer.preview(act, tr.file, () -> {
                        playing = null;
                        refreshList();
                    });
                    refreshList();
                }
            });
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-2, -2);
            pl.setMargins(ui.dp(6), 0, ui.dp(6), 0);
            row.addView(play, pl);

            TextView del = ui.pillButton("حذف", 0xFFFF8A80);
            del.setOnClickListener(v -> {
                if (tr.file.equals(playing)) stopPlaying();
                Tracks.delete(act, tr);
                refreshList();
            });
            row.addView(del);
            listBox.addView(row);
            if (i < tracks.size() - 1) listBox.addView(ui.divider());
        }
    }

    /** نتيجة اختيار ملفات صوتية من الهاتف. */
    void onPicked(final List<android.net.Uri> uris) {
        if (uris.isEmpty()) return;
        Toast.makeText(act, "جارٍ إضافة التسجيل...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int ok = 0, status = Tracks.OK;
            for (android.net.Uri u : uris) {
                int r = Tracks.add(act, u);
                if (r == Tracks.OK) ok++;
                else status = r;
            }
            final int added = ok, last = status;
            act.runOnUiThread(() -> {
                String msg;
                if (added > 0 && last == Tracks.OK) msg = "تمت إضافة " + HijriUtil.digits(added) + " تسجيل";
                else if (last == Tracks.TOO_MANY) msg = "وصلت للحد الأقصى من التسجيلات (١٢)";
                else if (last == Tracks.TOO_BIG) msg = "الملف كبير جدًا (الحد ٣٠ ميغابايت)";
                else msg = added > 0 ? "أُضيف بعض التسجيلات فقط" : "تعذّر قراءة الملف";
                Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
                refreshList();
            });
        }).start();
    }

    @Override
    void onShow() {
        refreshList();
    }

    @Override
    void onHide() {
        stopPlaying();
    }
}
