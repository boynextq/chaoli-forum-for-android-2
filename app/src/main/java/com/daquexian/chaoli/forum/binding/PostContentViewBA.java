package com.daquexian.chaoli.forum.binding;

import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.flexiblerichtextview.FlexibleRichTextView;

/**
 * Created by jianhao on 16-9-27.
 */

public class PostContentViewBA {
    @BindingAdapter("app:post")
    public static void setPost(FlexibleRichTextView flexibleRichTextView, Post post) {
        /**
         * JLaTeXMath seems not support \begin{align*} and so on
         */
        post.content = post.content.replaceAll("\\\\begin\\{align\\*?\\}", "\\\\begin{flalign}")
                .replaceAll("\\\\end\\{align\\*?\\}", "\\\\end{flalign}");
        flexibleRichTextView.setText(post.getContent(), post.getAttachments());
    }

    @BindingAdapter("app:listener")
    public static void setListener(FlexibleRichTextView postContentView, FlexibleRichTextView.OnViewClickListener onViewClickListener) {
        postContentView.setOnClickListener(onViewClickListener);
    }
}