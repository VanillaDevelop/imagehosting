/**
 * Simple script to copy the video URL to clipboard on click on the corresponding button.
 *
 * @author Vanilla
 */


const copyButton = document.getElementById('copy-link-button');

if (copyButton) {
    copyButton.addEventListener('click', () => {
        const videoUrl = document.querySelector('meta[name="twitter:url"]').content;
        navigator.clipboard.writeText(videoUrl).then(() => {
            alert('Video URL copied to clipboard!');
        }).catch(() => {
            console.error('Failed to copy URL!');
        });
    });
}