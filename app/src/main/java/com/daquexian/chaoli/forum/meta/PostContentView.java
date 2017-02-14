package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.parse.MyImageView;
import com.daquexian.chaoli.forum.meta.parse.TextWithFormula;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kbiakov.codeview.CodeView;

import static com.daquexian.chaoli.forum.meta.Parser4.*;

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
    private OnViewClickListener mOnViewClickListener;

    private List<Parser4.TOKEN> mTokenList;
    private int mTokenIndex;

    private SpannableStringBuilder mBuilder = new SpannableStringBuilder();

    private Boolean mShowQuote = true;

    public PostContentView(Context context) {
        super(context);
        init(context);
    }

    @SuppressWarnings("unused")
    public PostContentView(Context context, OnViewClickListener onViewClickListener) {
        super(context);
        init(context, onViewClickListener);
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
     * fullContent  -> codeBlock attachment
     *
     * codeBlock    -> quote CODE_START code CODE_END codeBlock
     *                  | quote
     *
     * quote        -> table QUOTE_START codeBlock QUOTE_END quote      // due to tech limit, codeBlock here is actually LaTeX
     *                  | table
     *
     * table        -> LaTeX TABLE_START codeBlock TABLE_END table
     *                  | LaTeX
     *
     * LaTeX        -> plainText img LaTeX plainText
     *
     * @param post the post
     */
    public void setPost(Post post) {
        removeAllViews();
        mPost = post;
        List<Post.Attachment> attachmentList = new ArrayList<>(post.getAttachments());
        String content = post.getContent();
        setText(content, attachmentList);
    }

    public void setText(String text, List<Post.Attachment> attachmentList) {
        text = text.replaceAll("\u00AD", "");

        mAttachmentList = attachmentList;
        mTokenList = tokenizer(text, mAttachmentList);
        resetTokenIndex();
        // fullContent(content, attachmentList);
        List<Object> result = until(END.class);

        for (Post.Attachment att : mAttachmentList) {
            append(result, attachment(att));
        }

        for (final Object o : result) {
            if (o instanceof TextWithFormula) {
                final TextWithFormula builder = (TextWithFormula) o;
                List<TextWithFormula.Formula> formulas = builder.getFormulas();

                final TextView textView = new TextView(mContext);
                textView.setText((TextWithFormula) o);
                for (final TextWithFormula.Formula formula : formulas) {
                    Glide.with(mContext)
                            .load(Constants.FORMULA_SITE + formula.content)
                            .asBitmap()
                            .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                            .into(new SimpleTarget<Bitmap>()
                            {
                                @Override
                                public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
                                {
                                    final int HEIGHT_THRESHOLD = 60;
                                    // post to avoid ConcurrentModificationException, from https://github.com/bumptech/glide/issues/375
                                    textView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Bitmap newImage;
                                            if (resource.getWidth() > Constants.MAX_IMAGE_WIDTH) {
                                                int newHeight = resource.getHeight() * Constants.MAX_IMAGE_WIDTH / resource.getWidth();
                                                newImage = Bitmap.createScaledBitmap(resource, Constants.MAX_IMAGE_WIDTH, newHeight, true);
                                            } else {
                                                newImage = resource;
                                            }

                                            if(newImage.getHeight() > HEIGHT_THRESHOLD) {
                                                builder.setSpan(new ImageSpan(mContext, newImage), formula.start, formula.end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                            } else {
                                                builder.setSpan(new CenteredImageSpan(mContext, resource), formula.start, formula.end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                            }

                                            textView.setText(builder);
                                            // retrieveFormulaOnlineImg(formulaList, view, builder, i + 1, start);
                                        }
                                    });
                                }
                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    super.onLoadFailed(e, errorDrawable);
                                    if (e != null) e.printStackTrace();
                                }
                            });
                }
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                myAddView(textView);
            } else if (o instanceof CodeView) {
                myAddView((CodeView) o);
            } else if (o instanceof ImageView) {
                myAddView((ImageView) o);
            } else if (o instanceof HorizontalScrollView) {
                myAddView((HorizontalScrollView) o);
            } else if (o instanceof QuoteView) {
                myAddView((QuoteView) o);
            }
        }
    }

    private void myAddView(View view) {
        if (view instanceof MyImageView && ((MyImageView) view).centered) {
            // TODO: 17-2-13 any more efficient way?
            RelativeLayout rl = new RelativeLayout(mContext);
            RelativeLayout.LayoutParams rlLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlLp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            rl.addView(view);
            rl.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(rl);
        } else {
            addView(view);
        }
    }

    private void resetTokenIndex() {
        mTokenIndex = 0;
    }

    private final Class[] start = {CENTER_START.class, BOLD_START.class,
            ITALIC_START.class, UNDERLINE_START.class, DELETE_START.class,
            CURTAIN_START.class, TITLE_START.class};

    private final Class[] end = {CENTER_END.class, BOLD_END.class, ITALIC_END.class,
            UNDERLINE_END.class, DELETE_END.class, CURTAIN_END.class, TITLE_END.class};

    private final String CENTER_OP = "center";
    private final String BOLD_OP = "bold";
    private final String ITALIC_OP = "italic";
    private final String UNDERLINE_OP = "underline";
    private final String DELETE_OP = "delete";
    private final String CURTAIN_OP = "curtain";
    private final String TITLE_OP = "title";

    private final String[] operation = {CENTER_OP, BOLD_OP, ITALIC_OP, UNDERLINE_OP, DELETE_OP, CURTAIN_OP, TITLE_OP};

    private <T extends TOKEN> List<Object> until(Class<T> endClass) {
        List<Object> ret = new ArrayList<>();

        while (!(thisToken() instanceof END) && !(endClass.isInstance(thisToken()))) {
            boolean flag = false;
            int tmp;

            for (Class anEnd : end) {
                if (anEnd.isInstance(thisToken())) {
                    append(ret, new TextWithFormula(thisToken().value));
                    flag = true;
                    break;
                }
            }

            for (int i = 0; i < start.length; i++) {
                if (start[i].isInstance(thisToken())) {
                    if (thisToken() instanceof CENTER_START) {
                        mCenter = true;
                    }
                    tmp = getTokenIndex();
                    next();
                    List<Object> shown = until(end[i]);
                    mCenter = false;
                    if (shown != null) {
                        concat(ret, operate(shown, operation[i]));
                    } else {
                        setTokenIndex(tmp);
                        append(ret, new TextWithFormula(thisToken().value));
                    }
                    flag = true;
                }
            }

            if (!flag) {
                if (thisToken() instanceof PLAIN) {
                    append(ret, new TextWithFormula(thisToken().value));

                } else if (thisToken() instanceof ICON) {
                    final ICON thisToken = (ICON) thisToken();

                    TextWithFormula textWithFormula = new TextWithFormula(thisToken.value);
                    textWithFormula.setSpan(new ImageSpan(mContext, thisToken.iconId), 0,
                            thisToken.value.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                    append(ret, textWithFormula);

                } else if (thisToken() instanceof FORMULA) {

                    FORMULA thisToken = (FORMULA) thisToken();

                    TextWithFormula textWithFormula = new TextWithFormula(thisToken().value);

                    textWithFormula.addFormula(0, thisToken.length,
                            thisToken.content, thisToken.contentStart,
                            thisToken.contentStart + thisToken.content.length());

                    append(ret, textWithFormula);

                } else if (thisToken() instanceof CODE_START) {
                    /**
                     * [code][code][/code][/code][/code] shows [code][/code][/code]
                     */
                    tmp = getTokenIndex();
                    int i = 1;
                    StringBuilder string = new StringBuilder("");
                    StringBuilder substring = new StringBuilder("");
                    next();
                    while (!(thisToken() instanceof END)) {
                        if (thisToken() instanceof CODE_START) {
                            i++;
                        }
                        if (thisToken() instanceof CODE_END) {
                            i--;
                            if (i == 0) {
                                string.append(substring);
                                break;
                            }
                            string.append(substring);
                            substring.delete(0, substring.length());
                            tmp = getTokenIndex() + 1;
                        }
                        substring.append(thisToken().value);
                        next();
                    }

                    if (i == 0) {
                        final CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
                        codeView.setCode(string.toString());
                        ret.add(codeView);
                    } else if (!TextUtils.isEmpty(string)) {
                        setTokenIndex(tmp);
                        final CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
                        codeView.setCode(string.toString());
                        ret.add(codeView);
                    } else {
                        setTokenIndex(tmp);
                        append(ret, new TextWithFormula(thisToken().value));
                    }

                } else if (thisToken() instanceof IMAGE) {

                    IMAGE thisToken = (IMAGE) thisToken();
                    MyImageView imageView = loadImage(thisToken.url, thisToken.size);
                    if (mCenter) {
                        imageView.centered = true;
                    }
                    append(ret, imageView);

                } else if (thisToken() instanceof TABLE) {

                    View table = table(thisToken().value);
                    append(ret, table);

                } else if (thisToken() instanceof ATTACHMENT) {

                    final ATTACHMENT thisToken = (ATTACHMENT) thisToken();

                    mAttachmentList.remove(thisToken.attachment);   // shows remaining attachments at the bottom of view

                    append(ret, attachment(thisToken.attachment));

                } else if (thisToken() instanceof QUOTE_START) {
                    tmp = getTokenIndex();
                    int i = 1;
                    StringBuilder string = new StringBuilder("");
                    StringBuilder substring = new StringBuilder("");
                    next();
                    while (!(thisToken() instanceof END)) {
                        if (thisToken() instanceof QUOTE_START) {
                            i++;
                        }
                        if (thisToken() instanceof QUOTE_END) {
                            i--;
                            if (i == 0) {
                                string.append(substring);
                                break;
                            }
                            string.append(substring);
                            substring.delete(0, substring.length());
                            tmp = getTokenIndex() + 1;
                        }
                        substring.append(thisToken().value);
                        next();
                    }

                    if (i == 0) {
                        final QuoteView quoteView = new QuoteView(mContext);
                        quoteView.setText(string.toString());
                        ret.add(quoteView);
                    } else if (!TextUtils.isEmpty(string)) {
                        setTokenIndex(tmp);
                        final QuoteView quoteView = new QuoteView(mContext);
                        quoteView.setText(string.toString());
                        ret.add(quoteView);
                    } else {
                        setTokenIndex(tmp);
                        append(ret, new TextWithFormula(thisToken().value));
                    }
                }
            }
            next();
        }

        if (endClass.isInstance(thisToken())) {
            return ret;
        }

        return null;
    }

    private Object attachment(final Post.Attachment attachment) {
        if (attachment.isImage()) {
            String url = MyUtils.getAttachmentImageUrl(attachment);
            MyImageView imageView = loadImage(url);
            if (mCenter) {
                imageView.centered = true;
            }

            return imageView;
        } else {
            TextWithFormula builder = new TextWithFormula(attachment.getFilename());
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    MyUtils.downloadAttachment(mContext, attachment);
                }
            }, 0, attachment.getFilename().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            builder.append("\n\n");

            return builder;
        }
    }
    private void append(List<Object> list, Object element) {
        concat(list, Collections.singletonList(element));
    }

    private MyImageView loadImage(String url) {
        return loadImage(url, -1);
    }

    private MyImageView loadImage(String url, int size) {
        return loadImage(url, size, size);
    }

    private MyImageView loadImage(String url, int width, int height) {
        final MyImageView imageView = new MyImageView(mContext);

        ViewGroup.LayoutParams layoutParams;
        if (height == -1 || width == -1) {
            if (imageView.centered) {
                layoutParams = new RelativeLayout.LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                ((RelativeLayout.LayoutParams) layoutParams).addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            } else {
                layoutParams = new LinearLayout.LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
            }
        } else {
            if (imageView.centered) {
                layoutParams = new RelativeLayout.LayoutParams(width, height);
                ((RelativeLayout.LayoutParams) layoutParams).addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            } else {
                layoutParams = new LinearLayout.LayoutParams(width, height);
            }
        }
        imageView.setLayoutParams(layoutParams);
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(0, 0, 0, 10);

        Glide.with(mContext)
                .load(url)
                .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        /**
                         * adjust the size of ImageView according to image
                         */
                        if (imageView.centered) {
                            final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                            params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                            imageView.setLayoutParams(params);
                        } else {
                            imageView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                        }

                        imageView.setImageDrawable(resource);

                        imageView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (mOnViewClickListener != null) {
                                    mOnViewClickListener.onImgClick(imageView);
                                }
                            }
                        });
                        return false;
                    }
                })
                .into(imageView);
        return imageView;
    }

    private List<Object> operate(List<Object> list, String operation) {
        switch (operation) {
            case BOLD_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new StyleSpan(Typeface.BOLD), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case CENTER_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    } else if (o instanceof ImageView) {
                        ((ImageView) o).setScaleType(ImageView.ScaleType.CENTER);
                    }
                }
                break;
            case ITALIC_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new StyleSpan(Typeface.ITALIC), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case UNDERLINE_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new UnderlineSpan(), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case DELETE_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new StrikethroughSpan(), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case CURTAIN_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new BackgroundColorSpan(Color.DKGRAY), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
            case TITLE_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
        }
        return list;
    }

    private <T> void concat(List<Object> list1, List<T> list2) {
        if (list1.size() == 0) {
            list1.addAll(list2);
        } else {
            if (list2.size() > 0) {
                if (list1.get(list1.size() - 1) instanceof TextWithFormula &&
                        list2.get(0) instanceof TextWithFormula) {

                    TextWithFormula a = (TextWithFormula) list1.get(list1.size() - 1);
                    TextWithFormula b = (TextWithFormula) list2.get(0);
                    for (TextWithFormula.Formula formula : b.getFormulas()) {
                        formula.start += a.length();
                        formula.end += a.length();
                        formula.contentStart += a.length();
                        formula.contentEnd += a.length();
                    }
                    a.getFormulas().addAll(b.getFormulas());
                    a.append(b);

                    //((SpannableStringBuilder) list1.get(list1.size() - 1)).append((SpannableStringBuilder) list2.get(0));
                    list1.addAll(list2.subList(1, list2.size()));
                } else {
                    list1.addAll(list2);
                }
            }
        }
    }

    private TOKEN thisToken() {
        return mTokenList.get(mTokenIndex);
    }

    private void next() {
        mTokenIndex++;
    }

    public int getTokenIndex() {
        return mTokenIndex;
    }

    public void setTokenIndex(int tokenIndex) {
        this.mTokenIndex = tokenIndex;
    }

    private void code() {
        if (thisToken() instanceof CODE_START) {
            displayExistingText();
            next();
            CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
            StringBuilder codeContent = new StringBuilder();
            while (!(thisToken() instanceof CODE_END)) {
                codeContent.append(thisToken().value);
                next();
            }
            codeView.setCode(codeContent.toString());
            addView(codeView);
            next();
        }
        quote();
    }

    private void quote() {
        table();
    }

    private void table() {
        if (thisToken() instanceof TABLE) {
            displayExistingText();
            table(thisToken().value);
            next();
        }
        LaTeX();
    }

    private void LaTeX() {
        if (thisToken() instanceof FORMULA) {
            //// TODO: 17-2-9
            next();
        }
        image();
    }

    private void image() {
        if (thisToken() instanceof IMAGE) {
            // TODO: 17-2-9
            displayExistingText();
            next();
        }
        center();
    }

    private boolean mCenter = false;
    private int mCenterStart;
    private boolean mTitle = false;
    private int mTitleStart;

    private void center() {
        if (thisToken() instanceof CENTER_END) {
            if (mCenter) {
                mCenter = false;
                mBuilder.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), mCenterStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }
        if (thisToken() instanceof CENTER_START) {
            if (!mCenter) {
                mCenter = true;
                mCenterStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        title();
    }

    private void title() {
        if (thisToken() instanceof TITLE_END) {
            if (mTitle) {
                mTitle = false;
                mBuilder.setSpan(new RelativeSizeSpan(1.3f), mTitleStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof TITLE_START) {
            if (!mTitle) {
                mTitle = true;
                mTitleStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        delete();
    }

    private boolean mDelete;
    private int mDeleteStart;

    private void delete() {
        if (thisToken() instanceof DELETE_END) {
            if (mDelete) {
                mDelete = false;
                mBuilder.setSpan(new StrikethroughSpan(), mDeleteStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof DELETE_START) {
            if (!mDelete) {
                mDelete = true;
                mDeleteStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        underline();
    }

    private boolean mUnderline;
    private int mUnderlineStart;

    private void underline() {
        if (thisToken() instanceof UNDERLINE_END) {
            if (mUnderline) {
                mUnderline = false;
                mBuilder.setSpan(new UnderlineSpan(), mUnderlineStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof UNDERLINE_START) {
            if (!mUnderline) {
                mUnderline = true;
                mUnderlineStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        bold();
    }

    private boolean mBold;
    private int mBoldStart;

    private void bold() {
        if (thisToken() instanceof BOLD_END) {
            if (mBold) {
                mBold = false;
                mBuilder.setSpan(new StyleSpan(Typeface.BOLD), mBoldStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof BOLD_START) {
            if (!mBold) {
                mBold = true;
                mBoldStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        italic();
    }
    private boolean mItalic;
    private int mItalicStart;

    private void italic() {
        if (thisToken() instanceof ITALIC_END) {
            if (mItalic) {
                mItalic = false;
                mBuilder.setSpan(new StyleSpan(Typeface.ITALIC), mItalicStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof ITALIC_START) {
            if (!mItalic) {
                mItalic = true;
                mItalicStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        curtain();
    }

    private boolean mCurtain;
    private int mCurtainStart;

    private void curtain() {
        if (thisToken() instanceof CURTAIN_END) {
            if (mCurtain) {
                mCurtain = false;
                mBuilder.setSpan(new BackgroundColorSpan(Color.DKGRAY), mCurtainStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof CURTAIN_START) {
            if (!mCurtain) {
                mCurtain = true;
                mCurtainStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        url();
    }

    private boolean mUrl;
    private int mUrlStart;
    private String mUrlValue;

    private void url() {
        if (thisToken() instanceof URL_END) {
            if (mUrl) {
                mUrl = false;
                mBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mUrlValue)));
                    }
                }, mUrlStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof URL_START) {
            if (!mUrl) {
                mUrl = true;
                mUrlValue = ((URL_START) thisToken()).url;
                mUrlStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        color();
    }

    private boolean mColor;
    private int mColorStart;

    private void color() {
        if (thisToken() instanceof COLOR_END) {
            if (mColor) {
                mColor = false;
                mBuilder.setSpan(new RelativeSizeSpan(1.3f), mColorStart, mBuilder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        } else if (thisToken() instanceof COLOR_START) {
            if (!mColor) {
                mColor = true;
                mColorStart = mBuilder.length();
            } else {
                mBuilder.append(thisToken().value);
            }
            next();
        }

        attachment();
    }

    private void attachment() {
        if (thisToken() instanceof ATTACHMENT) {

            next();
        }

        plain();
    }

    private void plain() {
        if (thisToken() instanceof PLAIN) {
            mBuilder.append(thisToken().value);
            next();
        }

        end();
    }

    private void end() {
        if (thisToken() instanceof END) {
            displayExistingText();
        } else {
            code();
        }
    }

    private void displayExistingText() {
        if (!TextUtils.isEmpty(mBuilder)) {
            TextView textView = new TextView(mContext);
            textView.setText(mBuilder);
            addView(textView);
        }
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

        codeBlock(str);

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
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + url);

                Glide.with(mContext)
                        .load(url)
                        .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                imageView.setImageDrawable(resource);

                                imageView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mOnViewClickListener != null) {
                                            mOnViewClickListener.onImgClick(imageView);
                                        }
                                    }
                                });
                                return false;
                            }
                        })
                        .into(imageView);
                addView(imageView);
            } else {
                int start = builder.length();
                builder.append(attachment.getFilename());
                builder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "onClick() called with: view = [" + view + "]");
                        MyUtils.downloadAttachment(mContext, attachment);
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

    private void codeBlock(String str) {
        int codeStartPos, codeEndPos = 0;
        String piece, code;
        while (codeEndPos != -1 && (codeStartPos = str.indexOf(CODE_START_TAG, codeEndPos)) >= 0) {//codeMatcher.find(codeEndPos)) {
            if (codeEndPos != codeStartPos) {
                piece = str.substring(codeEndPos, codeStartPos);
                quote(piece);
            }
            codeEndPos = pairedIndex(str, codeStartPos, CODE_START_TAG, CODE_END_TAG);
            //codeEndPos = content.indexOf(CODE_END_TAG, codeStartPos) + CODE_END_TAG.length();
            if (codeEndPos == -1) {
                piece = str.substring(codeStartPos);
                quote(piece);
                codeEndPos = str.length();
            } else {
                code = str.substring(codeStartPos + CODE_START_TAG.length(), codeEndPos - CODE_END_TAG.length());
                code(code);
            }
        }
        if (codeEndPos != str.length()) {
            piece = str.substring(codeEndPos);
            quote(piece);
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
                table(piece);
            }
            quoteEndPos = pairedIndex(str, quoteStartPos, QUOTE_START_TAG, QUOTE_END_TAG);

            if (quoteEndPos == -1) {
                piece = str.substring(quoteStartPos);
                table(piece);
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
            table(piece);
        }
    }

    private View table(CharSequence str) {
        final String SPECIAL_CHAR = "\uF487";
        Pattern pattern = Pattern.compile("(?:\\n|^)( *\\|.+\\| *\\n)??( *\\|(?: *:?----*:? *\\|)+ *\\n)((?: *\\|.+\\| *(?:\\n|$))+)");
        Matcher matcher = pattern.matcher(str);
        int[] margins;
        final int LEFT = 0, RIGHT = 1, CENTER = 2;

        int startIndex = 0, endIndex;

        if (matcher.find()) {
            endIndex = matcher.start();
            if (endIndex != startIndex) {
                LaTeX2(str.subSequence(startIndex, endIndex));
            }
            startIndex = matcher.end();

            List<String> headers = null;
            if (!TextUtils.isEmpty(matcher.group(1))) {
                String wholeHeader = matcher.group(1);

                headers = new ArrayList<>(Arrays.asList(wholeHeader.split("\\|")));
                format(headers);
            }

            List<String> partitions = new ArrayList<>(Arrays.asList(matcher.group(2).split("\\|")));
            format(partitions);
            final int columnNum = partitions.size();
            margins = new int[columnNum];

            for (int i = 0; i < partitions.size(); i++) {
                String partition = partitions.get(i);
                if (partition.startsWith(":") && partition.endsWith(":")) {
                    margins[i] = CENTER;
                } else if (partition.startsWith(":")) {
                    margins[i] = LEFT;
                } else if (partition.endsWith(":")) {
                    margins[i] = RIGHT;
                } else {
                    margins[i] = CENTER;
                }
            }

            String[] rows = matcher.group(3).replace("\\|", SPECIAL_CHAR).split("\n");
            final List<List<String>> content = new ArrayList<>();
            for (String row : rows) {
                content.add(format(new ArrayList<>(Arrays.asList(row.split("\\|")))));
            }

            final List<String[]> whole = new ArrayList<>();
            if (headers != null) {
                whole.add(headers.toArray(new String[columnNum]));
            }
            for (List<String> strings : content) {
                whole.add(strings.toArray(new String[columnNum]));
            }

            // render table
            HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
            TableLayout tableLayout = new TableLayout(mContext);

            tableLayout.addView(getHorizontalDivider());
            for (int i = 0; i < whole.size(); i++) {
                String[] row = whole.get(i);
                TableRow tableRow = new TableRow(mContext);
                final TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(params);

                tableRow.addView(getVerticalDivider());
                for (int j = 0; j < row.length; j++) {
                    String cell = row[j];
                    if (cell != null) {
                        cell = cell.replace(SPECIAL_CHAR, "|");
                    }
                    PostContentView postContentView = PostContentView.newInstance(getContext(), cell, mAttachmentList, mOnViewClickListener);
                    TableRow.LayoutParams pcvParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    switch (margins[j]) {
                        case CENTER:
                            pcvParams.gravity = Gravity.CENTER;
                            break;
                        case LEFT:
                            pcvParams.gravity = Gravity.START;
                            break;
                        case RIGHT:
                            pcvParams.gravity = Gravity.END;
                            break;
                    }
                    postContentView.setPadding(10, 10, 10, 10);
                    postContentView.setLayoutParams(pcvParams);
                    tableRow.addView(postContentView);
                    tableRow.addView(getVerticalDivider());
                }
                tableLayout.addView(tableRow);
                tableLayout.addView(getHorizontalDivider());
            }

            scrollView.addView(tableLayout);

            /* LayoutParams svParams = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
            svParams.setMargins(0, 10, 0, 10);
            scrollView.setLayoutParams(svParams); */

            return scrollView;
        }

        return null;

        /* if (startIndex != str.length()) {
            LaTeX2(str.subSequence(startIndex, str.length()));
        } */
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void LaTeX2(CharSequence str) {
        Log.d(TAG, "LaTeX2: " + str);
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        builder = SFXParser3.removeTags(SFXParser3.parse(mContext, builder, mAttachmentList));
        List<OnlineImgUtils.Formula> formulaList = OnlineImgUtils.getAll(builder, mAttachmentList);
        formulaList.add(new OnlineImgUtils.Formula(builder.length(), builder.length(), "", "", OnlineImgUtils.Formula.TYPE_IMG));

        int beginIndex = 0, endIndex;
        for (int i = 0; i < formulaList.size() && beginIndex < builder.length(); i++) {
            final OnlineImgUtils.Formula formula = formulaList.get(i);
            endIndex = formula.start;
            if (formula.type == Formula.TYPE_ATT || formula.type == Formula.TYPE_IMG) {
                TextView textView = new TextView(mContext);
                final CharSequence subSequence = builder.subSequence(beginIndex, endIndex);
                final SpannableStringBuilder subBuilder = new SpannableStringBuilder(subSequence);
                textView.setText(subSequence);
                /**
                 * make links clickable
                 */
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                addView(textView);
                OnlineImgUtils.retrieveFormulaOnlineImg(OnlineImgUtils.formulasBetween(formulaList, beginIndex, endIndex), textView, subBuilder, 0, beginIndex);
                beginIndex = formula.end + 1;

                if (formula.url.equals("")) {
                    continue;
                }

                final ImageView imageView = new ImageView(mContext);

                LinearLayout.LayoutParams layoutParams;
                if (formula.size == -1) {
                    layoutParams = new LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                } else {
                    layoutParams = new LayoutParams(formula.size, formula.size);
                }
                imageView.setLayoutParams(layoutParams);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + formula.url);
                Glide.with(mContext)
                        .load(formula.url)
                        .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                if (formula.size == -1) {
                                    imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                }

                                imageView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mOnViewClickListener != null) {
                                            mOnViewClickListener.onImgClick(imageView);
                                        }
                                    }
                                });
                                return false;
                            }
                        })
                        .into(imageView);
                addView(imageView);
            }
        }
    }

    private List<String> format(List<String> strings) {
        for (int i = strings.size() - 1; i >= 0; i--) {
            String str = strings.get(i);
            if (TextUtils.isEmpty(str) || str.equals("\n")) {
                strings.remove(i);
            }
        }

        for (int i = 0; i < strings.size(); i++) {
            strings.set(i, strings.get(i).trim());
        }

        return strings;
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

    public static PostContentView newInstance(Context context, String string, List<Post.Attachment> attachmentList, OnViewClickListener onViewClickListener) {
        PostContentView postContentView = new PostContentView(context, onViewClickListener);

        if (!TextUtils.isEmpty(string)) {
            postContentView.setText(string, attachmentList);
        }

        return postContentView;
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

    private View getHorizontalDivider() {
        View horizontalDivider = new View(mContext);
        horizontalDivider.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        horizontalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return horizontalDivider;
    }

    private View getVerticalDivider() {
        View verticalDivider = new View(mContext);
        verticalDivider.setLayoutParams(new TableRow.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        verticalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return verticalDivider;
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
        mContext = context;
        removeAllViews();
    }

    public void setOnImgClickListener(OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
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

    public interface OnViewClickListener {
        void onImgClick(ImageView imageView);
    }

    private static class Formula {
        static final int TYPE_1 = 1;
        static final int TYPE_2 = 2;
        static final int TYPE_3 = 3;
        static final int TYPE_4 = 4;
        static final int TYPE_5 = 5;
        static final int TYPE_IMG = 4;
        static final int TYPE_ATT = 5;
        int start, end;
        String content, url;
        int type;

        Formula(int start, int end, String content, String url, int type) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.url = url;
            this.type = type;
        }
    }
}
