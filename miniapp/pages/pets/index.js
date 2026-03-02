const { request } = require("../../utils/request");

Page({
  data: {
    profile: null,
    avatarInitial: "我",
    profileNickname: "",
    profileAvatarUrl: "",
    defaultPetName: "",
    pets: [],
    formId: null,
    name: "",
    type: "",
    ageMonths: ""
  },

  onShow() {
    if (!getApp().globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadPageData();
  },

  async loadPageData() {
    try {
      const [profile, pets] = await Promise.all([
        request({ url: "/api/v1/users/me" }),
        request({ url: "/api/v1/pets" })
      ]);
      getApp().updateCurrentUser(profile);
      this.setData({
        profile,
        avatarInitial: this.pickAvatarInitial(profile.nickname),
        profileNickname: profile.nickname || "",
        profileAvatarUrl: profile.avatarUrl || "",
        defaultPetName: (pets.find((item) => item.defaultPet) || {}).name || "",
        pets
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

  async saveProfile() {
    try {
      const profile = await request({
        url: "/api/v1/users/me",
        method: "PUT",
        data: {
          nickname: this.data.profileNickname,
          avatarUrl: this.data.profileAvatarUrl || null
        }
      });
      getApp().updateCurrentUser(profile);
      this.setData({
        profile,
        avatarInitial: this.pickAvatarInitial(profile.nickname)
      });
      wx.showToast({ title: "资料已保存", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  async syncWechatProfile() {
    try {
      const profileInfo = await this.fetchWechatProfile();
      const code = await getApp().getWechatCode();
      const profile = await request({
        url: "/api/v1/users/me/wechat-bind",
        method: "POST",
        data: {
          code,
          nickname: profileInfo.nickname || this.data.profileNickname || null,
          avatarUrl: profileInfo.avatarUrl || this.data.profileAvatarUrl || null
        }
      });
      getApp().updateCurrentUser(profile);
      this.setData({
        profile,
        avatarInitial: this.pickAvatarInitial(profile.nickname),
        profileNickname: profile.nickname || "",
        profileAvatarUrl: profile.avatarUrl || ""
      });
      wx.showToast({ title: "已同步微信资料", icon: "success" });
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
        desc: "用于同步微信头像和昵称",
        success: (response) => {
          const info = response.userInfo || {};
          resolve({
            nickname: info.nickName || "",
            avatarUrl: info.avatarUrl || ""
          });
        },
        fail: () => resolve({})
      });
    });
  },

  editPet(event) {
    const petId = Number(event.currentTarget.dataset.id);
    const pet = this.data.pets.find((item) => item.id === petId);
    if (!pet) {
      return;
    }
    this.setData({
      formId: pet.id,
      name: pet.name,
      type: pet.type,
      ageMonths: String(pet.ageMonths)
    });
  },

  resetForm() {
    this.setData({
      formId: null,
      name: "",
      type: "",
      ageMonths: ""
    });
  },

  async submitPet() {
    try {
      const payload = {
        name: this.data.name,
        type: this.data.type,
        ageMonths: Number(this.data.ageMonths)
      };
      if (this.data.formId) {
        await request({
          url: "/api/v1/pets/" + this.data.formId,
          method: "PUT",
          data: payload
        });
      } else {
        await request({
          url: "/api/v1/pets",
          method: "POST",
          data: payload
        });
      }
      this.resetForm();
      await this.loadPageData();
      wx.showToast({ title: "宠物已保存", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  async setDefaultPet(event) {
    try {
      await request({
        url: "/api/v1/pets/" + event.currentTarget.dataset.id + "/default",
        method: "POST"
      });
      await this.loadPageData();
      wx.showToast({ title: "已设为默认", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  logout() {
    getApp().clearSession();
    wx.reLaunch({ url: "/pages/login/index" });
  },

  pickAvatarInitial(nickname) {
    return nickname ? String(nickname).slice(0, 1) : "我";
  }
});
