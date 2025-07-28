let trimmer = null;

// Get drop zone element
const dropZone = document.querySelector('.video-drop-zone');
const fileInput = document.getElementById('video-input');

// Handle file selection (both click and drop)
function handleFileSelect(file) {
    if (file && file.type.startsWith('video/')) {
        const video = document.getElementById('videoPlayer');
        const source = document.getElementById('videoSource');

        source.src = URL.createObjectURL(file);
        source.type = file.type;

        video.addEventListener('loadedmetadata', function() {
            const duration = video.duration;
            trimmer = new VideoTrimmer(duration);
        });

        video.load();
        video.play();

        // Update drop zone to show selected file
        dropZone.querySelector('.upload-text').textContent = file.name;
        dropZone.querySelector('.upload-hint').textContent = 'Video loaded successfully';
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

// Handle trim button click
document.getElementById('trimButton').addEventListener('click', function(e) {
    e.preventDefault();

    if (!trimmer) {
        alert('Please select a video file first.');
        return;
    }

    // Set the hidden fields with current trimmer values
    document.getElementById('startTimeSeconds').value = trimmer.startTime;
    document.getElementById('endTimeSeconds').value = trimmer.endTime;

    // Get video title
    const videoTitle = document.getElementById('videoTitleInput').value;
    document.getElementById('videoTitle').value = videoTitle;

    // Submit the form
    document.getElementById('videoForm').submit();
});