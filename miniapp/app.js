App({
  globalData: {
    baseUrl: "http://127.0.0.1:8081",
    token: "",
    user: null
  },

  onLaunch() {
    const baseUrl = wx.getStorageSync("baseUrl");
    const token = wx.getStorageSync("token");
    const user = wx.getStorageSync("user");
    if (baseUrl) {
      this.globalData.baseUrl = baseUrl;
    }
    if (token) {
      this.globalData.token = token;
    }
    if (user) {
      this.globalData.user = user;
    }
  },

  setBaseUrl(baseUrl) {
    const normalized = String(baseUrl || "").trim().replace(/\/$/, "");
    if (!normalized) {
      return;
    }
    this.globalData.baseUrl = normalized;
    wx.setStorageSync("baseUrl", normalized);
  },

  setSession(payload) {
    this.globalData.token = payload.token || this.globalData.token;
    this.globalData.user = {
      userId: payload.userId,
      nickname: payload.nickname,
      avatarUrl: payload.avatarUrl || "",
      phone: payload.phone || "",
      role: payload.role,
      wechatBound: Boolean(payload.wechatBound)
    };
    wx.setStorageSync("token", this.globalData.token);
    wx.setStorageSync("user", this.globalData.user);
  },

  updateCurrentUser(profile) {
    if (!profile) {
      return;
    }
    this.globalData.user = {
      userId: profile.userId,
      nickname: profile.nickname,
      avatarUrl: profile.avatarUrl || "",
      phone: profile.phone || "",
      role: profile.role,
      wechatBound: Boolean(profile.wechatBound)
    };
    if (profile.token) {
      this.globalData.token = profile.token;
      wx.setStorageSync("token", profile.token);
    }
    wx.setStorageSync("user", this.globalData.user);
  },

  getWechatCode() {
    const cachedCode = wx.getStorageSync("wechatMockCode");
    if (cachedCode) {
      return Promise.resolve(cachedCode);
    }

    return new Promise((resolve) => {
      wx.login({
        success: (response) => {
          const code = response.code || ("mock-" + Date.now());
          wx.setStorageSync("wechatMockCode", code);
          resolve(code);
        },
        fail: () => {
          const fallbackCode = "mock-" + Date.now();
          wx.setStorageSync("wechatMockCode", fallbackCode);
          resolve(fallbackCode);
        }
      });
    });
  },

  clearSession() {
    this.globalData.token = "";
    this.globalData.user = null;
    wx.removeStorageSync("token");
    wx.removeStorageSync("user");
  }
});
