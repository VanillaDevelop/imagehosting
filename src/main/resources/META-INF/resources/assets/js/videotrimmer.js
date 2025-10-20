/**
 * This script composes a class for a video trimmer component that allows users to select a portion of a video.
 * The component is subsequently initialized when a video file is selected via file input or drag-and-drop.
 *
 * Note that for visual alignment, there are two separate systems at play:
 * - Start and end times are calculated based on the handle positions, not taking into consideration the handle widths.
 *   For example, optically, to the user, the left handle is at 12px and the right handle is at -12px from the edges of
 *   the trimmer bar. However, for the purpose of calculating start and end times, the handles are considered to be at
 *   0px offset from the edges of the trimmer bar - the outer edges of the handles are the basis for calculation.
 *
 * - The position indicator, however, is calculated with the handle widths in mind. So, if the left handle is at 0%,
 *   for the position indicator, 12px is considered the start of the trimmer bar, and if the right handle is at 100%,
 *   the position indicator considers the end of the trimmer bar to be -12px from the right edge. When the user clicks
 *   into the timeline, the click position is also calculated with the handle widths in mind.
 *
 * @author Vanilla
 */

const HANDLE_WIDTH_PX = 12;
const PLAY_ANIMATION_DURATION_MS = 600;

// Video Trimmer Class
class VideoTrimmer {

    /**
     * Initializes the video trimmer for a video with the given video duration.
     * @param videoDurationInSeconds - The total duration of the video in seconds.
     */
    constructor(videoDurationInSeconds) {
        this.duration = videoDurationInSeconds;
        this.startTime = 0;
        this.endTime = videoDurationInSeconds;
        this.activeHandle = null;
        this.handlers = {
            startDragLeft: () => this.startDrag('left'),
            startDragRight: () => this.startDrag('right'),
            drag: (e) => this.drag(e),
            endDrag: () => this.endDrag(),
            handleTimelineClick: (e) => this.handleTimelineClick(e),
            togglePlayPause: () => this.togglePlayPause(),
            submitForm: (e) => this.submitForm(e)
        }

        this.initializeElements();
        this.setupEventListeners();
        this.updateTrimmerDisplay();
        this.updatePositionIndicator();
    }

    /**
     * Initializes references to all necessary DOM elements.
     */
    initializeElements() {
        //Main bar element which acts as the full timeline for the video
        this.trimmerBar = document.getElementById('trimmer-bar');
        //Handle elements which can be dragged to set start/end time
        this.leftHandle = document.getElementById('left-handle');
        this.rightHandle = document.getElementById('right-handle');
        //Progress bar overlay element, to show the selected portion of the video
        this.progressBar = document.getElementById('progress-bar');
        //Position indicator element, to show the current position of the video
        this.positionIndicator = document.getElementById('position-indicator');
        //Display elements for start time, selected duration, and end time
        this.startTimeEl = document.getElementById('start-time');
        this.selectedDurationEl = document.getElementById('selected-duration');
        this.endTimeEl = document.getElementById('end-time');
        //Video player, to show the video being trimmed
        this.videoPlayer = document.getElementById('video-player');
        //Play/pause overlay elements - container to show/hide and icon to change
        this.playPauseOverlay = document.getElementById('play-pause-overlay');
        this.playPauseIcon = document.getElementById('play-pause-icon');
        //Button for submitting the form
        this.trimButton = document.getElementById('trim-button');
        //Input for video title
        this.videoTitleInput = document.getElementById('video-title-input');

        //Hidden form fields for submission
        this.startTimeSecondsForm = document.getElementById('start-time-seconds');
        this.endTimeSecondsForm = document.getElementById('end-time-seconds');
        this.videoTitleForm = document.getElementById('video-title');
        this.videoFormField = document.getElementById('video-upload-form');
    }

    /**
     * Sets up all necessary event listeners for user interaction.
     */
    setupEventListeners() {
        // Start dragging when the handle is clicked
        this.leftHandle.addEventListener('mousedown', this.handlers.startDragLeft);
        this.rightHandle.addEventListener('mousedown', this.handlers.startDragRight);
        // Move the handle when dragging
        document.addEventListener('mousemove', this.handlers.drag);
        // Stop dragging when the mouse is released
        document.addEventListener('mouseup', this.handlers.endDrag);
        // Click on timeline to set video position
        this.trimmerBar.addEventListener('click', this.handlers.handleTimelineClick);
        // Click on video to toggle play/pause
        this.videoPlayer.addEventListener('click', this.handlers.togglePlayPause);
        // Click on trim button to submit the form
        this.trimButton.addEventListener('click', this.handlers.submitForm);
    }

    /**
     * Cleans up event listeners when the trimmer is no longer needed.
     */
    destroy() {
        this.leftHandle.removeEventListener('mousedown', this.handlers.startDragLeft);
        this.rightHandle.removeEventListener('mousedown', this.handlers.startDragRight);
        document.removeEventListener('mousemove', this.handlers.drag);
        document.removeEventListener('mouseup', this.handlers.endDrag);
        this.trimmerBar.removeEventListener('click', this.handlers.handleTimelineClick);
        this.videoPlayer.removeEventListener('click', this.handlers.togglePlayPause);
        this.trimButton.removeEventListener('click', this.handlers.submitForm);
    }

    /**
     * Start dragging a handle
     * @param handle - 'left' or 'right' to indicate which handle is being dragged
     */
    startDrag(handle) {
        this.activeHandle = handle;
        document.body.style.cursor = 'ew-resize';

        if (handle === 'left') {
            this.leftHandle.classList.add('active');
        } else {
            this.rightHandle.classList.add('active');
        }
    }

    /**
     * Mouse move event - when a handle is being dragged, update its position
     * @param e - Mouse event, to determine the current mouse position
     */
    drag(e) {
        if (this.activeHandle === null) return;

        // Get the dimensions of the trimmer bar
        const rect = this.trimmerBar.getBoundingClientRect();
        // Calculate the x position relative to the trimmer bar
        const x = e.clientX - rect.left;
        // Calculate the percentage position within the trimmer bar, clamped between 0 and 1
        const percentage = Math.max(0, Math.min(1, x / rect.width));
        // Calculate the corresponding time in seconds for the video
        const time = percentage * this.duration;

        // Enforce a minimum distance between start and end times of 10% of the total duration or 5 seconds, whichever is smaller
        const minimumDistance = Math.min(this.duration * 0.1, 5);

        if (this.activeHandle === 'left') {
            this.startTime = Math.min(time, this.endTime - minimumDistance);
        } else {
            this.endTime = Math.max(time, this.startTime + minimumDistance);
        }

        // Update the display to reflect the new positions
        this.updateTrimmerDisplay();
    }

    /**
     * Stops dragging the active handle
     */
    endDrag() {
        this.activeHandle = null;
        document.body.style.cursor = 'default';

        this.leftHandle.classList.remove('active');
        this.rightHandle.classList.remove('active');
    }

    /**
     * When a user clicks on the timeline, set the video position accordingly
     * @param e - Mouse event, to determine the click position
     */
    handleTimelineClick(e) {
        if (this.activeHandle !== null) return;

        // Get the dimensions of the trimmer bar
        const rect = this.trimmerBar.getBoundingClientRect();
        // Calculate the x position relative to the trimmer bar
        const x = e.clientX - rect.left;

        // Calculate the pixel offsets of the start and end times, accounting for handle width
        const startPx = (this.startTime / this.duration) * rect.width + HANDLE_WIDTH_PX;
        const endPx = (this.endTime / this.duration) * rect.width - HANDLE_WIDTH_PX;
        
        // Only handle clicks within the active trimmed region
        if (x < startPx || x > endPx) {
            return;
        }
        
        // Calculate position within the trimmed region
        const relativePosition = (x - startPx) / (endPx - startPx);

        // Set the current time of the player based on the clicked position
        this.videoPlayer.currentTime = this.startTime + relativePosition * (this.endTime - this.startTime);
    }

    /**
     * Updates the visual display of the trimmer, including handle positions, progress bar, and time displays.
     */
    updateTrimmerDisplay() {
        // Calculate percentages for handle positions based on start and end times
        const leftPercent = (this.startTime / this.duration) * 100;
        const rightPercent = (this.endTime / this.duration) * 100;

        // Update handle positions
        this.leftHandle.style.left = `${leftPercent}%`;
        this.rightHandle.style.left = `${rightPercent}%`;

        // Update progress bar coverage (blue bar between handles)
        this.progressBar.style.left = `${leftPercent}%`;
        this.progressBar.style.width = `${rightPercent - leftPercent}%`;

        // Update time displays
        this.startTimeEl.textContent = this.formatTime(this.startTime);
        this.selectedDurationEl.textContent = this.formatTime(this.endTime - this.startTime);
        this.endTimeEl.textContent = this.formatTime(this.endTime);

        // Set player to this position
        // If we are dragging the right handle, skip to slightly before the end time so the user can see the end.
        if(this.activeHandle !== null) {
            this.videoPlayer.currentTime =
                this.activeHandle === 'left' ? this.startTime : Math.max(this.endTime - 1, this.startTime);
        }
    }

    /**
     * Formats a time input in seconds to a string in the format MM:SS
     * @param seconds - Time in seconds
     * @returns Formatted time string
     */
    formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }


    /**
     * Continuously updates the position indicator to reflect the current playback position of the video.
     */
    updatePositionIndicator() {
        if (this.videoPlayer) {
            const currentTime = this.videoPlayer.currentTime;
            const indicatorPercentage = (currentTime - this.startTime) / (this.endTime - this.startTime);
            const bounds = this.trimmerBar.getBoundingClientRect();
            const startPx = (this.startTime / this.duration) * bounds.width + HANDLE_WIDTH_PX;
            const endPx = (this.endTime / this.duration) * bounds.width - HANDLE_WIDTH_PX;

            this.positionIndicator.style.left = `${startPx + indicatorPercentage * (endPx - startPx)}px`;

            // Loop back around if the video ends
            if (currentTime >= this.endTime) {
                this.videoPlayer.currentTime = this.startTime;
            }
        }

        // Repeat on the next animation frame
        requestAnimationFrame(() => this.updatePositionIndicator());
    }

    /**
     * Toggles play/pause state of the video when clicked, and shows the corresponding icon with animation.
     */
    togglePlayPause() {
        if (this.videoPlayer.paused) {
            this.videoPlayer.play();
            this.showPlayPauseIcon('pi-play');
        } else {
            this.videoPlayer.pause();
            this.showPlayPauseIcon('pi-pause');
        }
    }

    /**
     * Flash the play or pause icon with animation
     * @param iconClass - PrimeIcons class for the icon to show
     */
    showPlayPauseIcon(iconClass) {
        // Update icon
        this.playPauseIcon.className = `pi ${iconClass}`;
        
        // Remove existing animation class
        this.playPauseOverlay.classList.remove('show');
        
        // Force synchronous layout recalculation to ensure class removal takes effect in case of rapid toggling
        this.playPauseOverlay.offsetHeight;
        
        // Add animation class
        this.playPauseOverlay.classList.add('show');
        
        // Remove animation class after animation completes
        setTimeout(() => {
            this.playPauseOverlay.classList.remove('show');
        }, PLAY_ANIMATION_DURATION_MS);
    }

    /**
     * Handles form submission, validating input and populating hidden fields before submitting.
     * @param e - Event object from the form submission
     */
    submitForm(e) {
        e.preventDefault();

        // Validate title
        if (!this.videoTitleInput.value.trim()) {
            alert('Please choose a video name.');
            this.videoTitleInput.focus();
            return;
        }

        // Fill properties
        this.startTimeSecondsForm.value = this.startTime;
        this.endTimeSecondsForm.value = this.endTime;
        this.videoTitleForm.value = this.videoTitleInput.value;

        // Submit the form
        this.videoFormField.submit();
    }
}