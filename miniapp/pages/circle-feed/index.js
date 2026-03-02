const { request } = require("../../utils/request");

Page({
  data: {
    circleId: 0,
    circle: null,
    posts: []
  },

  onLoad(query) {
    this.setData({
      circleId: Number(query.circleId || 0)
    });
  },

  onShow() {
    if (!getApp().globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadData();
  },

  async loadData() {
    try {
      const [circles, posts] = await Promise.all([
        request({ url: "/api/v1/circles" }),
        request({ url: "/api/v1/circles/" + this.data.circleId + "/posts" })
      ]);
      const circle = circles.find((item) => item.id === this.data.circleId) || null;
      this.setData({
        circle,
        posts
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

  openPublish() {
    wx.switchTab({ url: "/pages/publish/index" });
  }
});
