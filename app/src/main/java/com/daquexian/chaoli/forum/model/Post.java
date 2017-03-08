package com.daquexian.chaoli.forum.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.Nullable;

import com.daquexian.chaoli.forum.BR;
import com.daquexian.chaoli.forum.meta.PostAttachment;
import com.daquexian.flexiblerichtextview.Attachment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Post extends BaseObservable implements Comparable<Post>
{
	public int postId;
	public int conversationId;
	public int memberId;
	public long time;
	public int editMemberId;
	public long editTime;
	public int deleteMemberId;
	public long deleteTime;
	public String title;
	public String content;
	public int floor;
	public String username;
	public String avatarFormat;
	public String signature;
	public List<PostAttachment> attachments = new ArrayList<>();

	public Post(){}
	public Post(int memberId, String username, String avatarSuffix, String content, String time) {
		this.memberId = memberId;
		this.username = username;
		this.avatarFormat = avatarSuffix;
		this.content = content;
		this.time = Long.parseLong(time);
		this.floor = 1;
	}

	public Post(int postId, int conversationId,
				int memberId, long time,
				int editMemberId, long editTime,
				int deleteMemberId, long deleteTime,
				String title, String content,
				int floor,
				String username, String avatarFormat,
				@Nullable Map<Integer, String> groups, @Nullable String groupNames,
				@Nullable String signature,
				@Nullable List<PostAttachment> attachments)
	{
		this.postId = postId;
		notifyPropertyChanged(BR.postId);
		this.conversationId = conversationId;
		notifyPropertyChanged(BR.conversationId);
		this.memberId = memberId;
		notifyPropertyChanged(BR.memberId);
		this.time = time;
		notifyPropertyChanged(BR.time);
		this.editMemberId = editMemberId;
		notifyPropertyChanged(BR.editMemberId);
		this.editTime = editTime;
		notifyPropertyChanged(BR.editTime);
		this.deleteMemberId = deleteMemberId;
		notifyPropertyChanged(BR.deleteMemberId);
		this.deleteTime = deleteTime;
		notifyPropertyChanged(BR.deleteTime);
		this.title = title;
		notifyPropertyChanged(BR.title);
		this.content = content;
		notifyPropertyChanged(BR.content);
		this.floor = floor;
		notifyPropertyChanged(BR.floor);
		this.username = username;
		notifyPropertyChanged(BR.username);
		this.avatarFormat = avatarFormat;
		notifyPropertyChanged(BR.avatarFormat);
		this.attachments = attachments;
		notifyPropertyChanged(BR.postAttachments);
	}

	@Bindable
	public int getPostId()
	{
		return postId;
	}

	public void setPostId(int postId)
	{
		this.postId = postId;
		notifyPropertyChanged(BR.postId);
	}

	@Bindable
	public int getConversationId()
	{
		return conversationId;
	}

	public void setConversationId(int conversationId)
	{
		this.conversationId = conversationId;
		notifyPropertyChanged(BR.conversationId);
	}

	@Bindable
	public int getMemberId()
	{
		return memberId;
	}

	public void setMemberId(int memberId)
	{
		this.memberId = memberId;
		notifyPropertyChanged(BR.memberId);
	}

	@Bindable
	public long getTime()
	{
		return time;
	}

	public void setTime(long time)
	{
		this.time = time;
		notifyPropertyChanged(BR.time);
	}

	@Bindable
	public int getEditMemberId()
	{
		return editMemberId;
	}

	public void setEditMemberId(int editMemberId)
	{
		this.editMemberId = editMemberId;
		notifyPropertyChanged(BR.editMemberId);
	}

	@Bindable
	public long getEditTime()
	{
		return editTime;
	}

	public void setEditTime(long editTime)
	{
		this.editTime = editTime;
		notifyPropertyChanged(BR.editTime);
	}

	@Bindable
	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
		notifyPropertyChanged(BR.signature);
	}

	@Bindable
	public int getDeleteMemberId()
	{
		return deleteMemberId;
	}

	public void setDeleteMemberId(int deleteMemberId)
	{
		this.deleteMemberId = deleteMemberId;
		notifyPropertyChanged(BR.deleteMemberId);
	}

	@Bindable
	public long getDeleteTime()
	{
		return deleteTime;
	}

	public void setDeleteTime(long deleteTime)
	{
		this.deleteTime = deleteTime;
		notifyPropertyChanged(BR.deleteTime);
	}

	@Bindable
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
		notifyPropertyChanged(BR.title);
	}

	@Bindable
	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
		notifyPropertyChanged(BR.content);
	}

	@Bindable
	public int getFloor()
	{
		return floor;
	}

	public void setFloor(int floor)
	{
		this.floor = floor;
		notifyPropertyChanged(BR.floor);
	}

	@Bindable
	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
		notifyPropertyChanged(BR.username);
	}

	@Bindable
	public String getAvatarFormat()
	{
		return avatarFormat;
	}

	public void setAvatarFormat(String avatarFormat)
	{
		this.avatarFormat = avatarFormat;
		notifyPropertyChanged(BR.avatarFormat);
	}

	@Bindable
	public List<PostAttachment> getPostAttachments()
	{
		return attachments;
	}

	public List<Attachment> getAttachments() {
		List<Attachment> ret = new ArrayList<>();
		for (PostAttachment attachment : attachments) {
			ret.add(attachment);
		}
		return ret;
	}

	public void setPostAttachments(List<PostAttachment> attachments)
	{
		this.attachments = attachments;
		notifyPropertyChanged(BR.postAttachments);
	}

	/*public void setAvatarView(AvatarView avatarView)
	{
		this.avatarView = avatarView;
notifyPropertyChanged(BR.avatarView);
	}

	public void setAvatarView()
	{
		this.avatarView = new AvatarView(context, avatarFormat, memberId, username);
notifyPropertyChanged(BR.new AvatarView(context, avatarFormat, memberId, username));
	}*/

	/*public AvatarView getAvatarView()
	{
		return avatarView;
	}*/

	@Override
	public int compareTo(Post post) {
        if (this.getTime() < post.getTime()) return -1;
		if (this.getTime() == post.getTime()) return 0;
		else return 1;
	}
}
