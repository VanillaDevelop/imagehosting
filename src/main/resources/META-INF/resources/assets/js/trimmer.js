/**
 * This script holds data for a video trimmer component that allows users to select a portion of a video
 * @author Vanilla
 */

// Video Trimmer Class
class VideoTrimmer {
    constructor(videoDurationInSeconds) {
        this.duration = videoDurationInSeconds;
        this.startTime = 0;
        this.endTime = videoDurationInSeconds;
        this.isDragging = false;
        this.activeHandle = null;

        this.initializeElements();
        this.setupEventListeners();
        this.updateDisplay();
        this.updatePositionIndicator();
    }

    initializeElements() {
        //Main bar element, to click on to set start/end time
        this.trimmerBar = document.getElementById('trimmerBar');
        //Handle elements which can be dragged to set start/end time
        this.leftHandle = document.getElementById('leftHandle');
        this.rightHandle = document.getElementById('rightHandle');
        //Progress bar element, to show the selected portion of the video
        this.progressBar = document.getElementById('progressBar');
        //Position indicator element, to show the current position of the video
        this.positionIndicator = document.getElementById('positionIndicator');
        //Time display elements
        this.startTimeEl = document.getElementById('start-time');
        this.selectedDurationEl = document.getElementById('selected-duration');
        this.endTimeEl = document.getElementById('end-time');
        //Video player, to show the video being trimmed
        this.videoPlayer = document.getElementById('video-player');
        //Play/pause overlay elements
        this.playPauseOverlay = document.getElementById('play-pause-overlay');
        this.playPauseIcon = document.getElementById('play-pause-icon');
    }

    setupEventListeners() {
        // Start dragging when the handle is clicked
        this.leftHandle.addEventListener('mousedown', (e) => this.startDrag(e, 'left'));
        this.rightHandle.addEventListener('mousedown', (e) => this.startDrag(e, 'right'));
        // Move the handle when dragging
        document.addEventListener('mousemove', (e) => this.drag(e));
        // Stop dragging when the mouse is released
        document.addEventListener('mouseup', () => this.endDrag());
        // Click on timeline to set position
        this.trimmerBar.addEventListener('click', (e) => this.handleTimelineClick(e));
        // Click on video to play/pause
        this.videoPlayer.addEventListener('click', (e) => this.togglePlayPause(e));
    }

    // Start dragging the handle
    startDrag(e, handle) {
        e.preventDefault();
        this.isDragging = true;
        this.activeHandle = handle;
        document.body.style.cursor = 'ew-resize';

        if (handle === 'left') {
            this.leftHandle.classList.add('active');
        } else {
            this.rightHandle.classList.add('active');
        }
    }

    // Update position of the handle while dragging
    drag(e) {
        if (!this.isDragging) return;

        const rect = this.trimmerBar.getBoundingClientRect();
        const handleOffset = this.duration / rect.width * 12;
        const x = e.clientX - rect.left;
        const percentage = Math.max(0, Math.min(1, x / rect.width));
        const time = percentage * this.duration;

        if (this.activeHandle === 'left') {
            this.startTime = Math.min(time, this.endTime - 4 * handleOffset);
        } else {
            this.endTime = Math.max(time, this.startTime + 4 * handleOffset);
        }

        this.updateDisplay();
    }

    // End dragging the handle
    endDrag() {
        if (!this.isDragging) return;

        this.isDragging = false;
        this.activeHandle = null;
        document.body.style.cursor = 'default';

        this.leftHandle.classList.remove('active');
        this.rightHandle.classList.remove('active');
    }

    // Handle clicks on the timeline to set start/end time
    handleTimelineClick(e) {
        if (this.isDragging) return;

        const rect = this.trimmerBar.getBoundingClientRect();
        const x = e.clientX - rect.left;
        
        // Calculate bounds the same way as updatePositionIndicator
        const startPx = (this.startTime / this.duration) * rect.width;
        const endPx = (this.endTime / this.duration) * rect.width - 24;
        
        // Only handle clicks within the active trimmed region
        if (x < startPx + 12 || x > endPx + 12) {
            return;
        }
        
        // Calculate position within the trimmed region
        const relativePosition = (x - startPx - 12) / (endPx - startPx);

        // Set the current time based on the clicked position
        this.videoPlayer.currentTime = this.startTime + relativePosition * (this.endTime - this.startTime);
    }

    // Update the display of the trimmer
    updateDisplay() {
        // Update handle positions
        const leftPercent = (this.startTime / this.duration) * 100;
        const rightPercent = (this.endTime / this.duration) * 100;

        this.leftHandle.style.left = `${leftPercent}%`;
        this.rightHandle.style.left = `${rightPercent}%`;
        this.rightHandle.style.transform = 'translateX(-100%)';

        // Update progress bar
        this.progressBar.style.left = `${leftPercent}%`;
        this.progressBar.style.width = `${rightPercent - leftPercent}%`;

        // Update time displays
        this.startTimeEl.textContent = this.formatTime(this.startTime);
        this.selectedDurationEl.textContent = this.formatTime(this.endTime - this.startTime);
        this.endTimeEl.textContent = this.formatTime(this.endTime);

        // Set player to this position
        if(this.activeHandle !== null) {
            this.videoPlayer.currentTime =
                this.activeHandle === 'left' ? this.startTime : Math.max(this.endTime - 1, this.startTime);
        }
        this.videoPlayer.play();
    }

    // Format time in seconds to MM:SS
    formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    }


    // Periodically update the position indicator
    updatePositionIndicator() {
        if (this.videoPlayer) {
            const currentTime = this.videoPlayer.currentTime;
            const indicatorPosition = (currentTime - this.startTime) / (this.endTime - this.startTime);
            const bounds = this.trimmerBar.getBoundingClientRect();
            const startPx = (this.startTime / this.duration) * bounds.width;
            const endPx = (this.endTime / this.duration) * bounds.width - 24;

            this.positionIndicator.style.left = `${startPx + 12 + indicatorPosition * (endPx - startPx)}px`;

            // Loop back around if the video ends
            if (currentTime >= this.endTime) {
                this.videoPlayer.currentTime = this.startTime;
                this.videoPlayer.play();
            }
        }

        requestAnimationFrame(() => this.updatePositionIndicator());
    }

    // Toggle play/pause when video is clicked
    togglePlayPause(e) {
        e.preventDefault();
        
        if (this.videoPlayer.paused) {
            this.videoPlayer.play();
            this.showPlayPauseIcon('pi-play');
        } else {
            this.videoPlayer.pause();
            this.showPlayPauseIcon('pi-pause');
        }
    }

    // Show play/pause icon with flash animation
    showPlayPauseIcon(iconClass) {
        // Update icon
        this.playPauseIcon.className = `pi ${iconClass}`;
        
        // Remove existing animation class
        this.playPauseOverlay.classList.remove('show');
        
        // Force reflow to ensure class removal takes effect
        this.playPauseOverlay.offsetHeight;
        
        // Add animation class
        this.playPauseOverlay.classList.add('show');
        
        // Remove animation class after animation completes
        setTimeout(() => {
            this.playPauseOverlay.classList.remove('show');
        }, 600);
    }

    destroy() {
        // Remove event listeners
        this.leftHandle.removeEventListener('mousedown', this.startDrag);
        this.rightHandle.removeEventListener('mousedown', this.startDrag);
        document.removeEventListener('mousemove', this.drag);
        document.removeEventListener('mouseup', this.endDrag);
        this.trimmerBar.removeEventListener('click', this.handleTimelineClick);
        this.videoPlayer.removeEventListener('click', this.togglePlayPause);

        // Clear elements
        this.trimmerBar = null;
        this.leftHandle = null;
        this.rightHandle = null;
        this.progressBar = null;
        this.startTimeEl = null;
        this.selectedDurationEl = null;
        this.endTimeEl = null;
        this.videoPlayer = null;
    }
}