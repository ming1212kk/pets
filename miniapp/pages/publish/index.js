const { request } = require("../../utils/request");

Page({
  data: {
    pets: [],
    petIndex: 0,
    hasPets: false,
    circles: [],
    selectedCircleIds: [],
    content: "",
    topic: "",
    imageUrlsText: "",
    videoUrl: "",
    visibility: "PUBLIC"
  },

  onShow() {
    if (!getApp().globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadFormData();
  },

  async loadFormData() {
    try {
      const [pets, circles] = await Promise.all([
        request({ url: "/api/v1/pets" }),
        request({ url: "/api/v1/circles" })
      ]);
      const selectedCircleIds = this.data.selectedCircleIds;
      const defaultIndex = pets.findIndex((item) => item.defaultPet);
      this.setData({
        pets,
        circles: circles.map((item) => Object.assign({}, item, {
          selected: selectedCircleIds.indexOf(item.id) !== -1
        })),
        hasPets: pets.length > 0,
        petIndex: defaultIndex >= 0 ? defaultIndex : 0
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

  changePet(event) {
    this.setData({
      petIndex: Number(event.detail.value)
    });
  },

  changeVisibility(event) {
    this.setData({
      visibility: event.currentTarget.dataset.value
    });
  },

  changeCircleSelection(event) {
    const selectedCircleIds = event.detail.value.map((item) => Number(item));
    this.setData({
      selectedCircleIds,
      circles: this.data.circles.map((item) => Object.assign({}, item, {
        selected: selectedCircleIds.indexOf(item.id) !== -1
      }))
    });
  },

  async submitPost() {
    try {
      const imageUrls = this.data.imageUrlsText
        .split(/\s|,|\n/)
        .map((item) => item.trim())
        .filter(Boolean);

      if (this.data.visibility === "CIRCLE" && !this.data.selectedCircleIds.length) {
        wx.showToast({ title: "请至少选择一个圈子", icon: "none" });
        return;
      }

      const petId = this.data.hasPets ? this.data.pets[this.data.petIndex].id : null;
      await request({
        url: "/api/v1/posts",
        method: "POST",
        data: {
          petId,
          content: this.data.content,
          topic: this.data.topic || null,
          imageUrls,
          videoUrl: this.data.videoUrl || null,
          visibility: this.data.visibility,
          circleIds: this.data.visibility === "CIRCLE" ? this.data.selectedCircleIds : []
        }
      });
      this.setData({
        content: "",
        topic: "",
        imageUrlsText: "",
        videoUrl: "",
        visibility: "PUBLIC",
        selectedCircleIds: []
      });
      wx.showToast({ title: "发布成功", icon: "success" });
      wx.switchTab({ url: "/pages/square/index" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  }
});
