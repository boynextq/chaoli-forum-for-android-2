package com.daquexian.chaoli.forum.meta;

import android.graphics.Color;
import android.util.Log;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by daquexian on 17-2-9.
 */

public class Parser4 {
    private static final String TAG = "SFXParser3";

    private static final String[] iconStrs = new String[]{"/:)", "/:D", "/^b^", "/o.o", "/xx", "/#", "/))", "/--", "/TT", "/==",
            "/.**", "/:(", "/vv", "/$$", "/??", "/:/", "/xo", "/o0", "/><", "/love",
            "/...", "/XD", "/ii", "/^^", "/<<", "/>.", "/-_-", "/0o0", "/zz", "/O!O",
            "/##", "/:O", "/<", "/heart", "/break", "/rose", "/gift", "/bow", "/moon", "/sun",
            "/coin", "/bulb", "/tea", "/cake", "/music", "/rock", "/v", "/good", "/bad", "/ok",
            "/asnowwolf-smile", "/asnowwolf-laugh", "/asnowwolf-upset", "/asnowwolf-tear",
            "/asnowwolf-worry", "/asnowwolf-shock", "/asnowwolf-amuse"};
    private static final int[] icons = new int[]{R.drawable.emoticons__0050_1, R.drawable.emoticons__0049_2, R.drawable.emoticons__0048_3, R.drawable.emoticons__0047_4,
            R.drawable.emoticons__0046_5, R.drawable.emoticons__0045_6, R.drawable.emoticons__0044_7, R.drawable.emoticons__0043_8, R.drawable.emoticons__0042_9,
            R.drawable.emoticons__0041_10, R.drawable.emoticons__0040_11, R.drawable.emoticons__0039_12, R.drawable.emoticons__0038_13, R.drawable.emoticons__0037_14,
            R.drawable.emoticons__0036_15, R.drawable.emoticons__0035_16, R.drawable.emoticons__0034_17, R.drawable.emoticons__0033_18, R.drawable.emoticons__0032_19,
            R.drawable.emoticons__0031_20, R.drawable.emoticons__0030_21, R.drawable.emoticons__0029_22, R.drawable.emoticons__0028_23, R.drawable.emoticons__0027_24,
            R.drawable.emoticons__0026_25, R.drawable.emoticons__0025_26, R.drawable.emoticons__0024_27, R.drawable.emoticons__0023_28, R.drawable.emoticons__0022_29,
            R.drawable.emoticons__0021_30, R.drawable.emoticons__0020_31, R.drawable.emoticons__0019_32, R.drawable.emoticons__0018_33, R.drawable.emoticons__0017_34,
            R.drawable.emoticons__0016_35, R.drawable.emoticons__0015_36, R.drawable.emoticons__0014_37, R.drawable.emoticons__0013_38, R.drawable.emoticons__0012_39,
            R.drawable.emoticons__0011_40, R.drawable.emoticons__0010_41, R.drawable.emoticons__0009_42, R.drawable.emoticons__0008_43, R.drawable.emoticons__0007_44,
            R.drawable.emoticons__0006_45, R.drawable.emoticons__0005_46, R.drawable.emoticons__0004_47, R.drawable.emoticons__0003_48, R.drawable.emoticons__0002_49,
            R.drawable.emoticons__0001_50, R.drawable.asonwwolf_smile, R.drawable.asonwwolf_laugh, R.drawable.asonwwolf_upset, R.drawable.asonwwolf_tear,
            R.drawable.asonwwolf_worry, R.drawable.asonwwolf_shock, R.drawable.asonwwolf_amuse};

    private static int mTokenIndex;

    public static abstract class TOKEN implements Comparable<TOKEN> {
        int position;
        int length;
        CharSequence value;

        @Override
        public int compareTo(@NotNull TOKEN token) {
            return ((Integer) position).compareTo(token.position);
        }
    }

    public static class PLAIN extends TOKEN {
        PLAIN(int position, CharSequence value) {
            this.position = position;
            this.value = value;
            this.length = value.length();
        }
    }

    public static class COLOR_START extends TOKEN {
        int color;

        COLOR_START(int position, String value, int length) {
            this.position = position;
            this.value = value;
            try {
                this.color = Color.parseColor(value);
            } catch (IllegalArgumentException e) {
                this.color = Color.BLACK;
            }
            this.length = length;
        }
    }

    public static class COLOR_END extends TOKEN {
        COLOR_END(int position, String value) {
            this.position = position;
            this.value = value;
            this.length = value.length();
        }
    }

    public static class URL_START extends TOKEN {
        String url;
        URL_START(int position, String url, String value) {
            this.position = position;
            this.url = url;
            this.value = value;
            this.length = value.length();
        }
    }

    public static class URL_END extends TOKEN {
        URL_END(int position) {
            this.position = position;
            this.value = "[/url]";
            this.length = 6;
        }
    }

    public static class CURTAIN_START extends TOKEN {
        CURTAIN_START(int position) {
            this.position = position;
            this.value = "[curtain]";
            this.length = 9;
        }
    }

    public static class CURTAIN_END extends TOKEN {
        CURTAIN_END(int position) {
            this.position = position;
            this.value = "[/curtain]";
            this.length = 10;
        }

    }

    public static class BOLD_START extends TOKEN {
        BOLD_START(int position) {
            this.position = position;
            this.value = "[b]";
            this.length = 3;
        }
    }

    public static class BOLD_END extends TOKEN {

        BOLD_END(int position) {
            this.position = position;
            this.value = "[/b]";
            this.length = 4;
        }
    }

    public static class ITALIC_START extends TOKEN {
        ITALIC_START(int position) {
            this.position = position;
            this.value = "[i]";
            this.length = 3;
        }
    }

    public static class ITALIC_END extends TOKEN {
        ITALIC_END(int position) {
            this.position = position;
            this.value = "[/i]";
            this.length = 4;
        }
    }

    public static class UNDERLINE_START extends TOKEN {
        UNDERLINE_START(int position) {
            this.position = position;
            this.value = "[u]";
            this.length = 3;
        }
    }

    public static class UNDERLINE_END extends TOKEN {
        UNDERLINE_END(int position) {
            this.position = position;
            this.value = "[/u]";
            this.length = 4;
        }
    }

    public static class DELETE_START extends TOKEN {
        DELETE_START(int position) {
            this.position = position;
            this.value = "[s]";
            this.length = 3;
        }
    }

    public static class DELETE_END extends TOKEN {
        DELETE_END(int position) {
            this.position = position;
            this.value = "[/s]";
            this.length = 4;
        }
    }

    public static class CENTER_START extends TOKEN {
        CENTER_START(int position) {
            this.position = position;
            this.value = "[center]";
            this.length = 8;
        }
    }

    public static class CENTER_END extends TOKEN {
        CENTER_END(int position) {

            this.position = position;
            this.value = "[/center]";
            this.length = 9;
        }
    }

    public static class TITLE_START extends TOKEN {
        TITLE_START(int position) {
            this.position = position;
            this.value = "[h]";
            this.length = 3;
        }
    }

    public static class TITLE_END extends TOKEN {
        TITLE_END(int position) {
            this.position = position;
            this.value = "[/h]";
            this.length = 4;
        }
    }

    public static class ATTACHMENT extends TOKEN {
        Post.Attachment attachment;
        ATTACHMENT(int position, Post.Attachment attachment, String value) {
            this.position = position;
            this.attachment = attachment;
            this.value = value;
            this.length = value.length();
        }
    }

    public static class ICON extends TOKEN {
        int iconId;
        ICON(int position, String iconStr, int iconId) {
            this.position = position;
            this.value = iconStr;
            this.iconId = iconId;
            this.length = iconStr.length();
        }
    }

    public static class FORMULA extends TOKEN {
        String content;
        int contentStart;
        FORMULA(int position, String content, int contentStart, String value) {
            this.position = position;
            this.content = content;
            this.contentStart = contentStart;
            /*
             * remove all newline character to avoid the ImageSpan shows multiple times when
             * formula content stretches over multiple lines.
             */
            this.value = value.replaceAll("[\n\r]", "");
            this.length = value.length();
        }
    }

    public static class CODE_START extends TOKEN {
        CODE_START(int position) {
            this.position = position;
            this.value = "[code]";
            this.length = 6;
        }
    }

    public static class CODE_END extends TOKEN {
        CODE_END(int position) {
            this.position = position;
            this.value = "[/code]";
            this.length = 7;
        }
    }

    public static class QUOTE_START extends TOKEN {
        String quotedUsername;
        String postId;
        QUOTE_START(int position, String value, String quotedUsername, String postId) {
            this.position = position;
            this.value = value;
            this.length = value.length();
            this.quotedUsername = quotedUsername;
            this.postId = postId;
        }
    }

    public static class QUOTE_END extends TOKEN {
        QUOTE_END(int position) {
            this.position = position;
            this.value = "[/quote]";
            this.length = value.length();
        }
    }

    public static class IMAGE extends TOKEN {
        String url;
        int size;

        IMAGE(int position, String url, String value) {
            this(position, url, value, -1);
        }

        IMAGE(int position, String url, String value, int size) {
            this.position = position;
            this.url = url;
            this.value = value;
            this.length = value.length();
            this.size = size;
        }
    }

    public static class TABLE extends TOKEN {
        static final int LEFT = 0, RIGHT = 1, CENTER = 2;
        TABLE(int position, String content) {
            this.position = position;
            this.value = content;
            this.length = content.length();
        }
    }

    public static class END extends TOKEN {
        END(int position) {
            this.position = position;
        }
    }

    private static final Pattern COLOR_START_REG1 = Pattern.compile("(?i)\\[c=(.*?)]");
    private static final Pattern COLOR_START_REG2 = Pattern.compile("(?i)\\[color=(.*?)]");
    private static final Pattern COLOR_END_REG1 = Pattern.compile("(?i)\\[/c]");
    private static final Pattern COLOR_END_REG2 = Pattern.compile("(?i)\\[/color]");
    private static final Pattern URL_START_REG = Pattern.compile("(?i)\\[url=(.*?)]");
    private static final Pattern URL_END_REG = Pattern.compile("(?i)\\[/url]");
    private static final Pattern CURTAIN_START_REG = Pattern.compile("(?i)\\[curtain]");
    private static final Pattern CURTAIN_END_REG = Pattern.compile("(?i)\\[/curtain]");
    private static final Pattern BOLD_START_REG = Pattern.compile("(?i)\\[b]");
    private static final Pattern BOLD_END_REG = Pattern.compile("(?i)\\[/b]");
    private static final Pattern ITALIC_START_REG = Pattern.compile("(?i)\\[i]");
    private static final Pattern ITALIC_END_REG = Pattern.compile("(?i)\\[/i]");
    private static final Pattern UNDERLINE_START_REG = Pattern.compile("(?i)\\[u]");
    private static final Pattern UNDERLINE_END_REG = Pattern.compile("(?i)\\[/u]");
    private static final Pattern DELETE_START_REG = Pattern.compile("(?i)\\[s]");
    private static final Pattern DELETE_END_REG = Pattern.compile("(?i)\\[/s]");
    private static final Pattern CENTER_START_REG = Pattern.compile("(?i)\\[center]");
    private static final Pattern CENTER_END_REG = Pattern.compile("(?i)\\[/center]");
    private static final Pattern TITLE_START_REG = Pattern.compile("(?i)\\[h]");
    private static final Pattern TITLE_END_REG = Pattern.compile("(?i)\\[/h]");
    private static final Pattern CODE_START_REG = Pattern.compile("(?i)\\[code]");
    private static final Pattern CODE_END_REG = Pattern.compile("(?i)\\[/code]");
    private static final Pattern QUOTE_START_REG = Pattern.compile("(?i)\\[quote(?:=(\\d+?):@(.*?))?]");
    private static final Pattern QUOTE_END_REG = Pattern.compile("(?i)\\[/quote]");
    private static final Pattern ATTACHMENT_REG = Pattern.compile("(?i)\\[attachment:(.*?)]");

    private static final Pattern FORMULA_REG1 = Pattern.compile("(?i)\\$\\$?((.|\\n)+?)\\$\\$?");
    private static final Pattern FORMULA_REG2 = Pattern.compile("(?i)\\\\[(\\[]((.|\\n)*?)\\\\[\\])]");
    private static final Pattern FORMULA_REG3 = Pattern.compile("(?i)\\[tex]((.|\\n)*?)\\[/tex]");
    private static final Pattern FORMULA_REG4 = Pattern.compile("(?i)\\\\begin\\{.*?\\}(.|\\n)*?\\\\end\\{.*?\\}");
    // private static final Pattern FORMULA_REG5 = Pattern.compile("(?i)\\$\\$(.+?)\\$\\$");
    private static final Pattern[] PATTERNS = {FORMULA_REG1, FORMULA_REG2, FORMULA_REG3, FORMULA_REG4};

    private static final Pattern IMG_REG = Pattern.compile("(?i)\\[img(=\\d+)?](.*?)\\[/img]");

    private static final Pattern TABLE_REG = Pattern.compile("(?:\\n|^)( *\\|.+\\| *\\n)??( *\\|(?: *:?----*:? *\\|)+ *\\n)((?: *\\|.+\\| *(?:\\n|$))+)");

    private static List<TOKEN> mTokenList = new ArrayList<>();

    public static List<TOKEN> tokenizer(CharSequence text, List<Post.Attachment> attachmentList) {
        mTokenList = new ArrayList<>();

        Pattern pattern;
        Matcher matcher;
        int start;

        pattern = COLOR_START_REG1;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new COLOR_START(matcher.start(), matcher.group(1), matcher.group().length()));

        }

        pattern = COLOR_START_REG2;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new COLOR_START(matcher.start(), matcher.group(1), matcher.group().length()));

        }

        pattern = URL_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            String url = matcher.group(1).toLowerCase();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            mTokenList.add(new URL_START(matcher.start(), url, matcher.group()));

        }

        pattern = URL_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new URL_END(matcher.start()));

        }

        pattern = CENTER_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new CENTER_START(matcher.start()));

        }

        pattern = CENTER_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new CENTER_END(matcher.start()));

        }

        pattern = CURTAIN_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new CURTAIN_START(matcher.start()));

        }
        pattern = CURTAIN_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new CURTAIN_END(matcher.start()));

        }

        pattern = ATTACHMENT_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            String id = matcher.group(1);
            if (attachmentList != null) {
                for (Post.Attachment attachment : attachmentList) {
                    if (attachment.getAttachmentId().equals(id)) {
                        mTokenList.add(new ATTACHMENT(matcher.start(), attachment, matcher.group()));
                        break;
                    }
                }
            }
        }

        pattern = COLOR_END_REG1;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new COLOR_END(matcher.start(), matcher.group()));
        }

        pattern = COLOR_END_REG2;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new COLOR_END(matcher.start(), matcher.group()));
        }

        pattern = ITALIC_START_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new ITALIC_START(matcher.start()));
        }

        pattern = ITALIC_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new ITALIC_END(matcher.start()));

        }

        pattern = BOLD_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new BOLD_START(matcher.start()));

        }

        pattern = BOLD_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new BOLD_END(matcher.start()));

        }

        pattern = DELETE_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new DELETE_START(matcher.start()));

        }

        pattern = DELETE_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new DELETE_END(matcher.start()));

        }

        pattern = UNDERLINE_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new UNDERLINE_START(matcher.start()));

        }

        pattern = UNDERLINE_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new UNDERLINE_END(matcher.start()));

        }

        pattern = TITLE_START_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new TITLE_START(matcher.start()));

        }

        pattern = TITLE_END_REG;
        matcher = pattern.matcher(text);


        while (matcher.find()) {
            mTokenList.add(new TITLE_END(matcher.start()));
        }

        pattern = CODE_START_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new CODE_START(matcher.start()));
        }

        pattern = CODE_END_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new CODE_END(matcher.start()));
        }

        pattern = QUOTE_START_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new QUOTE_START(matcher.start(), matcher.group(), matcher.group(1), matcher.group(2)));
        }

        pattern = QUOTE_END_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new QUOTE_END(matcher.start()));
        }

        String str = text.toString();
        for (int j = 0; j < iconStrs.length; j++) {
            int from = 0;
            String iconStr = iconStrs[j];
            while ((from = str.indexOf(iconStr, from)) >= 0) {
                if (("/<".equals(iconStr) && str.substring(from).startsWith("/<<") || ("/#".equals(iconStr) && str.substring(from).startsWith("/##")))) {
                    from++;
                    continue;
                }

                /**
                 * only show icons when iconStr is surrounded by spaces
                 */
                if (iconStr.equals("/^^")) Log.d(TAG, "parse: " + str.trim().length() + ", " + iconStr.length() + ", " + (from + iconStr.length()) + ", " + str.length());
                if (str.trim().length() == iconStr.length() ||
                        ((from == 0 || ' ' == str.charAt(from - 1)) && (from + iconStr.length() == str.length() || ' ' == str.charAt(from + iconStr.length()) || '\n' == str.charAt(from + iconStr.length())))) {
                    mTokenList.add(new ICON(from, iconStr, icons[j]));
                }
                from += iconStr.length();
            }
        }

        pattern = IMG_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                mTokenList.add(new IMAGE(matcher.start(), matcher.group(2), matcher.group(), Integer.valueOf(matcher.group(1).substring(1))));
            } catch (Exception e) {
                mTokenList.add(new IMAGE(matcher.start(), matcher.group(2), matcher.group()));
            }
        }

        pattern = TABLE_REG;
        matcher = pattern.matcher(text);

        while (matcher.find()) {
            mTokenList.add(new TABLE(matcher.start(), matcher.group()));
        }


        final int[] indexInRegex = {1, 1, 1, 0, 1};
        Matcher[] matchers = new Matcher[PATTERNS.length];
        for (int i = 0; i < PATTERNS.length; i++) {
            matchers[i] = PATTERNS[i].matcher(text);
        }

        for (int i = 0; i < matchers.length; i++) {
            matcher = matchers[i];
            int index = indexInRegex[i];

            String content, value;
            int contentStart;

            while (matcher.find()) {
                start = matcher.start();
                content = matcher.group(index);
                value = matcher.group();

                contentStart = matcher.start(index);

                mTokenList.add(new FORMULA(start, content, contentStart - start, value));
            }
        }

        Collections.sort(mTokenList);

        for (int i = 0; i < mTokenList.size(); i++) {
            TOKEN token = mTokenList.get(i);

            if (token instanceof TABLE) {
                for (int j = 0; j < mTokenList.size(); j++) {
                    TOKEN token1 = mTokenList.get(j);

                    if (token1.position >= token.position + token.length) {
                        break;
                    }

                    if (token1.position > token.position) {
                        mTokenList.remove(j);
                        j--;
                    }
                }
            }
        }

        mTokenList.add(new END(text.length()));

        start = 0;
        for (int i = 0; i < mTokenList.size(); i++) {
            TOKEN token = mTokenList.get(i);
            if (token.position > start) {
                mTokenList.add(i, new PLAIN(start, text.subSequence(start, token.position)));
                i++;
            }
            start = token.position + token.length;
        }

        return mTokenList;
    }


}
