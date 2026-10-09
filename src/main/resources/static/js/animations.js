/**
 * 
 */(function () {
  'use strict';

  var REDUCE_MOTION = window.matchMedia &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function animatePageEntrance() {
    if (REDUCE_MOTION || typeof gsap === 'undefined') return;
    var navbar = document.querySelector('.navbar');
    var sidebar = document.getElementById('sidebar-wrapper');
    var main = document.getElementById('mainContentWrapper');
    var tl = gsap.timeline();
    if (navbar) tl.from(navbar, { y: -22, opacity: 0, duration: 0.5, ease: 'power2.out' });
    if (sidebar) tl.from(sidebar, { x: -36, opacity: 0, duration: 0.5, ease: 'power2.out' }, '<');
    if (main && main.children.length) {
      tl.from(Array.prototype.slice.call(main.children), {
        y: 22, opacity: 0, duration: 0.55, ease: 'power3.out', stagger: 0.08
      }, '-=0.25');
    }
    return tl;
  }

  function initButtonRipple() {
    document.addEventListener('click', function (e) {
      var btn = e.target.closest && e.target.closest('.btn');
      if (!btn) return;
      var rect = btn.getBoundingClientRect();
      var size = Math.max(rect.width, rect.height);
      var ripple = document.createElement('span');
      ripple.className = 'btn-ripple';
      ripple.style.width = ripple.style.height = size + 'px';
      ripple.style.left = (e.clientX - rect.left - size / 2) + 'px';
      ripple.style.top = (e.clientY - rect.top - size / 2) + 'px';
      btn.appendChild(ripple);
      ripple.addEventListener('animationend', function () { ripple.remove(); });
    });
  }

  function initSidebarToggleEnhancement() {
    var toggle = document.getElementById('sidebarToggle');
    var wrapper = document.getElementById('wrapper');
    if (!toggle || !wrapper) return;
    var icon = toggle.querySelector('i');
    if (!icon) return;
    toggle.addEventListener('click', function () {
      var collapsed = wrapper.classList.contains('sidebar-collapsed');
      icon.style.transform = collapsed ? 'rotate(180deg)' : 'rotate(0deg)';
    });
  }

  function initNavbarScroll() {
    var navbar = document.querySelector('.navbar');
    if (!navbar) return;
    function onScroll() {
      if (window.scrollY > 8) navbar.classList.add('navbar-scrolled');
      else navbar.classList.remove('navbar-scrolled');
    }
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  function resolveCounterNode(el) {
    var span = el.querySelector(':scope > span');
    if (span && /\d/.test(span.textContent)) return span;
    return el;
  }

  function animateCounter(el) {
    var node = resolveCounterNode(el);
    var original = node.textContent.trim();
    var match = original.match(/^([^\d]*)([\d,]+(?:\.\d+)?)([^\d]*)$/);
    if (!match) return;
    var prefix = match[1], numStr = match[2], suffix = match[3];
    var cleanNum = numStr.replace(/,/g, '');
    var end = parseFloat(cleanNum);
    if (isNaN(end)) return;
    var decimals = cleanNum.indexOf('.') > -1 ? cleanNum.split('.')[1].length : 0;
    if (REDUCE_MOTION || typeof gsap === 'undefined') { node.textContent = original; return; }
    var counter = { value: 0 };
    gsap.to(counter, {
      value: end, duration: 1.4, ease: 'power3.out',
      onUpdate: function () {
        node.textContent = prefix + counter.value.toLocaleString('en-IN', {
          minimumFractionDigits: decimals, maximumFractionDigits: decimals
        }) + suffix;
      },
      onComplete: function () { node.textContent = original; }
    });
  }

  function animateCounters() {
    document.querySelectorAll('.fs-2.fw-bold, .fs-1.fw-bold, .counter-value').forEach(animateCounter);
  }

  function animateTableRows() {
    if (REDUCE_MOTION || typeof gsap === 'undefined') return;
    var rows = document.querySelectorAll('.table tbody tr');
    if (!rows.length) return;
    gsap.from(rows, { opacity: 0, x: -16, duration: 0.45, stagger: 0.045, ease: 'power2.out', delay: 0.35 });
  }

  function initNotificationPulse() {
    var badge = document.querySelector('.notif-badge');
    if (badge) badge.classList.add('animate__animated', 'animate__pulse', 'animate__infinite', 'animate__slower');
  }

  window.showToast = function (message, type) {
    type = type || 'success';
    var container = document.getElementById('toastContainer');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'position-fixed bottom-0 end-0 p-3';
      container.style.zIndex = '9999';
      document.body.appendChild(container);
    }
    var id = 'toast_' + Date.now();
    var bgClass = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-primary';
    var animIn = REDUCE_MOTION ? '' : 'animate__animated animate__slideInRight';
    container.insertAdjacentHTML('beforeend',
      '<div id="' + id + '" class="toast align-items-center text-white ' + bgClass + ' border-0 show ' + animIn + '" role="alert">' +
        '<div class="d-flex"><div class="toast-body">' + message + '</div>' +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div></div>'
    );
    var toastEl = document.getElementById(id);
    var remove = function () {
      if (!toastEl) return;
      if (REDUCE_MOTION) { toastEl.remove(); return; }
      toastEl.classList.remove('animate__slideInRight');
      toastEl.classList.add('animate__slideOutRight');
      toastEl.addEventListener('animationend', function () { toastEl.remove(); });
    };
    toastEl.querySelector('.btn-close').addEventListener('click', remove);
    setTimeout(remove, 4000);
  };

  function hidePageLoader() {
    var loader = document.getElementById('pageLoader');
    if (loader) loader.classList.add('loader-hidden');
  }

  function initAOS() {
      if (typeof AOS === 'undefined') return;

      document.querySelectorAll('.card:not(.kpi-card):not([data-aos]), .list-group-item:not(.sidebar-link):not([data-aos])')
        .forEach(function (el, i) {
          if (el.querySelector('canvas')) return; // skip chart cards — Chart.js already animates itself
          el.setAttribute('data-aos', 'fade-up');
          el.setAttribute('data-aos-delay', String(Math.min(i * 40, 300)));
        });

      AOS.init({ duration: 550, easing: 'ease-out-cubic', once: true, offset: 30, disable: REDUCE_MOTION });
    }

  function animateAlerts() {
    if (REDUCE_MOTION) return;
    document.querySelectorAll('.alert').forEach(function (el) {
      el.classList.add('animate__animated', 'animate__fadeInDown', 'animate__faster');
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    animatePageEntrance();
    animateAlerts();
    initButtonRipple();
    initSidebarToggleEnhancement();
    initNavbarScroll();
    animateCounters();
    animateTableRows();
    initNotificationPulse();
    setTimeout(initAOS, 600);
  });

  window.addEventListener('load', hidePageLoader);
  setTimeout(hidePageLoader, 1200);
})();