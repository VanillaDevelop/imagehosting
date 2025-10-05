/**
 * This script manages the video file input, providing drag-and-drop functionality as well as access via a file chooser.
 * When a video file is selected, it initializes a video trimmer interface allowing users to trim and upload the video.
 * Requires videotrimmer.js for the trimming functionality.
 *
 * @author Vanilla
 */

// Element for the drag-and-drop zone
const dropZone = document.querySelector('.video-drop-zone');
// File input element
const fileInput = document.getElementById('video-input');
// Container for the video trimmer interface
const trimmerContainer = document.getElementById('trimmer-container');
// Video player element
const videoPlayer = document.getElementById('video-player');
// Video player source element
const videoSource = document.getElementById('video-source');
// Active video trimmer instance
let videoTrimmerInstance = null;

/**
 * Handles file selection from input or drag-and-drop.
 * @param file - The file selected by the user.
 */
function handleFileSelect(file) {
    if (file && file.type.startsWith('video/')) {
        // Set the file input element for form submission
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        fileInput.files = dataTransfer.files;

        // Load the video into the player
        videoSource.src = URL.createObjectURL(file);
        videoSource.type = file.type;

        // Initialize a new video trimmer as soon as metadata is loaded - clean up previous instance if any exist
        videoPlayer.addEventListener('loadedmetadata', function() {
            const duration = videoPlayer.duration;
            if(videoTrimmerInstance) {
                videoTrimmerInstance.destroy();
            }
            videoTrimmerInstance = new VideoTrimmer(duration);
        });

        // Load and play the video
        videoPlayer.load();
        videoPlayer.play();

        // Hide the drop zone and show the trimmer interface
        dropZone.style.display = 'none';
        trimmerContainer.style.display = 'block';

    } else if (file) {
        alert('Please select a video file.');
    }
}

// File chooser events
dropZone.addEventListener('click', function() {
    fileInput.click();
})

fileInput.addEventListener('change', function (event) {
    const file = event.target.files[0];
    handleFileSelect(file);
});

// Drag-and-drop events
dropZone.addEventListener('dragover', function(e) {
    // Required for browser to allow drop
    e.preventDefault();
    dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', function() {
    dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', function(e) {
    e.preventDefault();
    dropZone.classList.remove('dragover');

    const files = e.dataTransfer.files;
    if (files.length > 0) {
        handleFileSelect(files[0]);
    }
});