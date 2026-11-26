/** Landing page — logo, mobile menu, screenshots, Play Store link. */
(function () {
  document.getElementById("year").textContent = new Date().getFullYear();

  var logoUrl = window.APP_LOGO_URL || "";
  if (logoUrl) {
    ["brand-logo", "cta-logo", "footer-logo", "favicon"].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.src = logoUrl;
    });
  }

  initMobileMenu();

  var shots = window.LANDING_SCREENSHOTS || [];
  var fallback = (window.DEFAULT_DOWNLOAD_URL || "").trim()
    || "https://play.google.com/store/apps/details?id=com.chill.with.examsaathi";

  function setLinks(url) {
    var href = url && url.trim() ? url.trim() : fallback;
    document.querySelectorAll(".js-download-link").forEach(function (el) {
      el.href = href;
    });
  }

  setLinks(fallback);

  if (shots.length) {
    buildGallery(shots);
    initHeroCarousel(shots);
  }

  function initMobileMenu() {
    var toggle = document.getElementById("menu-toggle");
    var nav = document.getElementById("site-nav");
    if (!toggle || !nav) return;

    function closeMenu() {
      document.body.classList.remove("nav-open");
      toggle.setAttribute("aria-expanded", "false");
      toggle.setAttribute("aria-label", "Open menu");
    }

    function openMenu() {
      document.body.classList.add("nav-open");
      toggle.setAttribute("aria-expanded", "true");
      toggle.setAttribute("aria-label", "Close menu");
    }

    toggle.addEventListener("click", function () {
      if (document.body.classList.contains("nav-open")) {
        closeMenu();
      } else {
        openMenu();
      }
    });

    nav.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        if (window.innerWidth <= 768) closeMenu();
      });
    });

    window.addEventListener("resize", function () {
      if (window.innerWidth > 768) closeMenu();
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") closeMenu();
    });
  }

  function buildGallery(shots) {
    var grid = document.getElementById("screenshots-grid");
    if (!grid) return;
    grid.innerHTML = "";
    shots.forEach(function (url, index) {
      var card = document.createElement("article");
      card.className = "screenshot-card";
      var img = document.createElement("img");
      img.src = url;
      img.alt = "Bhagavat Gita screenshot " + (index + 1);
      img.loading = index < 3 ? "eager" : "lazy";
      card.appendChild(img);
      grid.appendChild(card);
    });
  }

  function initHeroCarousel(shots) {
    var heroImg = document.getElementById("hero-screenshot");
    var dotsWrap = document.getElementById("hero-dots");
    if (!heroImg || !dotsWrap || !shots.length) return;

    var index = 0;
    heroImg.src = shots[0];
    heroImg.alt = "Bhagavat Gita app screenshot 1";

    shots.forEach(function (_, i) {
      var dot = document.createElement("button");
      dot.type = "button";
      dot.className = "device-dot" + (i === 0 ? " active" : "");
      dot.setAttribute("aria-label", "Screenshot " + (i + 1));
      dot.addEventListener("click", function () {
        goTo(i);
        resetTimer();
      });
      dotsWrap.appendChild(dot);
    });

    function goTo(i) {
      index = i;
      heroImg.style.opacity = "0";
      setTimeout(function () {
        heroImg.src = shots[index];
        heroImg.alt = "Bhagavat Gita app screenshot " + (index + 1);
        heroImg.style.opacity = "1";
      }, 180);
      dotsWrap.querySelectorAll(".device-dot").forEach(function (d, j) {
        d.classList.toggle("active", j === index);
      });
    }

    var timer;
    function resetTimer() {
      clearInterval(timer);
      timer = setInterval(function () {
        goTo((index + 1) % shots.length);
      }, 4200);
    }

    resetTimer();
  }

  try {
    firebase.initializeApp(window.FIREBASE_CONFIG);
    firebase.database().ref("website_config/downloadUrl").once("value").then(function (snap) {
      var websiteUrl = snap.val();
      if (typeof websiteUrl === "string" && websiteUrl.trim()) {
        setLinks(websiteUrl);
        return null;
      }
      return firebase.database().ref("app_update_config/storeUrl").once("value");
    }).then(function (snap) {
      if (!snap) return;
      var url = snap.val();
      if (typeof url === "string" && url.trim()) {
        setLinks(url);
      }
    }).catch(function () {});
  } catch (e) {}
})();
