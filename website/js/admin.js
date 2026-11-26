(function () {
  firebase.initializeApp(window.FIREBASE_CONFIG);

  var auth = firebase.auth();
  var db = firebase.database();
  var googleProvider = new firebase.auth.GoogleAuthProvider();
  var ADMIN_UID = window.ADMIN_UID;
  var ADMIN_EMAIL = (window.ADMIN_EMAIL || "").toLowerCase();
  var plansCache = [];

  googleProvider.setCustomParameters({ prompt: "select_account" });

  function isAdminUser(user) {
    if (!user) return false;
    if (user.uid === ADMIN_UID) return true;
    var email = (user.email || "").toLowerCase();
    return email && email === ADMIN_EMAIL;
  }

  var loginSection = document.getElementById("login-section");
  var adminSection = document.getElementById("admin-section");
  var loginError = document.getElementById("login-error");
  var adminUserLabel = document.getElementById("admin-user-label");
  var btnSignOut = document.getElementById("btn-sign-out");
  var toastEl = document.getElementById("toast");

  function $(id) {
    return document.getElementById(id);
  }

  function showToast(message, type) {
    toastEl.textContent = message;
    toastEl.className = "toast show" + (type ? " " + type : "");
    clearTimeout(showToast._t);
    showToast._t = setTimeout(function () {
      toastEl.classList.remove("show");
    }, 3200);
  }

  function showLogin() {
    loginSection.classList.remove("hidden");
    adminSection.classList.add("hidden");
    btnSignOut.classList.add("hidden");
  }

  function showAdmin(user) {
    loginSection.classList.add("hidden");
    adminSection.classList.remove("hidden");
    btnSignOut.classList.remove("hidden");
    adminUserLabel.textContent = user.displayName
      ? user.displayName + " · " + user.email
      : user.email || user.uid;
    attachListeners();
  }

  function denyAccess() {
    auth.signOut();
    loginError.textContent = "This account is not an admin.";
    loginError.classList.remove("hidden");
    showLogin();
  }

  auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL).catch(function () {});

  auth.onAuthStateChanged(function (user) {
    if (!user) {
      showLogin();
      return;
    }
    if (!isAdminUser(user)) {
      denyAccess();
      return;
    }
    loginError.classList.add("hidden");
    showAdmin(user);
  });

  $("login-form").addEventListener("submit", function (e) {
    e.preventDefault();
    loginError.classList.add("hidden");
    var email = $("login-email").value.trim();
    var password = $("login-password").value;
    var btn = $("btn-email-sign-in");
    btn.disabled = true;
    btn.textContent = "Signing in…";
    auth.signInWithEmailAndPassword(email, password)
      .catch(function (err) {
        loginError.textContent = mapLoginError(err);
        loginError.classList.remove("hidden");
      })
      .finally(function () {
        btn.disabled = false;
        btn.textContent = "Sign in with email";
      });
  });

  $("btn-google-sign-in").addEventListener("click", function () {
    loginError.classList.add("hidden");
    var btn = $("btn-google-sign-in");
    btn.disabled = true;
    auth.signInWithPopup(googleProvider)
      .catch(function (err) {
        loginError.textContent = mapLoginError(err);
        loginError.classList.remove("hidden");
      })
      .finally(function () {
        btn.disabled = false;
      });
  });

  function mapLoginError(err) {
    if (!err || !err.code) {
      return (err && err.message) || "Sign-in failed.";
    }
    if (err.code === "auth/popup-closed-by-user" || err.code === "auth/cancelled-popup-request") {
      return "Sign-in cancelled.";
    }
    if (err.code === "auth/user-not-found" || err.code === "auth/wrong-password" || err.code === "auth/invalid-credential") {
      return "Wrong email or password.";
    }
    if (err.code === "auth/operation-not-allowed") {
      return "Enable Google or Email/Password in Firebase Authentication → Sign-in method.";
    }
    if (err.code === "auth/unauthorized-domain") {
      return "Add this site to Firebase Authentication → Settings → Authorized domains.";
    }
    if (err.code === "auth/too-many-requests") {
      return "Too many attempts. Try again later.";
    }
    return err.message || "Sign-in failed.";
  }

  btnSignOut.addEventListener("click", function () {
    auth.signOut();
  });

  var listenersAttached = false;

  function attachListeners() {
    if (listenersAttached) return;
    listenersAttached = true;

    db.ref("users").on("value", function (snap) {
      $("stat-users").textContent = formatNumber(snap.numChildren());
    });

    db.ref("subscription_config/premiumEnabled").on("value", function (snap) {
      var enabled = snap.val() !== false;
      $("premium-enabled").checked = enabled;
      $("stat-premium").textContent = enabled ? "ON" : "OFF";
    });

    db.ref("maintenance").on("value", function (snap) {
      var active = snap.child("active").val() === true;
      $("maintenance-active").checked = active;
      $("maintenance-title").value = val(snap.child("title"));
      $("maintenance-message").value = val(snap.child("message"));
      $("maintenance-button").value = val(snap.child("button_text"));
      $("stat-maintenance").textContent = active ? "ON" : "OFF";
    });

    db.ref("website_config/downloadUrl").on("value", function (snap) {
      var url = val(snap);
      $("website-download-url").value = url || (window.DEFAULT_DOWNLOAD_URL || "");
    });

    db.ref("app_update_config").on("value", function (snap) {
      $("update-enabled").checked = snap.child("enabled").val() === true;
      $("update-force").checked = snap.child("forceUpdate").val() === true;
      $("update-version").value = val(snap.child("latestVersion"));
      $("update-title").value = val(snap.child("title"));
      $("update-message").value = val(snap.child("message"));
      $("update-url").value = val(snap.child("storeUrl"));
    });

    db.ref("subscription_plans").on("value", function (snap) {
      plansCache = [];
      snap.forEach(function (child) {
        plansCache.push({
          id: child.key,
          title: val(child.child("title")),
          subtitle: val(child.child("subtitle")),
          priceLabel: val(child.child("priceLabel")),
          badge: val(child.child("badge")),
          active: child.child("active").val() !== false,
          sortOrder: num(child.child("sortOrder"))
        });
      });
      plansCache.sort(function (a, b) {
        return a.sortOrder - b.sortOrder;
      });
      renderPlans();
      var activeCount = plansCache.filter(function (p) {
        return p.active;
      }).length;
      $("stat-plans").textContent = String(activeCount);
    });

    $("btn-save-all").addEventListener("click", saveAppSettings);
    $("btn-save-website").addEventListener("click", saveWebsiteConfig);
    $("btn-save-maintenance").addEventListener("click", saveMaintenance);
    $("btn-add-plan").addEventListener("click", addPlan);
    $("btn-send-push").addEventListener("click", sendPush);
  }

  function saveWebsiteConfig() {
    var url = $("website-download-url").value.trim();
    if (url && !/^https?:\/\//i.test(url)) {
      showToast("Enter a valid URL starting with https://", "error");
      return;
    }
    db.ref("website_config/downloadUrl").set(url)
      .then(function () {
        showToast("Landing download link saved.", "success");
      })
      .catch(function (err) {
        showToast(err.message || "Save failed.", "error");
      });
  }

  function saveAppSettings() {
    var updates = {};
    updates["subscription_config/premiumEnabled"] = $("premium-enabled").checked;
    updates["app_update_config/enabled"] = $("update-enabled").checked;
    updates["app_update_config/forceUpdate"] = $("update-force").checked;
    updates["app_update_config/latestVersion"] = $("update-version").value.trim();
    updates["app_update_config/title"] = $("update-title").value.trim();
    updates["app_update_config/message"] = $("update-message").value.trim();
    updates["app_update_config/storeUrl"] = $("update-url").value.trim();

    db.ref().update(updates)
      .then(function () {
        showToast("App settings saved.", "success");
      })
      .catch(function (err) {
        showToast(err.message || "Save failed.", "error");
      });
  }

  function saveMaintenance() {
    db.ref("maintenance").update({
      active: $("maintenance-active").checked,
      title: $("maintenance-title").value.trim() || "Under maintenance",
      message: $("maintenance-message").value.trim() || "App is under maintenance.",
      button_text: $("maintenance-button").value.trim() || "OK"
    })
      .then(function () {
        showToast("Maintenance settings saved.", "success");
      })
      .catch(function (err) {
        showToast(err.message || "Save failed.", "error");
      });
  }

  function renderPlans() {
    var list = $("plan-list");
    list.innerHTML = "";
    if (!plansCache.length) {
      list.innerHTML = '<p style="color:var(--text-muted);margin:0;">No plans yet.</p>';
      return;
    }
    plansCache.forEach(function (plan) {
      var row = document.createElement("div");
      row.className = "plan-row";
      var info = document.createElement("div");
      info.innerHTML =
        "<strong>" + escapeHtml(plan.title) + (plan.active ? "" : " · hidden") + "</strong>" +
        '<div class="plan-meta">' + escapeHtml(plan.id) + " · " + escapeHtml(plan.priceLabel) +
        (plan.badge ? " · " + escapeHtml(plan.badge) : "") + "</div>";
      var removeBtn = document.createElement("button");
      removeBtn.className = "btn btn-danger btn-small";
      removeBtn.type = "button";
      removeBtn.textContent = "Remove";
      removeBtn.addEventListener("click", function () {
        if (!confirm("Remove plan \"" + plan.title + "\"?")) return;
        db.ref("subscription_plans/" + plan.id).remove()
          .then(function () {
            showToast("Plan removed.", "success");
          })
          .catch(function (err) {
            showToast(err.message || "Remove failed.", "error");
          });
      });
      row.appendChild(info);
      row.appendChild(removeBtn);
      list.appendChild(row);
    });
  }

  function addPlan() {
    var planId = $("plan-id").value.trim().toLowerCase().replace(/\s+/g, "_");
    var title = $("plan-title").value.trim();
    var price = $("plan-price").value.trim();
    if (!planId) {
      showToast("Plan ID is required.", "error");
      return;
    }
    if (!title) {
      showToast("Plan title is required.", "error");
      return;
    }
    if (!price) {
      showToast("Price label is required.", "error");
      return;
    }
    var row = {
      title: title,
      subtitle: $("plan-subtitle").value.trim(),
      priceLabel: price,
      active: true,
      sortOrder: plansCache.length + 1
    };
    var badge = $("plan-badge").value.trim();
    if (badge) row.badge = badge;

    db.ref("subscription_plans/" + planId).set(row)
      .then(function () {
        $("plan-id").value = "";
        $("plan-title").value = "";
        $("plan-subtitle").value = "";
        $("plan-price").value = "";
        $("plan-badge").value = "";
        showToast("Plan added.", "success");
      })
      .catch(function (err) {
        showToast(err.message || "Add failed.", "error");
      });
  }

  function sendPush() {
    var title = $("push-title").value.trim();
    var body = $("push-body").value.trim();
    if (!title || !body) {
      showToast("Title and message are required.", "error");
      return;
    }
    var user = auth.currentUser;
    if (!isAdminUser(user)) {
      showToast("Admin sign-in required.", "error");
      return;
    }
    var row = {
      title: title,
      body: body,
      topic: "all_users",
      createdBy: user.uid,
      createdAt: Date.now(),
      status: "pending"
    };
    var imageUrl = $("push-image").value.trim();
    var linkUrl = $("push-link").value.trim();
    if (imageUrl) row.imageUrl = imageUrl;
    if (linkUrl) row.linkUrl = linkUrl;

    $("btn-send-push").disabled = true;
    db.ref("push_broadcasts").push(row)
      .then(function () {
        $("push-title").value = "";
        $("push-body").value = "";
        $("push-image").value = "";
        $("push-link").value = "";
        showToast("Notification queued.", "success");
      })
      .catch(function (err) {
        showToast(err.message || "Send failed.", "error");
      })
      .finally(function () {
        $("btn-send-push").disabled = false;
      });
  }

  function val(snap) {
    var v = snap.val();
    return v == null ? "" : String(v).trim();
  }

  function num(snap) {
    var v = snap.val();
    return typeof v === "number" ? v : parseInt(v, 10) || 0;
  }

  function formatNumber(n) {
    try {
      return new Intl.NumberFormat().format(n);
    } catch (e) {
      return String(n);
    }
  }

  function escapeHtml(text) {
    return String(text)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
})();
