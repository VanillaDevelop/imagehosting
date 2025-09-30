function toggleSettings() {
    const panel = document.getElementById('settings-panel');
    const chevron = document.getElementById('settings-chevron');

    if (panel.style.display === 'none' || panel.style.display === '') {
        panel.style.display = 'block';
        chevron.classList.remove('pi-chevron-down');
        chevron.classList.add('pi-chevron-up');
    } else {
        panel.style.display = 'none';
        chevron.classList.remove('pi-chevron-up');
        chevron.classList.add('pi-chevron-down');
    }
}