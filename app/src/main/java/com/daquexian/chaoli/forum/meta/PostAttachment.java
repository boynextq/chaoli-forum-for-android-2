package com.daquexian.chaoli.forum.meta;

import com.daquexian.flexiblerichtextview.Attachment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by daquexian on 17-2-17.
 */

public class PostAttachment extends Attachment
{
    private static final String[] IMAGE_FILE_EXTENSION = {".jpg", ".png", ".gif", ".jpeg"};

    public String attachmentId;
    public String filename;
    public String secret;
    public int postId;
    public int draftMemberId;
    public int draftConversationId;

    public PostAttachment()
    {
        this("", "", "", 0);
    }

    public PostAttachment(String attachmentId, String filename, String secret, int postId)
    {
        this(attachmentId, filename, secret, postId, 0, 0);
    }

    public PostAttachment(String attachmentId, String filename, String secret, int postId,
                      int draftMemberId, int draftConversationId)
    {
        this.attachmentId = attachmentId;
        this.filename = filename;
        this.secret = secret;
        this.postId = postId;
        this.draftMemberId = draftMemberId;
        this.draftConversationId = draftConversationId;
    }

    public String getAttachmentId()
    {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId)
    {
        this.attachmentId = attachmentId;
    }

    @Override
    public String getText()
    {
        return filename.toLowerCase();
    }

    public String getFilename() {
        return getText();
    }
    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    public int getPostId()
    {
        return postId;
    }

    public void setPostId(int postId)
    {
        this.postId = postId;
    }

    public int getDraftMemberId()
    {
        return draftMemberId;
    }

    public void setDraftMemberId(int draftMemberId)
    {
        this.draftMemberId = draftMemberId;
    }

    public int getDraftConversationId()
    {
        return draftConversationId;
    }

    public void setDraftConversationId(int draftConversationId)
    {
        this.draftConversationId = draftConversationId;
    }

    @Override
    public boolean isImage() {
        for (String image_ext : IMAGE_FILE_EXTENSION) {
            if (getText().endsWith(image_ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getUrl() {
        if (isImage()) {
            return Constants.ATTACHMENT_IMAGE_URL + getAttachmentId() + getSecret();
        } else {
            try {
                return "https://chaoli.club/index.php/attachment/" + getAttachmentId() + "_" + URLEncoder.encode(getText(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "";
            }
        }
    }
}

