package com.alikhwa.thikrni;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** مكتبة التسجيلات الصوتية (عدة تسجيلات، عشوائي أو بالترتيب). */
public final class Tracks {
    private Tracks() {}

    public static final int MAX_TRACKS = 12;
    public static final long MAX_BYTES = 30L * 1024 * 1024;
    public static final int OK = 1, FAIL = 0, TOO_MANY = -1, TOO_BIG = -2;

    private static final Random RND = new Random();

    public static final class Track {
        public final File file;
        public final String name;

        Track(File f, String n) {
            file = f;
            name = n;
        }
    }

    private static File dir(Context c) {
        File d = new File(c.getApplicationContext().getFilesDir(), "tracks");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static List<Track> list(Context c) {
        Prefs p = new Prefs(c);
        File[] fs = dir(c).listFiles();
        List<Track> out = new ArrayList<>();
        if (fs == null) return out;
        Arrays.sort(fs, Comparator.comparingLong(File::lastModified));
        int i = 1;
        for (File f : fs) {
            if (f.length() <= 0) continue;
            String n = p.trackName(f.getName());
            if (n == null || n.isEmpty()) n = "تسجيل " + HijriUtil.digits(i);
            out.add(new Track(f, n));
            i++;
        }
        return out;
    }

    public static boolean hasAny(Context c) {
        return !list(c).isEmpty();
    }

    public static int add(Context c, Uri uri) {
        if (list(c).size() >= MAX_TRACKS) return TOO_MANY;
        File f = null;
        try {
            f = File.createTempFile("t_", ".snd", dir(c));
            long total = 0;
            try (InputStream in = c.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(f)) {
                if (in == null) {
                    f.delete();
                    return FAIL;
                }
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_BYTES) {
                        out.close();
                        f.delete();
                        return TOO_BIG;
                    }
                    out.write(buf, 0, n);
                }
            }
            String name = displayName(c, uri);
            if (name != null) new Prefs(c).setTrackName(f.getName(), name);
            return OK;
        } catch (Exception e) {
            if (f != null) f.delete();
            return FAIL;
        }
    }

    private static String displayName(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) {
                    String n = cur.getString(i);
                    if (n != null) {
                        int dot = n.lastIndexOf('.');
                        if (dot > 0) n = n.substring(0, dot);
                        return n;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void delete(Context c, Track t) {
        t.file.delete();
        new Prefs(c).removeTrackName(t.file.getName());
    }

    /** ينقل التسجيل المخصص من الإصدار القديم إلى المكتبة. */
    public static void migrateLegacy(Context c) {
        File old = new File(c.getApplicationContext().getFilesDir(), "salawat_custom");
        if (old.exists() && old.length() > 0) {
            File dst = new File(dir(c), "t_legacy.snd");
            if (old.renameTo(dst)) new Prefs(c).setTrackName(dst.getName(), "تسجيلي السابق");
        } else if (old.exists()) {
            old.delete();
        }
    }

    /** يختار تسجيلًا حسب الوضع (عشوائي أو بالترتيب). */
    public static File pick(Context c) {
        List<Track> l = list(c);
        if (l.isEmpty()) return null;
        Prefs p = new Prefs(c);
        int n = l.size();
        int i;
        if (p.sequential()) {
            i = p.seqIndex() % n;
            p.setSeqIndex((i + 1) % n);
        } else if (n == 1) {
            i = 0;
        } else {
            do {
                i = RND.nextInt(n);
            } while (i == p.lastIndex());
            p.setLastIndex(i);
        }
        return l.get(i).file;
    }
}
