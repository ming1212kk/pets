const { request } = require("../../utils/request");

Page({
  data: {
    posts: []
  },

  onShow() {
    const app = getApp();
    if (!app.globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadPosts();
  },

  async loadPosts() {
    try {
      const posts = await request({
        url: "/api/v1/posts"
      });
      this.setData({
        posts: (posts || []).filter((item) => item.visibility === "PUBLIC")
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  openDetail(event) {
    const postId = Number(event.currentTarget.dataset.id);
    wx.navigateTo({
      url: "/pages/post-detail/index?postId=" + postId
    });
  },

  openCircles() {
    wx.switchTab({ url: "/pages/circles/index" });
  },

  openPublish() {
    wx.switchTab({ url: "/pages/publish/index" });
  }
});
