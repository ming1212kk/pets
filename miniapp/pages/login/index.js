const { request } = require("../../utils/request");

Page({
  data: {
    baseUrl: "",
    phone: "",
    smsCode: "",
    debugCode: "",
    wechatNickname: "",
    wechatAvatarUrl: ""
  },

  onLoad() {
    this.setData({
      baseUrl: getApp().globalData.baseUrl
    });
  },

  onShow() {
    if (getApp().globalData.token) {
      wx.reLaunch({ url: "/pages/square/index" });
    }
  },

  updateField(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [field]: event.detail.value
    });
  },

  saveBaseUrl() {
    getApp().setBaseUrl(this.data.baseUrl);
    wx.showToast({ title: "地址已保存", icon: "success" });
  },

  async sendSmsCode() {
    try {
      getApp().setBaseUrl(this.data.baseUrl);
      const result = await request({
        url: "/api/v1/auth/sms/send",
        method: "POST",
        auth: false,
        data: { phone: this.data.phone }
      });
      this.setData({
        debugCode: result.debugCode,
        smsCode: result.debugCode
      });
      wx.showToast({ title: "验证码已发送", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  async phoneLogin() {
    try {
      getApp().setBaseUrl(this.data.baseUrl);
      const result = await request({
        url: "/api/v1/auth/login/phone",
        method: "POST",
        auth: false,
        data: {
          phone: this.data.phone,
          code: this.data.smsCode
        }
      });
      getApp().setSession(result);
      wx.reLaunch({ url: "/pages/square/index" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  async wechatLogin() {
    try {
      getApp().setBaseUrl(this.data.baseUrl);
      const profile = await this.fetchWechatProfile();
      const code = await getApp().getWechatCode();
      const result = await request({
        url: "/api/v1/auth/login/wechat",
        method: "POST",
        auth: false,
        data: {
          code,
          nickname: profile.nickname || this.data.wechatNickname || null,
          avatarUrl: profile.avatarUrl || this.data.wechatAvatarUrl || null
        }
      });
      getApp().setSession(result);
      this.setData({
        wechatNickname: result.nickname || "",
        wechatAvatarUrl: result.avatarUrl || ""
      });
      wx.reLaunch({ url: "/pages/square/index" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  fetchWechatProfile() {
    return new Promise((resolve) => {
      if (typeof wx.getUserProfile !== "function") {
        resolve({});
        return;
      }

      wx.getUserProfile({
        desc: "用于同步微信昵称和头像",
        success: (response) => {
          const profile = response.userInfo || {};
          this.setData({
            wechatNickname: profile.nickName || "",
            wechatAvatarUrl: profile.avatarUrl || ""
          });
          resolve({
            nickname: profile.nickName || "",
            avatarUrl: profile.avatarUrl || ""
          });
        },
        fail: () => {
          resolve({});
        }
      });
    });
  }
});
