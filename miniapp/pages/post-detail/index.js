const { request } = require("../../utils/request");

function buildCommentThreads(comments) {
  const roots = [];
  const rootMap = {};

  (comments || []).forEach((comment) => {
    const node = Object.assign({}, comment, { children: [] });
    if (!comment.parentCommentId) {
      roots.push(node);
      rootMap[comment.id] = node;
      return;
    }
    const rootId = rootMap[comment.parentCommentId] ? comment.parentCommentId : comment.parentCommentId;
    if (!rootMap[rootId]) {
      rootMap[rootId] = {
        id: rootId,
        children: []
      };
    }
    rootMap[rootId].children.push(node);
  });

  return roots;
}

Page({
  data: {
    postId: "",
    post: null,
    commentThreads: [],
    commentContent: "",
    user: null,
    replyContext: null,
    mentionUsers: []
  },

  onLoad(query) {
    this.setData({
      postId: query.postId
    });
  },

  onShow() {
    if (!getApp().globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.setData({
      user: getApp().globalData.user
    });
    this.loadPost();
  },

  async loadPost() {
    try {
      const post = await request({
        url: "/api/v1/posts/" + this.data.postId
      });
      this.setData({
        post,
        commentThreads: buildCommentThreads(post.comments)
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  updateField(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [field]: event.detail.value
    });
  },

  async toggleLike() {
    try {
      const method = this.data.post.likedByMe ? "DELETE" : "POST";
      await request({
        url: "/api/v1/posts/" + this.data.postId + "/likes",
        method
      });
      this.loadPost();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  setReplyContext(event) {
    const commentId = Number(event.currentTarget.dataset.id);
    const parentId = event.currentTarget.dataset.parentId ? Number(event.currentTarget.dataset.parentId) : null;
    const authorId = Number(event.currentTarget.dataset.authorId);
    const authorNickname = event.currentTarget.dataset.authorNickname;
    const parentCommentId = parentId || commentId;
    const replyContext = {
      parentCommentId,
      replyToUserId: authorId,
      replyToNickname: authorNickname
    };

    this.setData({
      replyContext,
      commentContent: "@" + authorNickname + " " + (this.data.commentContent || "")
    });
  },

  addMention(event) {
    const userId = Number(event.currentTarget.dataset.userid);
    const nickname = event.currentTarget.dataset.nickname;
    const mentionUsers = this.data.mentionUsers.slice();

    if (!mentionUsers.some((item) => item.userId === userId)) {
      mentionUsers.push({ userId, nickname });
    }

    this.setData({
      mentionUsers,
      commentContent: this.data.commentContent + "@" + nickname + " "
    });
  },

  clearReplyContext() {
    this.setData({
      replyContext: null
    });
  },

  removeMention(event) {
    const userId = Number(event.currentTarget.dataset.userid);
    this.setData({
      mentionUsers: this.data.mentionUsers.filter((item) => item.userId !== userId)
    });
  },

  async submitComment() {
    try {
      const mentionUserIds = this.data.mentionUsers.map((item) => item.userId);
      await request({
        url: "/api/v1/posts/" + this.data.postId + "/comments",
        method: "POST",
        data: {
          content: this.data.commentContent,
          parentCommentId: this.data.replyContext ? this.data.replyContext.parentCommentId : null,
          replyToUserId: this.data.replyContext ? this.data.replyContext.replyToUserId : null,
          mentionUserIds
        }
      });
      this.setData({
        commentContent: "",
        replyContext: null,
        mentionUsers: []
      });
      this.loadPost();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  async deleteComment(event) {
    try {
      await request({
        url: "/api/v1/comments/" + event.currentTarget.dataset.id,
        method: "DELETE"
      });
      this.loadPost();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  }
});
