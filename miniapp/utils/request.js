const app = getApp();

function request(options) {
  const auth = options.auth !== false;
  const token = app.globalData.token;
  const headers = Object.assign({
    "Content-Type": "application/json"
  }, options.header || {});

  if (auth && token) {
    headers.Authorization = "Bearer " + token;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + options.url,
      method: options.method || "GET",
      data: options.data,
      header: headers,
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data);
          return;
        }
        const message = response.data && response.data.message ? response.data.message : "请求失败";
        reject(new Error(message));
      },
      fail(error) {
        reject(error);
      }
    });
  });
}

module.exports = {
  request
};
