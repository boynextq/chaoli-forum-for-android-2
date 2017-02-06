package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kbiakov.codeview.CodeView;

/**
 * 包含QuoteView和OnlineImgTextView
 * 用于显示帖子
 * Created by jianhao on 16-8-26.
 */
public class PostContentView extends LinearLayout {
    private final static String TAG = "PostContentView";
    private final static String QUOTE_START_TAG = "[quote";
    private final static Pattern QUOTE_START_PATTERN = Pattern.compile("\\[quote(=(\\d+?):@(.*?))?]");
    private final static String QUOTE_END_TAG = "[/quote]";
    private final static String CODE_START_TAG = "[code]";
    private final static String CODE_END_TAG = "[/code]";
    private final static Pattern ATTACHMENT_PATTERN = Pattern.compile("\\[attachment:(.*?)]");
    private final static String[] TAGS = {QUOTE_START_TAG, QUOTE_END_TAG, CODE_START_TAG, CODE_END_TAG};

    private Context mContext;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Post mPost;
    private int mConversationId;
    private List<Post.Attachment> mAttachmentList;

    private Boolean mShowQuote = true;

    public PostContentView(Context context) {
        super(context);
        init(context);
    }
    public PostContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }
    public PostContentView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context);
    }


    /**
     * Recursive descent
     *
     * fullContent  -> quote attachment
     *
     * quote        -> LaTeX QUOTE_START content QUOTE_END quote
     *                  | content
     *
     * content      -> LaTeX CODE_START code CODE_END content
     *                  | LaTeX
     *
     * @param post the post
     */
    public void setPost(Post post) {
        removeAllViews();
        mPost = post;
        mAttachmentList = post.getAttachments();
        List<Post.Attachment> attachmentList = new ArrayList<>(post.getAttachments());
        String content = post.getContent();
        content = content.replaceAll("\u00AD", "");
        fullContent(content, attachmentList);
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void fullContent(String str, List<Post.Attachment> attachmentList) {
        Matcher attachmentMatcher = ATTACHMENT_PATTERN.matcher(str);
        while (attachmentMatcher.find()) {
            String id = attachmentMatcher.group(1);
            for (int i = attachmentList.size() - 1; i >= 0; i--) {
                Post.Attachment attachment = attachmentList.get(i);
                if (attachment.getAttachmentId().equals(id)) {
                    attachmentList.remove(i);
                }
            }
        }

        quote(str);

        SpannableStringBuilder builder = new SpannableStringBuilder();

        boolean isImage = false;
        for (final Post.Attachment attachment : attachmentList) {
            for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
                if (attachment.getFilename().endsWith(image_ext)) {
                    isImage = true;
                    break;
                }
            }

            if (isImage) {
                String url = MyUtils.getAttachmentImageUrl(attachment);
                final ImageView imageView = new ImageView(mContext);

                LinearLayout.LayoutParams layoutParams = new LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                imageView.setLayoutParams(layoutParams);
                imageView.setAdjustViewBounds(true);
                // imageView.setScaleType(ImageView.ScaleType.FIT_START);
                // imageView.setMaxWidth(Constants.MAX_IMAGE_WIDTH);
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + url);
                final ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray));
                imageView.setImageDrawable(colorDrawable);
                Glide.with(mContext)
                        .load(url)
                        .into(new SimpleTarget<GlideDrawable>() {
                            @Override
                            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                imageView.setImageDrawable(resource);
                            }
                        });
                addView(imageView);
            } else {
                    int start = builder.length();
                    builder.append(attachment.getFilename());
                    builder.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick() called with: view = [" + view + "]");
                            MyUtils.downloadAttachment(mContext, attachment);
                            /* try {
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(attUrl));
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Constants.APP_DIR_NAME + "/" + attachment.getFilename());
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
                                request.allowScanningByMediaScanner();// if you want to be available from media players
                                DownloadManager manager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
                                manager.enqueue(request);
                            } catch (SecurityException e) {
                                // in the case of user rejects the permission request
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(attUrl));
                                mContext.startActivity(intent);
                            } */
                        }
                    }, start, start + attachment.getFilename().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    builder.append("\n\n");
            }
        }

        if (builder.length() > 0) {
            TextView textView = new TextView(mContext);
            textView.setText(builder);
            /**
             * make links clickable
             */
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            addView(textView);
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void quote(String str) {
        int quoteStartPos, quoteEndPos = 0;
        String piece, quote;
        Matcher quoteMatcher = QUOTE_START_PATTERN.matcher(str);
        while (quoteEndPos != -1 && quoteMatcher.find(quoteEndPos)) {
            quoteStartPos = quoteMatcher.start();

            if (quoteEndPos != quoteStartPos) {
                piece = str.substring(quoteEndPos, quoteStartPos);
                content(piece);
            }
            quoteEndPos = pairedIndex(str, quoteStartPos, QUOTE_START_TAG, QUOTE_END_TAG);

            if (quoteEndPos == -1) {
                piece = str.substring(quoteStartPos);
                content(piece);
                quoteEndPos = str.length();
            } else if (mShowQuote) {
                quote = str.substring(quoteStartPos + quoteMatcher.group().length(), quoteEndPos - QUOTE_END_TAG.length());
                addQuoteView(quote);
            } else {
                addQuoteView("...");
            }
        }
        if (quoteEndPos != str.length()) {
            piece = str.substring(quoteEndPos);
            content(piece);
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void content(String str) {
        int codeStartPos, codeEndPos = 0;
        String piece, code;
        while (codeEndPos != -1 && (codeStartPos = str.indexOf(CODE_START_TAG, codeEndPos)) >= 0) {//codeMatcher.find(codeEndPos)) {
            if (codeEndPos != codeStartPos) {
                piece = str.substring(codeEndPos, codeStartPos);
                LaTeX(piece);
            }
            codeEndPos = pairedIndex(str, codeStartPos, CODE_START_TAG, CODE_END_TAG);
            //codeEndPos = content.indexOf(CODE_END_TAG, codeStartPos) + CODE_END_TAG.length();
            if (codeEndPos == -1) {
                piece = str.substring(codeStartPos);
                LaTeX(piece);
                codeEndPos = str.length();
            } else {
                code = str.substring(codeStartPos + CODE_START_TAG.length(), codeEndPos - CODE_END_TAG.length());
                code(code);
            }
        }
        if (codeEndPos != str.length()) {
            piece = str.substring(codeEndPos);
            LaTeX(piece);
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void LaTeX(String content) {
        content = removeTags(content);
        OnlineImgTextView onlineImgTextView;
        onlineImgTextView = new OnlineImgTextView(mContext, mAttachmentList);
        onlineImgTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        onlineImgTextView.setText(content);
        /**
         * make links clickable
         */
        onlineImgTextView.setMovementMethod(LinkMovementMethod.getInstance());
        addView(onlineImgTextView);
        //laTeXtView.setOnLongClickListener();
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void code(String str) {
        str = removeTags(str);
        CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
        codeView.setCode(str);
        addView(codeView);
    }

    private int pairedIndex(String str, int from, String startTag, String endTag) {
        int times = 0;
        for (int i = from; i < str.length(); i++) {
            if (str.substring(i).startsWith(startTag)) {
                times++;
            } else if (str.substring(i).startsWith(endTag)) {
                times--;
                if (times == 0) {
                    return i + endTag.length();
                }
            }
        }
        return -1;
    }

    private void addQuoteView(String content) {
        QuoteView quoteView = new QuoteView(mContext, mAttachmentList);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = 20;
        quoteView.setLayoutParams(params);
        quoteView.setOrientation(VERTICAL);
        quoteView.setText(content);
        addView(quoteView);
    }

    private String removeTags(String str) {
        for (String tag : TAGS) {
            str = str.replace(tag, "");
        }

        return str;
    }

    public void init(Context context) {
        mContext = context;
        removeAllViews();
    }

    public int getConversationId() {
        return mConversationId;
    }

    public void setConversationId(int mConversationId) {
        this.mConversationId = mConversationId;
    }

    @SuppressWarnings("unused")
    public void showQuote(Boolean showQuote) {
        mShowQuote = showQuote;
    }
}
