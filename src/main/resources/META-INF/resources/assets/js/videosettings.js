/**
 * Small script to toggle the visibility of the video settings panel.
 *
 * @author Vanilla
 */

const settingsPanel = document.getElementById('settings-panel');
const settingsIcon = document.getElementById('settings-icon');

/**
 * Toggles the visibility of the settings panel and updates the icon accordingly.
 */
function toggleSettings() {
    if (settingsPanel.style.display === 'none') {
        settingsPanel.style.display = 'block';
        settingsIcon.classList.remove('pi-chevron-down');
        settingsIcon.classList.add('pi-chevron-up');
    } else {
        settingsPanel.style.display = 'none';
        settingsIcon.classList.remove('pi-chevron-up');
        settingsIcon.classList.add('pi-chevron-down');
    }
}