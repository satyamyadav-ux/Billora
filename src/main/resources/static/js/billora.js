/**
 * Billora Frontend Interactive Script
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Sidebar Toggle (Mobile / Desktop)
    const sidebarToggleBtn = document.getElementById('sidebarToggleBtn');
    const appSidebar = document.getElementById('appSidebar');
    const sidebarBackdrop = document.getElementById('sidebarBackdrop');

    if (sidebarToggleBtn && appSidebar) {
        sidebarToggleBtn.addEventListener('click', () => {
            appSidebar.classList.toggle('show');
            if (sidebarBackdrop) {
                sidebarBackdrop.classList.toggle('show');
            }
        });
    }

    if (sidebarBackdrop) {
        sidebarBackdrop.addEventListener('click', () => {
            appSidebar.classList.remove('show');
            sidebarBackdrop.classList.remove('show');
        });
    }

    // 2. Global Quick Search Keyboard Shortcut (Cmd+K / Ctrl+K)
    const globalSearchInput = document.getElementById('globalSearchInput');
    window.addEventListener('keydown', (e) => {
        if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
            e.preventDefault();
            if (globalSearchInput) {
                globalSearchInput.focus();
            }
        }
    });

    // 3. Initialize Bootstrap Tooltips
    if (typeof bootstrap !== 'undefined') {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));
    }

    // 4. Interactive Subscription Tier Simulator (Homepage Widget)
    const subSlider = document.getElementById('subscriberRange');
    const planSelect = document.getElementById('calcPlanSelect');
    const estimatedMrrEl = document.getElementById('estimatedMrr');
    const estimatedArrEl = document.getElementById('estimatedArr');

    function updateCalculator() {
        if (!subSlider || !estimatedMrrEl) return;
        const count = parseInt(subSlider.value, 10);
        const planPrice = parseFloat(planSelect ? planSelect.value : 29);
        const mrr = count * planPrice;
        const arr = mrr * 12;

        const subCountDisplay = document.getElementById('subscriberCountDisplay');
        if (subCountDisplay) {
            subCountDisplay.textContent = count.toLocaleString();
        }

        estimatedMrrEl.textContent = '$' + mrr.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 });
        if (estimatedArrEl) {
            estimatedArrEl.textContent = '$' + arr.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 });
        }
    }

    if (subSlider) {
        subSlider.addEventListener('input', updateCalculator);
    }
    if (planSelect) {
        planSelect.addEventListener('change', updateCalculator);
    }
    updateCalculator();
});
