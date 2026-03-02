const state = {
  baseUrl: window.location.origin,
  token: ""
};

const refs = {
  baseUrl: document.querySelector("#base-url"),
  username: document.querySelector("#username"),
  password: document.querySelector("#password"),
  loginBtn: document.querySelector("#login-btn"),
  refreshBtn: document.querySelector("#refresh-btn"),
  banBtn: document.querySelector("#ban-btn"),
  unbanBtn: document.querySelector("#unban-btn"),
  userId: document.querySelector("#user-id"),
  loginMessage: document.querySelector("#login-message"),
  statusPill: document.querySelector("#status-pill"),
  postCount: document.querySelector("#post-count"),
  postList: document.querySelector("#post-list")
};

refs.baseUrl.value = state.baseUrl;

function setMessage(message) {
  refs.loginMessage.textContent = message;
}

function setStatus(live) {
  refs.statusPill.textContent = live ? "Connected" : "Disconnected";
  refs.statusPill.classList.toggle("live", live);
}

function normalizeBaseUrl() {
  state.baseUrl = refs.baseUrl.value.trim().replace(/\/$/, "") || window.location.origin;
  refs.baseUrl.value = state.baseUrl;
}

async function api(path, options = {}) {
  normalizeBaseUrl();
  const headers = Object.assign({
    "Content-Type": "application/json"
  }, options.headers || {});

  if (state.token) {
    headers.Authorization = "Bearer " + state.token;
  }

  const response = await fetch(state.baseUrl + path, {
    method: options.method || "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.message || "Request failed");
  }
  return payload;
}

async function login() {
  try {
    const payload = await api("/api/v1/auth/admin/login", {
      method: "POST",
      body: {
        username: refs.username.value.trim(),
        password: refs.password.value
      }
    });
    state.token = payload.token;
    setStatus(true);
    setMessage("Admin session ready.");
    await loadPosts();
  } catch (error) {
    setStatus(false);
    setMessage(error.message);
  }
}

async function loadPosts() {
  if (!state.token) {
    setMessage("Log in first.");
    return;
  }

  try {
    const posts = await api("/api/v1/admin/posts");
    refs.postCount.textContent = posts.length + " posts";
    if (!posts.length) {
      refs.postList.innerHTML = '<div class="empty">No posts yet.</div>';
      return;
    }

    refs.postList.innerHTML = posts.map((post) => `
      <article class="post-card">
        <header>
          <div>
            <h3>#${post.id} · ${escapeHtml(post.authorNickname)}</h3>
            <div class="meta">User ${post.authorId} · ${post.authorStatus} · ${escapeHtml(post.createdAt)}</div>
          </div>
        </header>
        <p>${escapeHtml(post.content)}</p>
        <div class="tag-row">
          <span class="tag">${escapeHtml(post.reviewStatus)}</span>
          <span class="tag dim">${post.likeCount} likes</span>
          <span class="tag dim">${post.commentCount} comments</span>
        </div>
        <div class="meta">${escapeHtml(post.reviewReason || "")}</div>
        <div class="post-actions">
          <button data-action="delete-post" data-id="${post.id}">Delete Post</button>
          <button class="danger" data-action="ban-user" data-user-id="${post.authorId}">Ban User</button>
          <button class="ghost" data-action="unban-user" data-user-id="${post.authorId}">Unban User</button>
        </div>
      </article>
    `).join("");
  } catch (error) {
    setMessage(error.message);
  }
}

async function deletePost(postId) {
  try {
    await api("/api/v1/admin/posts/" + postId, { method: "DELETE" });
    setMessage("Post deleted.");
    await loadPosts();
  } catch (error) {
    setMessage(error.message);
  }
}

async function updateUser(userId, action) {
  try {
    await api("/api/v1/admin/users/" + userId + "/" + action, { method: "POST" });
    setMessage("User " + action + " success.");
    await loadPosts();
  } catch (error) {
    setMessage(error.message);
  }
}

function readUserId() {
  return refs.userId.value.trim();
}

function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

refs.loginBtn.addEventListener("click", login);
refs.refreshBtn.addEventListener("click", loadPosts);
refs.banBtn.addEventListener("click", () => {
  const userId = readUserId();
  if (userId) {
    updateUser(userId, "ban");
  }
});
refs.unbanBtn.addEventListener("click", () => {
  const userId = readUserId();
  if (userId) {
    updateUser(userId, "unban");
  }
});

refs.postList.addEventListener("click", (event) => {
  const action = event.target.dataset.action;
  if (!action) {
    return;
  }
  if (action === "delete-post") {
    deletePost(event.target.dataset.id);
  }
  if (action === "ban-user") {
    updateUser(event.target.dataset.userId, "ban");
  }
  if (action === "unban-user") {
    updateUser(event.target.dataset.userId, "unban");
  }
});
