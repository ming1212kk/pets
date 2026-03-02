const { request } = require("../../utils/request");

Page({
  data: {
    token: "",
    joinResult: null
  },

  onLoad(query) {
    this.setData({
      token: query.token ? decodeURIComponent(query.token) : ""
    });
  },

  onShow() {
    if (!getApp().globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
    }
  },

  updateField(event) {
    this.setData({
      token: event.detail.value
    });
  },

  async submitJoin() {
    try {
      const joinResult = await request({
        url: "/api/v1/circles/join",
        method: "POST",
        data: {
          token: this.data.token
        }
      });
      this.setData({ joinResult });
      wx.showToast({ title: "加入成功", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  openCircleFeed() {
    if (!this.data.joinResult) {
      return;
    }
    wx.navigateTo({
      url: "/pages/circle-feed/index?circleId=" + this.data.joinResult.id
    });
  },

  backToCircles() {
    wx.switchTab({
      url: "/pages/circles/index"
    });
  }
});
