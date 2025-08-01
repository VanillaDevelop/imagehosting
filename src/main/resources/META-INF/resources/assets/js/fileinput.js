const dropZone = document.querySelector('.video-drop-zone');
const fileInput = document.getElementById('video-input');
const trimmerContainer = document.getElementById('trimmer-container');

/**
 * Handles file selection from input or drag-and-drop.
 * @param file - The file selected by the user.
 */
function handleFileSelect(file) {
    if (file && file.type.startsWith('video/')) {
        const video = document.getElementById('video-player');
        const source = document.getElementById('video-source');

        source.src = URL.createObjectURL(file);
        source.type = file.type;

        video.addEventListener('loadedmetadata', function() {
            // Initialize the video trimmer as soon as metadata is loaded
            const duration = video.duration;
            new VideoTrimmer(duration);
        });

        video.load();
        video.play();

        dropZone.style.display = 'none';
        trimmerContainer.style.display = 'block';

    } else if (file) {
        alert('Please select a video file.');
    }
}

// File input change event
fileInput.addEventListener('change', function (event) {
    const file = event.target.files[0];
    handleFileSelect(file);
});

// Drag and drop functionality
dropZone.addEventListener('dragover', function(e) {
    e.preventDefault();
    dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', function(e) {
    e.preventDefault();
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