	/* =============================================
   Financial CRM – Main JavaScript
   ============================================= */

/* ── CSRF token helper for AJAX ──────────────── */
function getCsrfToken() {
  const meta = document.querySelector('meta[name="_csrf"]');
  return meta ? meta.getAttribute('content') : '';
}
function getCsrfHeader() {
  const meta = document.querySelector('meta[name="_csrf_header"]');
  return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
}
function getCookieCsrf() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

/* ── Attach CSRF to all fetch calls ──────────── */
const _fetch = window.fetch;
window.fetch = function(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  if (['POST','PUT','DELETE','PATCH'].includes(method)) {
    options.headers = options.headers || {};
    options.headers['X-XSRF-TOKEN'] = getCookieCsrf();
  }
  return _fetch(url, options);
};

/* ── Confirm delete forms ────────────────────── */
document.addEventListener('DOMContentLoaded', function () {

  // Auto-close alerts after 5 seconds
  setTimeout(function () {
    document.querySelectorAll('.alert.fade').forEach(el => {
      const alert = bootstrap.Alert.getOrCreateInstance(el);
      alert.close();
    });
  }, 5000);

  // Tooltip initialization
  document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
    new bootstrap.Tooltip(el);
  });

  // Mark notifications as read via AJAX when clicking bell
  document.querySelectorAll('[data-notify-id]').forEach(el => {
    el.addEventListener('click', function () {
      const id = this.dataset.notifyId;
      fetch('/notifications/mark-read/' + id, { method: 'POST' })
        .then(r => r.json())
        .then(d => {
          if (d.success) {
            const badge = document.querySelector('.notification-badge');
            if (badge) badge.textContent = d.unreadCount || '';
            if (d.unreadCount === 0 && badge) badge.style.display = 'none';
          }
        });
    });
  });

  // Input number formatting (readonly display)
  document.querySelectorAll('.input-currency').forEach(inp => {
    inp.addEventListener('blur', function () {
      const val = parseFloat(this.value.replace(/,/g, ''));
      if (!isNaN(val)) this.value = val.toLocaleString('en-IN');
    });
  });

  // Mobile sidebar toggle
  const toggle = document.getElementById('sidebarToggle');
  const wrapper = document.getElementById('wrapper');
  if (toggle && wrapper) {
    toggle.addEventListener('click', function () {
      wrapper.classList.toggle('sidebar-collapsed');
      wrapper.classList.toggle('sidebar-open');
    });
  }

  // Customer mobile number validation
  document.querySelectorAll('input[name="mobileNumber"]').forEach(inp => {
    inp.addEventListener('blur', function () {
      const val = this.value.replace(/\s/g,'');
      if (val && !/^[6-9]\d{9}$/.test(val)) {
        this.classList.add('is-invalid');
        let fb = this.nextElementSibling;
        if (!fb || !fb.classList.contains('invalid-feedback')) {
          fb = document.createElement('div');
          fb.className = 'invalid-feedback';
          this.after(fb);
        }
        fb.textContent = 'Please enter a valid 10-digit mobile number.';
      } else {
        this.classList.remove('is-invalid');
      }
    });
  });

  // Confirm before delete forms
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', function (e) {
      if (!confirm(this.dataset.confirm || 'Are you sure?')) {
        e.preventDefault();
      }
    });
  });

  // Auto-submit date filter forms
  document.querySelectorAll('input[type="date"].auto-submit').forEach(inp => {
    inp.addEventListener('change', function () {
      this.closest('form').submit();
    });
  });

  // Pincode auto-validate
  document.querySelectorAll('input[name="pincode"]').forEach(inp => {
    inp.addEventListener('input', function () {
      this.value = this.value.replace(/\D/g, '').slice(0, 6);
    });
  });

});

/* ── Format INR number ───────────────────────── */
function formatINR(n) {
  if (isNaN(n)) return '₹0';
  return '₹' + Number(n).toLocaleString('en-IN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
}

/* ── Show toast notification ─────────────────── */
function showToast(message, type = 'success') {
  const container = document.getElementById('toastContainer') || (() => {
    const d = document.createElement('div');
    d.id = 'toastContainer';
    d.className = 'position-fixed bottom-0 end-0 p-3';
    d.style.zIndex = '9999';
    document.body.appendChild(d);
    return d;
  })();

  const id = 'toast_' + Date.now();
  const bgClass = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-primary';

  container.insertAdjacentHTML('beforeend', `
    <div id="${id}" class="toast align-items-center text-white ${bgClass} border-0 show" role="alert">
      <div class="d-flex">
        <div class="toast-body">${message}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
      </div>
    </div>
  `);

  setTimeout(() => {
    const el = document.getElementById(id);
    if (el) el.remove();
  }, 4000);
}

/* ── Customer live search (shared across pages) ── */
function initCustomerSearch(inputId, hiddenId, resultsId) {
  let timer;
  const input = document.getElementById(inputId);
  const hidden = document.getElementById(hiddenId);
  const results = document.getElementById(resultsId);
  if (!input) return;

  input.addEventListener('input', function () {
    clearTimeout(timer);
    const q = this.value.trim();
    if (q.length < 2) { results.style.display = 'none'; return; }
    timer = setTimeout(() => {
      fetch('/api/customers/search?q=' + encodeURIComponent(q))
        .then(r => r.json())
        .then(data => {
          results.innerHTML = '';
          if (!data.length) { results.style.display = 'none'; return; }
          data.forEach(c => {
            const a = document.createElement('a');
            a.className = 'list-group-item list-group-item-action py-2';
            a.href = '#';
            a.innerHTML = `<strong>${c.customerName}</strong> <span class="text-muted small ms-2">${c.mobileNumber}</span>`;
            a.addEventListener('click', e => {
              e.preventDefault();
              input.value = c.customerName;
              hidden.value = c.id;
              results.style.display = 'none';
            });
            results.appendChild(a);
          });
          results.style.display = 'block';
        });
    }, 300);
  });

  document.addEventListener('click', e => {
    if (!input.contains(e.target) && !results.contains(e.target)) {
      results.style.display = 'none';
    }
  });
}
