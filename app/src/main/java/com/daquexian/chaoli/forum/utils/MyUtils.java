package com.daquexian.chaoli.forum.utils;

import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Post;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * various strange things
 * Created by jianhao on 16-10-2.
 */

public class MyUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "MyUtils";

    /**
     * 针对可能有其他帖子被顶到最上方，导致下一页的主题帖与这一页的主题帖有重合的现象
     * @param A 已有的主题帖列表
     * @param B 下一页主题帖列表
     * @return 合成后的新列表的长度
     */
    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B) {
        return expandUnique(A, B, true);
    }

    /**
     * a > b 表示 a 排在 b 后面
     */
    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B, Boolean addBehind) {
        return expandUnique(A, B, addBehind, false);
    }

    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B, Boolean addBehind, Boolean reversed) {
        int lenA = A.size();
        if (lenA == 0) A.addAll(B);
        else {
            if (addBehind) {
                int i;
                for (i = 0; i < B.size(); i++)
                    if ((!reversed && B.get(i).compareTo(A.get(A.size() - 1)) > 0) || (reversed && B.get(i).compareTo(A.get(A.size() - 1)) < 0))
                        break;
                A.addAll(B.subList(i, B.size()));
            } else {
                int i;
                for (i = 0; i < B.size(); i++)
                    if ((!reversed && B.get(i).compareTo(A.get(0)) >= 0) || (reversed && B.get(i).compareTo(A.get(0)) <= 0))
                        break;
                A.addAll(0, B.subList(0, i));
            }
        }
        return A.size();
    }

    public static <T> List reverse(List<T> list) {
        List<T> reversed = new ArrayList<>();
        for (T item : list) {
            reversed.add(0, item);
        }
        return reversed;
    }

    public static String getAttachmentImageUrl(Post.Attachment attachment) {
        return Constants.ATTACHMENT_IMAGE_URL + attachment.getAttachmentId() + attachment.getSecret();
    }

    public static String getAttachmentFileUrl(Post.Attachment attachment) throws UnsupportedEncodingException{
        return "https://chaoli.club/index.php/attachment/" + attachment.getAttachmentId() + "_" + URLEncoder.encode(attachment.getFilename(), "UTF-8");
    }
}
