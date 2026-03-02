const { request } = require("../../utils/request");

Page({
  data: {
    user: null,
    circles: [],
    formName: "",
    formDescription: "",
    editingCircleId: null,
    memberCircle: null,
    members: [],
    invitePreview: null
  },

  onShow() {
    const app = getApp();
    if (!app.globalData.token) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.setData({
      user: app.globalData.user
    });
    this.loadCircles();
  },

  async loadCircles() {
    try {
      const circles = await request({ url: "/api/v1/circles" });
      this.setData({ circles });

      if (this.data.memberCircle) {
        const current = circles.find((item) => item.id === this.data.memberCircle.id);
        if (!current) {
          this.setData({
            memberCircle: null,
            members: []
          });
          return;
        }
        this.loadMembers(current.id, current);
      }
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

  async submitCircle() {
    try {
      const editingCircleId = this.data.editingCircleId;
      const method = editingCircleId ? "PUT" : "POST";
      const url = editingCircleId ? "/api/v1/circles/" + editingCircleId : "/api/v1/circles";

      await request({
        url,
        method,
        data: {
          name: this.data.formName,
          description: this.data.formDescription
        }
      });

      this.resetForm();
      wx.showToast({ title: editingCircleId ? "圈子已更新" : "圈子已创建", icon: "success" });
      this.loadCircles();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  startEdit(event) {
    this.setData({
      editingCircleId: Number(event.currentTarget.dataset.id),
      formName: event.currentTarget.dataset.name || "",
      formDescription: event.currentTarget.dataset.description || ""
    });
  },

  resetForm() {
    this.setData({
      editingCircleId: null,
      formName: "",
      formDescription: ""
    });
  },

  async loadMembers(circleId, circleData) {
    try {
      const members = await request({
        url: "/api/v1/circles/" + circleId + "/members"
      });
      this.setData({
        memberCircle: circleData || this.data.circles.find((item) => item.id === circleId) || null,
        members
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  toggleMembers(event) {
    const circleId = Number(event.currentTarget.dataset.id);
    if (this.data.memberCircle && this.data.memberCircle.id === circleId) {
      this.setData({
        memberCircle: null,
        members: []
      });
      return;
    }
    const circleData = this.data.circles.find((item) => item.id === circleId);
    this.loadMembers(circleId, circleData);
  },

  async createInvite(event) {
    try {
      const circleId = Number(event.currentTarget.dataset.id);
      const invite = await request({
        url: "/api/v1/circles/" + circleId + "/invites",
        method: "POST"
      });
      this.setData({
        invitePreview: invite
      });
      wx.setClipboardData({
        data: invite.inviteLink
      });
      wx.showToast({ title: "邀请链接已复制", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  openInviteJoinPage() {
    if (!this.data.invitePreview) {
      return;
    }
    wx.navigateTo({
      url: "/pages/circle-join/index?token=" + encodeURIComponent(this.data.invitePreview.token)
    });
  },

  openJoinPage() {
    wx.navigateTo({
      url: "/pages/circle-join/index"
    });
  },

  openFeed(event) {
    const circleId = Number(event.currentTarget.dataset.id);
    wx.navigateTo({
      url: "/pages/circle-feed/index?circleId=" + circleId
    });
  },

  async removeMember(event) {
    try {
      const circleId = Number(event.currentTarget.dataset.circleId);
      const memberUserId = Number(event.currentTarget.dataset.userId);
      await request({
        url: "/api/v1/circles/" + circleId + "/members/" + memberUserId,
        method: "DELETE"
      });
      wx.showToast({ title: "成员已移除", icon: "success" });
      this.loadMembers(circleId);
      this.loadCircles();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  async leaveOrDissolve(event) {
    const circleId = Number(event.currentTarget.dataset.id);
    const owner = event.currentTarget.dataset.owner === "true";

    try {
      if (owner) {
        await request({
          url: "/api/v1/circles/" + circleId,
          method: "DELETE"
        });
      } else {
        await request({
          url: "/api/v1/circles/" + circleId + "/leave",
          method: "POST"
        });
      }

      if (this.data.memberCircle && this.data.memberCircle.id === circleId) {
        this.setData({
          memberCircle: null,
          members: []
        });
      }
      wx.showToast({ title: owner ? "圈子已解散" : "已退出圈子", icon: "success" });
      this.loadCircles();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  }
});
